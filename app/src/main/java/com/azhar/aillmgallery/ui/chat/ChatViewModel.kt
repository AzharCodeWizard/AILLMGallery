package com.azhar.aillmgallery.ui.chat

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azhar.aillmgallery.ai.LlmInferenceManager
import com.azhar.aillmgallery.ai.ModelManager
import com.azhar.aillmgallery.ai.ModelState
import com.azhar.aillmgallery.data.model.AiCapabilities
import com.azhar.aillmgallery.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val llmManager = LlmInferenceManager()
    private val modelManager = ModelManager(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _modelState = MutableStateFlow<ModelState>(ModelState.Checking)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _attachedImage = MutableStateFlow<Bitmap?>(null)
    val attachedImage: StateFlow<Bitmap?> = _attachedImage.asStateFlow()

    private var isInitialized = false

    init {
        viewModelScope.launch {
            modelManager.ensureModelsDirectory()
            modelManager.scanForModels()
            modelManager.modelState.collect { state ->
                _modelState.value = state

                when (state) {
                    is ModelState.Ready -> {
                        if (!isInitialized) {
                            initializeModel(state.model.path)
                        }
                    }
                    is ModelState.NotFound -> {
                        // No model downloaded yet — start mock engine so UI is still usable
                        if (!isInitialized) {
                            llmManager.initializeMock("Demo Engine")
                            isInitialized = true
                            // Keep showing NotFound in the status bar so user knows to download
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun initializeModel(modelPath: String) {
        viewModelScope.launch {
            _modelState.value = ModelState.Loading()
            android.util.Log.d("ChatViewModel", "Attempting to initialize model at: $modelPath")
            val result = llmManager.initialize(getApplication(), modelPath)
            if (result.isSuccess) {
                android.util.Log.d("ChatViewModel", "Model initialized successfully!")
                isInitialized = true
                val currentState = modelManager.modelState.value
                if (currentState is ModelState.Ready) {
                    _modelState.value = currentState
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to initialize model"
                android.util.Log.e("ChatViewModel", "Model initialization failed: $errorMsg")
                _modelState.value = ModelState.Error(errorMsg)
            }
        }
    }

    fun sendMessage(text: String, capabilityId: String) {
        if (text.isBlank()) return

        val image = _attachedImage.value
        val userMessage = ChatMessage(
            text = text,
            isUser = true,
            imageBitmap = image
        )

        _messages.value = _messages.value + userMessage
        _attachedImage.value = null

        generateAiResponse(text, capabilityId, image)
    }

    private fun generateAiResponse(prompt: String, capabilityId: String, image: Bitmap?) {
        viewModelScope.launch {
            _isGenerating.value = true

            // Add a streaming placeholder
            val placeholderMessage = ChatMessage(
                text = "",
                isUser = false,
                isStreaming = true
            )
            _messages.value = _messages.value + placeholderMessage

            if (!llmManager.isInitialized.value) {
                _messages.value = _messages.value.dropLast(1) + placeholderMessage.copy(
                    text = "Model not loaded. Please download a model from the Gallery to continue.",
                    isStreaming = false
                )
                _isGenerating.value = false
                return@launch
            }

            val capability = AiCapabilities.all.find { it.id == capabilityId }
            val enhancedPrompt = buildPrompt(prompt, capabilityId)

            try {
                val flow = if (image != null) {
                    llmManager.generateVisionResponse(
                        prompt = enhancedPrompt,
                        imageDescription = "User has attached an image for analysis."
                    )
                } else {
                    llmManager.generateResponse(enhancedPrompt)
                }

                flow.collect { result ->
                    val aiMessage = placeholderMessage.copy(
                        text = result.text,
                        isStreaming = !result.isDone,
                        inferenceTimeMs = if (result.isDone) result.inferenceTimeMs else null
                    )
                    _messages.value = _messages.value.dropLast(1) + aiMessage
                }
            } catch (e: Exception) {
                val errorMessage = placeholderMessage.copy(
                    text = "Error: ${e.message}",
                    isStreaming = false
                )
                _messages.value = _messages.value.dropLast(1) + errorMessage
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun buildPrompt(userPrompt: String, capabilityId: String): String {
        val systemContext = when (capabilityId) {
            "text_generation" -> "You are a creative writing assistant. Generate engaging, imaginative content. Always respond in English only."
            "summarization" -> "You are a summarization expert. Condense the following text into key points. Always respond in English only."
            "code_assistant" -> "You are a helpful code assistant. Provide clear, well-commented code and explanations. Always respond in English only."
            "image_captioning" -> "You are an image captioning AI. Describe the image content in detail. Always respond in English only."
            "visual_qa" -> "You are a visual question answering AI. Answer questions about the image accurately. Always respond in English only."
            "object_detection" -> "You are an object detection AI. Identify and list all objects visible. Always respond in English only."
            else -> "You are a helpful AI assistant. Always respond in English only."
        }

        // Modern models like Qwen2.5 and SmolLM perform much better with ChatML format
        return "<|im_start|>system\n$systemContext<|im_end|>\n<|im_start|>user\n$userPrompt<|im_end|>\n<|im_start|>assistant\n"
    }

    fun setAttachedImage(bitmap: Bitmap?) {
        _attachedImage.value = bitmap
    }

    fun clearAttachedImage() {
        _attachedImage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        llmManager.close()
    }
}
