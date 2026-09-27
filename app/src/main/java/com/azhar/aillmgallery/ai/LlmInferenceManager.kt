package com.azhar.aillmgallery.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * App-wide owner of the on-device LLM. There is exactly one loaded model at a time, shared by
 * Chat, Story, Quiz and Settings — loading a multi-GB model is the most expensive thing the
 * app does, so it is loaded once and reused.
 *
 * Falls back to a mock engine if:
 *  - No model is downloaded
 *  - Running on an emulator (MediaPipe LLM doesn't support x86/emulators)
 *  - The model format is incompatible
 */
class LlmInferenceManager private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "LlmInferenceManager"
        private const val DEFAULT_MAX_TOKENS = 1024
        private const val EXTENDED_MAX_TOKENS = 4096
        private val DIRECT_EXECUTOR = Executor { it.run() }

        @Volatile
        private var instance: LlmInferenceManager? = null

        fun getInstance(context: Context): LlmInferenceManager {
            return instance ?: synchronized(this) {
                instance ?: LlmInferenceManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = InferencePreferences(appContext)

    /** Serializes model loading so concurrent screens never load the model twice. */
    private val loadMutex = Mutex()

    /** MediaPipe runs one generation at a time; queue the rest instead of failing. */
    private val generationMutex = Mutex()

    @Volatile private var llmInference: LlmInference? = null
    @Volatile private var activeSession: LlmInferenceSession? = null
    @Volatile private var loadedPath: String? = null
    @Volatile private var loadedBackend: LlmInference.Backend? = null
    @Volatile private var isMockMode = false

    private val activeGenerations = AtomicInteger(0)

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeModelName = MutableStateFlow("None")
    val activeModelName: StateFlow<String> = _activeModelName.asStateFlow()

    /** "GPU", "CPU" or "Demo" — shown as a chip in the UI. */
    private val _activeBackend = MutableStateFlow("—")
    val activeBackend: StateFlow<String> = _activeBackend.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _useGpu = MutableStateFlow(true)
    val useGpu: StateFlow<Boolean> = _useGpu.asStateFlow()

    /** Chat template matching the loaded model. */
    val promptTemplate: PromptFormatter.Template
        get() = PromptFormatter.templateFor(loadedPath?.let { File(it).name })

    /** Total context (prompt + response) of the loaded model, in tokens. */
    val contextTokens: Int
        get() = maxTokensFor(loadedPath)

    /**
     * Idempotent: loads the user's selected model (or the first one found, or the demo engine)
     * unless something is already loaded. Every screen calls this instead of scanning on its own.
     */
    suspend fun ensureInitialized() {
        if (_isInitialized.value) return
        loadMutex.withLock {
            if (_isInitialized.value) return
            _useGpu.value = prefs.useGpu()
            val path = resolveModelPath()
            if (path == null) activateMockMode("Demo Engine") else loadLocked(path)
        }
    }

    /** Loads [modelPath], persisting it as the user's selection. No-op if already loaded. */
    suspend fun selectModel(modelPath: String) {
        prefs.setSelectedModelPath(modelPath)
        loadMutex.withLock { loadLocked(modelPath) }
    }

    /** Loads a freshly downloaded model if nothing real is loaded yet (demo mode or idle). */
    suspend fun onModelDownloaded(modelPath: String) {
        if (isMockMode || !_isInitialized.value) selectModel(modelPath)
    }

    /** Switches between GPU and CPU and reloads the current model with the new backend. */
    suspend fun setUseGpu(enabled: Boolean) {
        prefs.setUseGpu(enabled)
        _useGpu.value = enabled
        val path = loadedPath ?: return
        loadMutex.withLock { loadLocked(path, force = true) }
    }

    /**
     * Initialize with a real model file, or fall back to mock mode if unavailable.
     * Kept for callers that pass an explicit path.
     */
    suspend fun initialize(context: Context, modelPath: String): Result<Unit> {
        loadMutex.withLock { loadLocked(modelPath) }
        return Result.success(Unit)
    }

    private suspend fun resolveModelPath(): String? = withContext(Dispatchers.IO) {
        prefs.selectedModelPath()?.takeIf { File(it).let { f -> f.exists() && f.length() > 0 } }
            ?: ModelManager(appContext).run {
                ensureModelsDirectory()
                scanForModels()
                availableModels.value.firstOrNull()?.path
            }
    }

    private suspend fun loadLocked(modelPath: String, force: Boolean = false) = withContext(Dispatchers.Default) {
        val wantedBackend = if (_useGpu.value) LlmInference.Backend.GPU else LlmInference.Backend.CPU
        if (!force && !isMockMode && llmInference != null && loadedPath == modelPath) {
            Log.d(TAG, "Model already loaded, skipping reload: $modelPath")
            return@withContext
        }

        _isLoading.value = true
        // Wait for any in-flight generation before tearing down the engine it runs on.
        cancelGeneration()
        generationMutex.withLock { releaseEngine() }

        val modelFile = File(modelPath)
        if (!modelFile.exists() || modelFile.length() == 0L) {
            Log.w(TAG, "Model file missing or empty — activating mock mode")
            activateMockMode("Demo Engine")
            return@withContext
        }

        val backends = if (wantedBackend == LlmInference.Backend.GPU) {
            listOf(LlmInference.Backend.GPU, LlmInference.Backend.CPU)
        } else {
            listOf(LlmInference.Backend.CPU)
        }

        for (backend in backends) {
            try {
                val start = System.currentTimeMillis()
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(maxTokensFor(modelPath))
                    .setPreferredBackend(backend)
                    .build()
                llmInference = LlmInference.createFromOptions(appContext, options)
                loadedPath = modelPath
                loadedBackend = backend
                isMockMode = false
                _isDemoMode.value = false
                _activeModelName.value = modelFile.name
                _activeBackend.value = backend.name
                _isInitialized.value = true
                _isLoading.value = false
                Log.d(TAG, "Loaded ${modelFile.name} on $backend in ${System.currentTimeMillis() - start}ms")
                return@withContext
            } catch (e: Throwable) {
                Log.w(TAG, "Loading on $backend failed (${e.javaClass.simpleName}): ${e.message}")
            }
        }

        Log.w(TAG, "All backends failed — falling back to mock engine for UI showcase")
        activateMockMode("Demo Engine (fallback)")
    }

    private fun maxTokensFor(path: String?): Int =
        if (path != null && ("4k" in path.lowercase() || "ekv4096" in path.lowercase())) EXTENDED_MAX_TOKENS
        else DEFAULT_MAX_TOKENS

    /**
     * Switch to the mock engine (no model file needed).
     */
    suspend fun initializeMock(name: String = "Demo Engine") {
        loadMutex.withLock {
            cancelGeneration()
            generationMutex.withLock { releaseEngine() }
            activateMockMode(name)
        }
    }

    /** Callers must have released the real engine (under [generationMutex]) first. */
    private fun activateMockMode(name: String) {
        isMockMode = true
        _isDemoMode.value = true
        _isInitialized.value = true
        _isLoading.value = false
        _activeModelName.value = name
        _activeBackend.value = "Demo"
        Log.d(TAG, "Mock engine activated: $name")
    }

    private fun releaseEngine() {
        runCatching { llmInference?.close() }.onFailure { Log.e(TAG, "Error closing LLM", it) }
        llmInference = null
        loadedPath = null
        loadedBackend = null
    }

    /**
     * Streams a response. [mockKey] picks the demo reply when no real model is loaded
     * (pass the raw user text, or "story"/"quiz").
     */
    fun generateResponse(
        prompt: String,
        config: GenerationConfig = GenerationConfig.CHAT,
        mockKey: String = prompt
    ): Flow<StreamingResult> {
        return if (isMockMode) generateMockResponse(mockKey) else generateRealResponse(prompt, config)
    }

    /** Stops the in-flight generation, if any. The partial response is kept. */
    fun cancelGeneration() {
        runCatching { activeSession?.cancelGenerateResponseAsync() }
            .onFailure { Log.w(TAG, "Cancel failed: ${it.message}") }
    }

    private fun generateRealResponse(prompt: String, config: GenerationConfig): Flow<StreamingResult> = callbackFlow {
        generationMutex.lock()
        val released = AtomicBoolean(false)
        var session: LlmInferenceSession? = null
        fun release() {
            if (released.compareAndSet(false, true)) {
                runCatching { session?.close() }
                activeSession = null
                markGenerating(false)
                generationMutex.unlock()
            }
        }

        markGenerating(true)
        val inference = llmInference
        if (inference == null) {
            trySend(StreamingResult(text = "Error: engine not initialized.", isDone = true, isError = true))
            release()
            close()
            awaitClose { }
            return@callbackFlow
        }

        val startTime = System.currentTimeMillis()
        var firstTokenTime = 0L
        var chunks = 0
        val accumulated = StringBuilder()
        val finished = AtomicBoolean(false)

        try {
            session = LlmInferenceSession.createFromOptions(
                inference,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(config.topK)
                    .setTopP(config.topP)
                    .setTemperature(config.temperature)
                    .build()
            ).also { activeSession = it }
            session.addQueryChunk(prompt)

            val future = session.generateResponseAsync { partial, done ->
                val chunk: String = partial ?: ""
                if (chunk.isNotEmpty()) {
                    if (firstTokenTime == 0L) firstTokenTime = System.currentTimeMillis() - startTime
                    chunks++
                    accumulated.append(chunk)
                }
                val elapsed = System.currentTimeMillis() - startTime
                trySend(
                    StreamingResult(
                        text = accumulated.toString(),
                        delta = chunk,
                        isDone = done,
                        inferenceTimeMs = elapsed,
                        timeToFirstTokenMs = firstTokenTime,
                        tokensPerSecond = decodeSpeed(chunks, elapsed - firstTokenTime)
                    )
                )
                if (done) {
                    finished.set(true)
                    Log.d(TAG, "Generation done in ${elapsed}ms, ttft=${firstTokenTime}ms, $chunks chunks")
                    close()
                }
            }
            // Surfaces errors and cancellation, and frees the session once native work is over.
            future.addListener({
                val error = runCatching { future.get() }.exceptionOrNull()
                if (error != null && !finished.get()) {
                    Log.w(TAG, "Generation ended early: ${error.message}")
                    trySend(
                        StreamingResult(
                            text = accumulated.toString(),
                            isDone = true,
                            isError = accumulated.isEmpty(),
                            inferenceTimeMs = System.currentTimeMillis() - startTime
                        )
                    )
                }
                close()
                release()
            }, DIRECT_EXECUTOR)
        } catch (e: Exception) {
            Log.e(TAG, "Real generation failed", e)
            trySend(StreamingResult(text = "Error: ${e.message}", isDone = true, isError = true))
            release()
            close()
        }

        awaitClose {
            // Collector went away (screen closed / Stop tapped) — stop burning CPU/GPU.
            if (!finished.get() && !released.get()) cancelGeneration()
        }
    }.flowOn(Dispatchers.Default)

    private fun decodeSpeed(chunks: Int, decodeMs: Long): Float =
        if (chunks > 1 && decodeMs > 0) (chunks - 1) * 1000f / decodeMs else 0f

    private fun markGenerating(started: Boolean) {
        val count = if (started) activeGenerations.incrementAndGet() else activeGenerations.decrementAndGet()
        _isGenerating.value = count > 0
    }

    /**
     * Generate a complete response. Useful for structured outputs like JSON where partial
     * streaming isn't needed.
     */
    suspend fun generateCompleteResponse(
        prompt: String,
        config: GenerationConfig = GenerationConfig.PRECISE,
        mockKey: String = prompt
    ): String {
        return generateResponse(prompt, config, mockKey).last().text
    }

    private fun generateMockResponse(key: String): Flow<StreamingResult> = flow {
        markGenerating(true)
        try {
            val startTime = System.currentTimeMillis()
            val response = mockResponseFor(key)
            delay(250) // simulated prefill
            val ttft = System.currentTimeMillis() - startTime

            // Simulate token streaming
            val words = response.split(" ")
            val current = StringBuilder()
            for ((i, word) in words.withIndex()) {
                if (i > 0) current.append(' ')
                current.append(word)
                delay(18)
                val elapsed = System.currentTimeMillis() - startTime
                emit(
                    StreamingResult(
                        text = current.toString(),
                        delta = word,
                        isDone = i == words.lastIndex,
                        inferenceTimeMs = elapsed,
                        timeToFirstTokenMs = ttft,
                        tokensPerSecond = decodeSpeed(i + 1, elapsed - ttft)
                    )
                )
            }
        } finally {
            markGenerating(false)
        }
    }.flowOn(Dispatchers.Default)

    private fun mockResponseFor(key: String): String {
        val lower = key.lowercase()
        return when {
            lower == "quiz" ->
                """{"title": "Demo Quiz", "questions": [{"text": "What is 2+2?", "options": ["3", "4", "5", "6"], "correct": 1}, {"text": "Which planet is closest to the Sun?", "options": ["Earth", "Venus", "Mercury", "Mars"], "correct": 2}, {"text": "What gas do plants primarily absorb?", "options": ["Oxygen", "Nitrogen", "Carbon Dioxide", "Hydrogen"], "correct": 2}, {"text": "Who painted the Mona Lisa?", "options": ["Michelangelo", "Da Vinci", "Raphael", "Donatello"], "correct": 1}, {"text": "What is the speed of light approximately?", "options": ["300 km/s", "300,000 km/s", "30,000 km/s", "3,000 km/s"], "correct": 1}]}"""

            lower == "story" ->
                "The old lighthouse keeper had not spoken a word in seven years. Each evening, he climbed the spiraling iron staircase, lit the great lamp, and watched the sea swallow the horizon whole. The townsfolk called him mad; the fishermen called him a ghost. But the ships that passed safely through the Devil's Strait every night — they called him a guardian.\n\nOne autumn morning, a girl with salt-crusted boots and a telescope appeared at his door. She carried a letter sealed with wax the color of dried blood. \"My father sent me,\" she said, though the keeper knew her father had been dead for three years. He let her in anyway. Some truths, he had learned, arrive wearing the clothes of impossibility.\n\nTogether they watched the storms roll in, counting the seconds between lightning and thunder. She told him about cities built on clouds and rivers that flowed uphill. He told her nothing — but she understood everything. When she left at dawn, the lighthouse burned brighter than it ever had before, and for the first time in seven years, the keeper smiled."

            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hi! I'm currently running in **Demo Mode**. To enable real on-device AI inference, download a model from the Model Hub. I can still demonstrate the chat interface!"

            lower.contains("story") || lower.contains("write") || lower.contains("creative") ->
                "In the year 2147, humanity's greatest invention wasn't the quantum computer or the gravity drive — it was an AI small enough to run in your pocket. It didn't conquer cities; it helped farmers know when to plant, helped doctors catch cancer early, and helped lonely people feel heard. The machines never wanted power. They just wanted to be useful."

            lower.contains("code") || lower.contains("function") || lower.contains("program") ->
                "Here's a Kotlin example:\n\n```kotlin\nfun fibonacci(n: Int): Long {\n    return when (n) {\n        0 -> 0L\n        1 -> 1L\n        else -> fibonacci(n - 1) + fibonacci(n - 2)\n    }\n}\n\nprintln(fibonacci(10)) // 55\n```"

            lower.contains("summarize") || lower.contains("summary") ->
                "**Summary:** The text discusses the main points in a concise manner. Key takeaways include the core ideas, supporting evidence, and the overall conclusion.\n\n_Demo mode — real summarization requires a downloaded model._"

            lower.contains("image") || lower.contains("photo") || lower.contains("picture") || lower.contains("describe") ->
                "I can see the image you've attached. It appears to contain visual elements that would be analyzed by the on-device vision model.\n\n_Demo mode — real image analysis requires a downloaded model with vision capabilities._"

            lower.contains("object") || lower.contains("detect") ->
                "Detected objects (demo):\n- Person (87%)\n- Laptop (91%)\n- Coffee cup (76%)\n- Desk (95%)\n\n_These are simulated detections — download a model for real object detection._"

            lower.contains("question") || lower.contains("what") || lower.contains("how") || lower.contains("why") || lower.contains("who") ->
                "That's an interesting question! In Demo Mode, I can't provide real answers based on reasoning. Once you download one of the available models (like SmolLM 135M or Qwen 2.5 0.5B), I'll be able to give you real, thoughtful responses."

            else ->
                "You asked: \"${key.trimEnd().takeLast(80)}\"\n\nThis is Demo Mode — the app is working, but real AI reasoning requires a downloaded model. Open the **Model Hub** and download SmolLM (159 MB, fastest) or Gemma 3 1B (529 MB, better quality)."
        }
    }

    fun getDownloadedModels(context: Context): List<File> {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) return emptyList()
        return modelsDir.listFiles { file -> file.extension == "task" || file.extension == "bin" }?.toList() ?: emptyList()
    }

    /** Deletes a model file, unloading it first (safely, after any in-flight generation) if active. */
    suspend fun deleteModel(context: Context, fileName: String): Boolean {
        val file = File(File(context.filesDir, "models"), fileName)
        if (!file.exists()) return false
        loadMutex.withLock {
            if (!isMockMode && _activeModelName.value == fileName) {
                cancelGeneration()
                generationMutex.withLock { releaseEngine() }
                _isInitialized.value = false
                _activeModelName.value = "None"
                _activeBackend.value = "—"
                Log.d(TAG, "Unloaded $fileName before deleting it")
            }
        }
        return withContext(Dispatchers.IO) { file.delete() }
    }
}

/** Sampling presets per use case. */

data class GenerationConfig(
    val temperature: Float,
    val topK: Int,
    val topP: Float
) {
    companion object {
        val CHAT = GenerationConfig(temperature = 0.7f, topK = 40, topP = 0.95f)
        val CREATIVE = GenerationConfig(temperature = 0.9f, topK = 64, topP = 0.95f)
        /** Low temperature for code, summaries and structured JSON. */
        val PRECISE = GenerationConfig(temperature = 0.2f, topK = 20, topP = 0.9f)
    }
}

data class StreamingResult(
    val text: String,
    val isDone: Boolean,
    val delta: String = "",
    val inferenceTimeMs: Long = 0,
    val timeToFirstTokenMs: Long = 0,
    val tokensPerSecond: Float = 0f,
    val isError: Boolean = false
)
