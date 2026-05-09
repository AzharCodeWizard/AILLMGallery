package com.azhar.aillmgallery.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
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
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages MediaPipe LLM Inference for on-device text generation.
 * Falls back to a smart mock engine if:
 *  - No model is downloaded
 *  - Running on an emulator (MediaPipe LLM doesn't support x86/emulators)
 *  - The model format is incompatible
 */
class LlmInferenceManager {

    companion object {
        private const val TAG = "LlmInferenceManager"
        private const val DEFAULT_MAX_TOKENS = 1024

        @Volatile
        private var instance: LlmInferenceManager? = null

        fun getInstance(context: Context): LlmInferenceManager {
            return instance ?: synchronized(this) {
                instance ?: LlmInferenceManager().also { instance = it }
            }
        }
    }

    private var llmInference: LlmInference? = null
    private var isMockMode = false
    private var mockModelName = "Demo Engine"

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeModelName = MutableStateFlow("None")
    val activeModelName: StateFlow<String> = _activeModelName.asStateFlow()

    /**
     * Initialize with a real model file, or fall back to mock mode if unavailable.
     */
    suspend fun initialize(
        context: Context,
        modelPath: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val modelFile = File(modelPath)
            Log.d(TAG, "Attempting to load model: $modelPath")
            Log.d(TAG, "File exists: ${modelFile.exists()}, size: ${modelFile.length()} bytes")

            if (!modelFile.exists() || modelFile.length() == 0L) {
                Log.w(TAG, "Model file missing or empty — activating mock mode")
                activateMockMode("Demo Engine")
                return@withContext Result.success(Unit)
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(maxTokens)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isMockMode = false
            _isInitialized.value = true
            _activeModelName.value = modelFile.name

            Log.d(TAG, "Real LLM initialized successfully: ${modelFile.name}")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Real LLM init failed (${e.javaClass.simpleName}): ${e.message}")
            Log.w(TAG, "Falling back to mock engine for UI showcase")
            activateMockMode("Demo Engine (emulator fallback)")
            Result.success(Unit) // Still success so UI works
        }
    }

    /**
     * Initialize the mock engine directly (no model file needed).
     * Used when no model is downloaded yet.
     */
    fun initializeMock(name: String = "Demo Engine") {
        activateMockMode(name)
    }

    private fun activateMockMode(name: String) {
        isMockMode = true
        mockModelName = name
        llmInference = null
        _isInitialized.value = true
        _activeModelName.value = name
        Log.d(TAG, "Mock engine activated: $name")
    }

    /**
     * Generate a response — real engine if available, smart mock otherwise.
     */
    fun generateResponse(prompt: String): Flow<StreamingResult> {
        return if (isMockMode) {
            generateMockResponse(prompt)
        } else {
            generateRealResponse(prompt)
        }
    }

    private fun generateRealResponse(prompt: String): Flow<StreamingResult> = callbackFlow {
        val inference = llmInference ?: run {
            trySend(StreamingResult(text = "Error: engine not initialized.", isDone = true, isError = true))
            close()
            return@callbackFlow
        }

        _isGenerating.value = true
        val startTime = System.currentTimeMillis()
        val accumulated = StringBuilder()

        try {
            Log.d(TAG, "Starting real generation, prompt length: ${prompt.length}")
            inference.generateResponseAsync(prompt) { partial, done ->
                accumulated.append(partial)
                trySend(
                    StreamingResult(
                        text = accumulated.toString(),
                        isDone = done,
                        inferenceTimeMs = if (done) System.currentTimeMillis() - startTime else 0
                    )
                )
                if (done) {
                    val totalTime = System.currentTimeMillis() - startTime
                    Log.d(TAG, "Generation done in ${totalTime}ms")
                    Log.d(TAG, "Final response length: ${accumulated.length}")
                    if (accumulated.isEmpty()) {
                        Log.w(TAG, "Model returned EMPTY response. Check prompt format.")
                    } else {
                        Log.d(TAG, "Sample output: ${accumulated.take(100)}...")
                    }
                    _isGenerating.value = false
                    close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Real generation failed", e)
            trySend(StreamingResult(text = "Error: ${e.message}", isDone = true, isError = true))
            _isGenerating.value = false
            close()
        }

        awaitClose { _isGenerating.value = false }
    }.flowOn(Dispatchers.Default)

    /**
     * Generate a complete response synchronously. Useful for structured outputs like JSON where partial streaming isn't needed.
     */
    suspend fun generateCompleteResponse(prompt: String): String = withContext(Dispatchers.Default) {
        if (isMockMode) {
            delay(1500)
            val lower = prompt.lowercase()
            // Story generation mock
            if (lower.contains("story") || lower.contains("author") || lower.contains("storytelling")) {
                return@withContext "The old lighthouse keeper had not spoken a word in seven years. Each evening, he climbed the spiraling iron staircase, lit the great lamp, and watched the sea swallow the horizon whole. The townsfolk called him mad; the fishermen called him a ghost. But the ships that passed safely through the Devil's Strait every night — they called him a guardian.\n\nOne autumn morning, a girl with salt-crusted boots and a telescope appeared at his door. She carried a letter sealed with wax the color of dried blood. \"My father sent me,\" she said, though the keeper knew her father had been dead for three years. He let her in anyway. Some truths, he had learned, arrive wearing the clothes of impossibility.\n\nTogether they watched the storms roll in, counting the seconds between lightning and thunder. She told him about cities built on clouds and rivers that flowed uphill. He told her nothing — but she understood everything. When she left at dawn, the lighthouse burned brighter than it ever had before, and for the first time in seven years, the keeper smiled."
            }
            // Quiz generation mock — must have exactly 4 options per question
            return@withContext """{"title": "Demo Quiz", "questions": [{"text": "What is 2+2?", "options": ["3", "4", "5", "6"], "correct": 1}, {"text": "Which planet is closest to the Sun?", "options": ["Earth", "Venus", "Mercury", "Mars"], "correct": 2}, {"text": "What gas do plants primarily absorb?", "options": ["Oxygen", "Nitrogen", "Carbon Dioxide", "Hydrogen"], "correct": 2}, {"text": "Who painted the Mona Lisa?", "options": ["Michelangelo", "Da Vinci", "Raphael", "Donatello"], "correct": 1}, {"text": "What is the speed of light approximately?", "options": ["300 km/s", "300,000 km/s", "30,000 km/s", "3,000 km/s"], "correct": 1}]}"""
        }

        val inference = llmInference ?: return@withContext "Error: engine not initialized."
        _isGenerating.value = true
        try {
            val response = inference.generateResponse(prompt)
            _isGenerating.value = false
            return@withContext response
        } catch (e: Exception) {
            _isGenerating.value = false
            return@withContext "Error: ${e.message}"
        }
    }

    private fun generateMockResponse(prompt: String): Flow<StreamingResult> = flow {
        _isGenerating.value = true
        val startTime = System.currentTimeMillis()

        val lower = prompt.lowercase()
        val response = when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hi! I'm currently running in Demo Mode. To enable real on-device AI inference, please download a model from the gallery. I can still demonstrate the chat interface!"

            lower.contains("story") || lower.contains("write") || lower.contains("creative") ->
                "In the year 2147, humanity's greatest invention wasn't the quantum computer or the gravity drive — it was an AI small enough to run in your pocket. It didn't conquer cities; it helped farmers know when to plant, helped doctors catch cancer early, and helped lonely people feel heard. The machines never wanted power. They just wanted to be useful."

            lower.contains("code") || lower.contains("function") || lower.contains("program") ->
                "Here's a Kotlin example:\n\n```kotlin\nfun fibonacci(n: Int): Long {\n    return when (n) {\n        0 -> 0L\n        1 -> 1L\n        else -> fibonacci(n - 1) + fibonacci(n - 2)\n    }\n}\n\nprintln(fibonacci(10)) // 55\n```"

            lower.contains("summarize") || lower.contains("summary") ->
                "Summary: The text discusses the main points in a concise manner. Key takeaways include the core ideas, supporting evidence, and the overall conclusion. [Demo mode — real summarization requires a downloaded model]"

            lower.contains("image") || lower.contains("photo") || lower.contains("picture") || lower.contains("describe") ->
                "I can see the image you've attached. It appears to contain visual elements that would be analyzed by the on-device vision model. [Demo mode — real image analysis requires a downloaded model with vision capabilities]"

            lower.contains("object") || lower.contains("detect") ->
                "Detected objects (demo): Person (87%), Laptop (91%), Coffee cup (76%), Desk (95%). [These are simulated detections — download a model for real object detection]"

            lower.contains("question") || lower.contains("what") || lower.contains("how") || lower.contains("why") || lower.contains("who") ->
                "That's an interesting question! In Demo Mode, I can't provide real answers based on reasoning. Once you download one of the available models (like SmolLM 135M or Qwen 2.5 0.5B), I'll be able to give you real, thoughtful responses."

            else ->
                "You asked: \"${prompt.trimEnd().takeLast(80)}\"\n\nThis is Demo Mode — the app is working, but real AI reasoning requires a downloaded model. Go back to the Gallery and tap the Models button to download SmolLM (159 MB, fastest) or Qwen 2.5 (521 MB, better quality)."
        }

        // Simulate token streaming
        val words = response.split(" ")
        var current = ""
        for ((i, word) in words.withIndex()) {
            current += "$word "
            delay(35)
            emit(
                StreamingResult(
                    text = current.trimEnd(),
                    isDone = i == words.lastIndex,
                    inferenceTimeMs = if (i == words.lastIndex) System.currentTimeMillis() - startTime else 0
                )
            )
        }

        _isGenerating.value = false
    }.flowOn(Dispatchers.Default)

    /**
     * Generate a vision response.
     */
    fun generateVisionResponse(prompt: String, imageDescription: String): Flow<StreamingResult> {
        val visionPrompt = "You are analyzing an image.\nContext: $imageDescription\n\nUser: $prompt"
        return generateResponse(visionPrompt)
    }

    fun close() {
        try {
            llmInference?.close()
            llmInference = null
            _isInitialized.value = false
            _activeModelName.value = "None"
            isMockMode = false
            Log.d(TAG, "LLM resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing LLM", e)
        }
    }

    fun getDownloadedModels(context: Context): List<File> {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) return emptyList()
        return modelsDir.listFiles { file -> file.extension == "task" || file.extension == "bin" }?.toList() ?: emptyList()
    }

    fun deleteModel(context: Context, fileName: String): Boolean {
        val file = File(File(context.filesDir, "models"), fileName)
        if (file.exists()) {
            if (_activeModelName.value == fileName) {
                close()
            }
            return file.delete()
        }
        return false
    }
}

data class StreamingResult(
    val text: String,
    val isDone: Boolean,
    val inferenceTimeMs: Long = 0,
    val isError: Boolean = false
)
