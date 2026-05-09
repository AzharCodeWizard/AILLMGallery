package com.azhar.aillmgallery.ui.quiz

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azhar.aillmgallery.ai.LlmInferenceManager
import com.azhar.aillmgallery.ai.ModelManager
import com.azhar.aillmgallery.ai.ModelState
import com.azhar.aillmgallery.data.local.AppDatabase
import com.azhar.aillmgallery.data.local.entity.QuestionEntity
import com.azhar.aillmgallery.data.local.entity.QuizEntity
import com.azhar.aillmgallery.data.local.entity.QuizWithQuestions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val quizDao = db.quizDao()
    private val llmManager = LlmInferenceManager.getInstance(application)
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
    }

    val quizzes: StateFlow<List<QuizWithQuestions>> = quizDao.getAllQuizzesWithQuestions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun generateQuiz(topic: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            
            val prompt = """
                <|im_start|>system
                You are an expert educational AI designed to create high-quality, engaging multiple-choice quizzes.
                Your task is to generate a quiz based on the user's topic.
                The quiz must be strictly formatted as a JSON object. Do not include any other text, markdown formatting, or explanations outside the JSON.
                
                The JSON must match this exact structure:
                {
                  "title": "A catchy, relevant title for the quiz",
                  "questions": [
                    {
                      "text": "The question text here?",
                      "options": ["Option A", "Option B", "Option C", "Option D"],
                      "correct": 0
                    }
                  ]
                }
                
                Guidelines:
                - Generate exactly 5 questions.
                - Each question MUST have exactly 4 options.
                - The "correct" field must be the 0-based index of the correct option (0, 1, 2, or 3).
                - Make the questions challenging but fair.
                - Ensure there is only one unambiguously correct answer per question.
                - Always generate all content in English only.
                <|im_end|>
                <|im_start|>user
                Generate a quiz about: $topic.
                <|im_end|>
                <|im_start|>assistant
            """.trimIndent()

            try {
                val responseJson = llmManager.generateCompleteResponse(prompt)
                // Extract JSON block using regex or string manipulation to handle conversational fluff
                val cleanJson = responseJson.replace("```json", "").replace("```", "").trim()
                
                // Find the first { and last } to isolate the JSON object
                val startIndex = cleanJson.indexOf('{')
                val endIndex = cleanJson.lastIndexOf('}')
                
                if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) {
                    throw Exception("No valid JSON object found in response.")
                }
                
                val jsonSubstring = cleanJson.substring(startIndex, endIndex + 1)
                val jsonObject = JSONObject(jsonSubstring)
                
                val title = jsonObject.optString("title", "$topic Quiz")
                val questionsArray = jsonObject.getJSONArray("questions")
                
                // Save to DB
                val quizId = quizDao.insertQuiz(QuizEntity(title = title, topic = topic))
                
                val questionEntities = mutableListOf<QuestionEntity>()
                for (i in 0 until questionsArray.length()) {
                    val qObj = questionsArray.getJSONObject(i)
                    val text = qObj.getString("text")
                    val optionsArray = qObj.getJSONArray("options")
                    val options = mutableListOf<String>()
                    for (j in 0 until optionsArray.length()) {
                        options.add(optionsArray.getString(j))
                    }
                    val correct = qObj.getInt("correct")
                    
                    questionEntities.add(
                        QuestionEntity(
                            quizId = quizId,
                            questionText = text,
                            options = options,
                            correctAnswerIndex = correct
                        )
                    )
                }
                
                quizDao.insertQuestions(questionEntities)
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Failed to generate quiz", e)
                _error.value = "Failed to generate quiz: Ensure a reasoning model is loaded and try again."
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    fun deleteQuiz(quiz: QuizEntity) {
        viewModelScope.launch {
            quizDao.deleteQuiz(quiz)
        }
    }
}
