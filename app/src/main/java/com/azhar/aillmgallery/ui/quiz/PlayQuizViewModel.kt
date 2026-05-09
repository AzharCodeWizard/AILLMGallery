package com.azhar.aillmgallery.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azhar.aillmgallery.data.local.AppDatabase
import com.azhar.aillmgallery.data.local.entity.QuizWithQuestions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayQuizViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val quizDao = db.quizDao()

    private val _quizState = MutableStateFlow<QuizWithQuestions?>(null)
    val quizState: StateFlow<QuizWithQuestions?> = _quizState.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()
    
    private val _selectedAnswerIndex = MutableStateFlow<Int?>(null)
    val selectedAnswerIndex: StateFlow<Int?> = _selectedAnswerIndex.asStateFlow()

    fun loadQuiz(quizId: Long) {
        viewModelScope.launch {
            _quizState.value = quizDao.getQuizWithQuestions(quizId)
            _currentQuestionIndex.value = 0
            _score.value = 0
            _isFinished.value = false
            _selectedAnswerIndex.value = null
        }
    }

    fun submitAnswer(index: Int) {
        if (_selectedAnswerIndex.value != null) return // Already answered
        
        _selectedAnswerIndex.value = index
        val currentQuestion = _quizState.value?.questions?.getOrNull(_currentQuestionIndex.value)
        
        if (currentQuestion != null && index == currentQuestion.correctAnswerIndex) {
            _score.value += 1
        }
    }

    fun nextQuestion() {
        val totalQuestions = _quizState.value?.questions?.size ?: 0
        if (_currentQuestionIndex.value + 1 < totalQuestions) {
            _currentQuestionIndex.value += 1
            _selectedAnswerIndex.value = null
        } else {
            _isFinished.value = true
        }
    }
}
