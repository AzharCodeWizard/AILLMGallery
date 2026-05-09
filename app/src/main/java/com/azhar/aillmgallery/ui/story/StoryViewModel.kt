package com.azhar.aillmgallery.ui.story

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azhar.aillmgallery.ai.LlmInferenceManager
import com.azhar.aillmgallery.ai.ModelManager
import com.azhar.aillmgallery.ai.ModelState
import com.azhar.aillmgallery.ai.PiperTtsManager
import com.azhar.aillmgallery.data.local.AppDatabase
import com.azhar.aillmgallery.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val storyDao = db.storyDao()
    private val llmManager = LlmInferenceManager.getInstance(application)
    private val piperTts = PiperTtsManager(application)
    private val modelManager = ModelManager(application)

    init {
        viewModelScope.launch {
            modelManager.ensureModelsDirectory()
            modelManager.scanForModels()
            modelManager.modelState.collect { state ->
                when (state) {
                    is ModelState.Ready -> {
                        if (!llmManager.isInitialized.value) {
                            llmManager.initialize(getApplication(), state.model.path)
                        }
                    }
                    is ModelState.NotFound -> {
                        if (!llmManager.isInitialized.value) {
                            llmManager.initializeMock("Demo Engine")
                        }
                    }
                    else -> Unit
                }
            }
        }

        // Auto-initialize Piper TTS if model is already downloaded
        viewModelScope.launch {
            if (piperTts.isModelDownloaded()) {
                piperTts.initialize()
            }
        }
    }

    val stories: StateFlow<List<StoryEntity>> = storyDao.getAllStories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _currentStory = MutableStateFlow<StoryEntity?>(null)
    val currentStory: StateFlow<StoryEntity?> = _currentStory.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isGeneratingAudio = MutableStateFlow(false)
    val isGeneratingAudio: StateFlow<Boolean> = _isGeneratingAudio.asStateFlow()

    val isPlaying: StateFlow<Boolean> = piperTts.isPlaying
    val isTtsReady: StateFlow<Boolean> = piperTts.isReady
    val isTtsDownloading: StateFlow<Boolean> = piperTts.isDownloading
    val ttsDownloadProgress: StateFlow<Float> = piperTts.downloadProgress

    fun isVoiceModelDownloaded(): Boolean = piperTts.isModelDownloaded()

    companion object {
        val GENRES = listOf("Fantasy", "Sci-Fi", "Mystery", "Romance", "Horror", "Adventure", "Comedy", "Song Lyrics", "Poem")
        // Max chars to send to TTS at once (longer text takes too long on-device)
        private const val TTS_MAX_CHARS = 500
    }

    fun generateStory(promptTopic: String, genre: String = "Fantasy") {
        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null

            val isSongOrPoem = genre in listOf("Song Lyrics", "Poem")

            val systemPrompt = if (isSongOrPoem) {
                """You are a world-renowned songwriter and poet.
                Your task is to write a $genre based on the user's prompt.
                
                Guidelines:
                - Format: Use verses, chorus (for songs), and stanzas (for poems). Label them clearly.
                - Style: Emotionally resonant, rhythmic, and memorable.
                - Length: 3-4 verses with a chorus for songs, or 3-5 stanzas for poems.
                - Constraints: Do not include any introductions, meta-commentary, or explanations. Output ONLY the lyrics/poem text itself. Always respond in English only."""
            } else {
                """You are a world-renowned, award-winning author known for your captivating and immersive storytelling.
                Your task is to write a short, engaging story based on the user's prompt.
                
                Guidelines:
                - Genre: $genre
                - Length: Approximately 3 to 5 well-developed paragraphs.
                - Style: Highly descriptive, emotionally resonant, and engaging from the very first sentence.
                - Constraints: Do not include any introductions, conclusions, meta-commentary, or titles. Output ONLY the story text itself. Always respond in English only."""
            }

            val prompt = """
                <|im_start|>system
                $systemPrompt
                <|im_end|>
                <|im_start|>user
                Write a compelling ${if (isSongOrPoem) genre.lowercase() else "story"} about: $promptTopic.
                <|im_end|>
                <|im_start|>assistant
            """.trimIndent()

            try {
                val response = llmManager.generateCompleteResponse(prompt)
                // Clean the response: convert literal \n to real newlines, remove leading meta-text
                val cleanResponse = response.trim()
                    .replace("\\n", "\n")           // literal \n → real newline
                    .replace(Regex("^Song Lyrics:\\s*\n*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^Poem:\\s*\n*", RegexOption.IGNORE_CASE), "")
                    .trim()

                // Determine a short title
                val titleWords = promptTopic.split(" ").take(4).joinToString(" ")
                val prefix = when (genre) {
                    "Song Lyrics" -> "Song:"
                    "Poem" -> "Poem:"
                    else -> "The Tale of"
                }
                val title = "$prefix $titleWords".replaceFirstChar { it.uppercase() }

                val story = StoryEntity(
                    title = title,
                    content = cleanResponse,
                    genre = genre
                )

                val id = storyDao.insertStory(story)
                val savedStory = storyDao.getStoryById(id)
                _currentStory.value = savedStory
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Failed to generate story", e)
                _error.value = "Failed to generate: Ensure a model is loaded and try again."
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun loadStory(story: StoryEntity) {
        _currentStory.value = story
    }

    fun clearCurrentStory() {
        piperTts.stop()
        _currentStory.value = null
    }

    fun downloadVoiceModel() {
        viewModelScope.launch {
            val result = piperTts.downloadVoiceModel()
            if (result.isSuccess) {
                piperTts.initialize()
            } else {
                _error.value = "Voice download failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun playStory() {
        val fullText = _currentStory.value?.content
        if (!fullText.isNullOrBlank()) {
            viewModelScope.launch {
                if (!piperTts.isReady.value) {
                    val initResult = piperTts.initialize()
                    if (initResult.isFailure) {
                        _error.value = "Voice not ready. Download the voice model first."
                        return@launch
                    }
                }

                // Truncate to max chars to keep generation fast on-device
                val text = if (fullText.length > TTS_MAX_CHARS) {
                    // Try to break at a sentence boundary
                    val truncated = fullText.take(TTS_MAX_CHARS)
                    val lastPeriod = truncated.lastIndexOf('.')
                    if (lastPeriod > TTS_MAX_CHARS / 2) {
                        truncated.substring(0, lastPeriod + 1)
                    } else {
                        truncated + "..."
                    }
                } else {
                    fullText
                }

                _isGeneratingAudio.value = true
                try {
                    piperTts.speak(text)
                } finally {
                    _isGeneratingAudio.value = false
                }
            }
        }
    }

    fun stopStory() {
        piperTts.stop()
    }

    fun deleteStory(story: StoryEntity) {
        viewModelScope.launch {
            if (_currentStory.value?.id == story.id) {
                stopStory()
                _currentStory.value = null
            }
            storyDao.deleteStory(story)
        }
    }

    override fun onCleared() {
        super.onCleared()
        piperTts.shutdown()
    }
}
