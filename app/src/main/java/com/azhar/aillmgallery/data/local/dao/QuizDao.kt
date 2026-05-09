package com.azhar.aillmgallery.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.azhar.aillmgallery.data.local.entity.QuestionEntity
import com.azhar.aillmgallery.data.local.entity.QuizEntity
import com.azhar.aillmgallery.data.local.entity.QuizWithQuestions
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Transaction
    @Query("SELECT * FROM quizzes ORDER BY timestamp DESC")
    fun getAllQuizzesWithQuestions(): Flow<List<QuizWithQuestions>>

    @Transaction
    @Query("SELECT * FROM quizzes WHERE id = :quizId")
    suspend fun getQuizWithQuestions(quizId: Long): QuizWithQuestions?

    @Delete
    suspend fun deleteQuiz(quiz: QuizEntity)
}
