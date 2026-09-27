package com.azhar.aillmgallery.ui.chat

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azhar.aillmgallery.ai.EngineStatus
import com.azhar.aillmgallery.ai.GenerationConfig
import com.azhar.aillmgallery.ai.LlmInferenceManager
import com.azhar.aillmgallery.ai.PromptFormatter
import com.azhar.aillmgallery.ai.engineStatusIn
import com.azhar.aillmgallery.data.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    // Shared, app-wide engine: the model is loaded once and reused across screens.
    private val llmManager = LlmInferenceManager.getInstance(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _attachedImage = MutableStateFlow<Bitmap?>(null)
    val attachedImage: StateFlow<Bitmap?> = _attachedImage.asStateFlow()

    val engineStatus: StateFlow<EngineStatus> = llmManager.engineStatusIn(viewModelScope)

    private var generationJob: Job? = null

    init {
        viewModelScope.launch { llmManager.ensureInitialized() }
    }

    fun sendMessage(text: String, capabilityId: String) {
        if (text.isBlank() || _isGenerating.value) return

        val image = _attachedImage.value
        val history = _messages.value
        _messages.update { it + ChatMessage(text = text.trim(), isUser = true, imageBitmap = image) }
        _attachedImage.value = null

        generateAiResponse(text.trim(), capabilityId, image != null, history)
    }

    /** Drops the last AI reply and asks again with the same user message. */
    fun regenerate(capabilityId: String) {
        if (_isGenerating.value) return
        val current = _messages.value
        val lastUserIndex = current.indexOfLast { it.isUser }
        if (lastUserIndex < 0) return
        val lastUser = current[lastUserIndex]
        _messages.value = current.subList(0, lastUserIndex + 1)
        generateAiResponse(lastUser.text, capabilityId, lastUser.imageBitmap != null, current.subList(0, lastUserIndex))
    }

    fun stopGeneration() {
        generationJob?.cancel()
    }

    fun clearChat() {
        stopGeneration()
        _messages.value = emptyList()
    }

    private fun generateAiResponse(
        userText: String,
        capabilityId: String,
        hasImage: Boolean,
        history: List<ChatMessage>
    ) {
        val placeholder = ChatMessage(text = "", isUser = false, isStreaming = true)
        _messages.update { it + placeholder }
        _isGenerating.value = true

        generationJob = viewModelScope.launch {
            try {
                llmManager.ensureInitialized()

                val prompt = PromptFormatter.build(
                    template = llmManager.promptTemplate,
                    systemPrompt = systemPromptFor(capabilityId),
                    userMessage = if (hasImage) "(An image is attached.)\n$userText" else userText,
                    history = history
                        .filter { !it.isStreaming && !it.isError && it.text.isNotBlank() }
                        .map { PromptFormatter.Turn(isUser = it.isUser, text = it.text) },
                    // Leave roughly half of the context window for the answer (~4 chars per token).
                    historyCharBudget = llmManager.contextTokens * 4 / 2
                )

                llmManager.generateResponse(prompt, configFor(capabilityId), mockKey = userText)
                    .conflate()
                    .collect { result ->
                        updateMessage(placeholder.id) {
                            it.copy(
                                text = PromptFormatter.cleanOutput(result.text).trimStart(),
                                isStreaming = !result.isDone,
                                isError = result.isError,
                                inferenceTimeMs = if (result.isDone) result.inferenceTimeMs else null,
                                tokensPerSecond = result.tokensPerSecond.takeIf { v -> v > 0f }
                            )
                        }
                        // Cap UI updates at ~30 fps; conflate() keeps only the newest chunk meanwhile.
                        if (!result.isDone) delay(33)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateMessage(placeholder.id) { it.copy(text = "Error: ${e.message}", isError = true) }
            } finally {
                updateMessage(placeholder.id) { msg ->
                    when {
                        !msg.isStreaming -> msg
                        msg.text.isBlank() -> msg.copy(isStreaming = false, text = "_Stopped._")
                        else -> msg.copy(isStreaming = false)
                    }
                }
                _isGenerating.value = false
            }
        }
    }

    private inline fun updateMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        _messages.update { list ->
            val index = list.indexOfLast { it.id == id }
            if (index < 0) list else list.toMutableList().also { it[index] = transform(it[index]) }
        }
    }

    private fun configFor(capabilityId: String): GenerationConfig = when (capabilityId) {
        "text_generation" -> GenerationConfig.CREATIVE
        "code_assistant", "summarization", "object_detection" -> GenerationConfig.PRECISE
        else -> GenerationConfig.CHAT
    }

    private fun systemPromptFor(capabilityId: String): String = when (capabilityId) {
        "text_generation" -> "You are a creative writing assistant. Generate engaging, imaginative content. Always respond in English only."
        "summarization" -> "You are a summarization expert. Condense the user's text into short, clear bullet points. Always respond in English only."
        "code_assistant" -> "You are a helpful code assistant. Provide clear, well-commented code in fenced code blocks, followed by a brief explanation. Always respond in English only."
        "image_captioning" -> "You are an image captioning AI. Describe the image content in detail. Always respond in English only."
        "visual_qa" -> "You are a visual question answering AI. Answer questions about the image accurately. Always respond in English only."
        "object_detection" -> "You are an object detection AI. Identify and list all objects visible. Always respond in English only."
        else -> "You are a helpful AI assistant. Be concise and accurate. Always respond in English only."
    }

    fun setAttachedImage(bitmap: Bitmap?) {
        _attachedImage.value = bitmap
    }

    fun clearAttachedImage() {
        _attachedImage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Don't close the shared engine — other screens reuse the loaded model.
        stopGeneration()
    }
}
