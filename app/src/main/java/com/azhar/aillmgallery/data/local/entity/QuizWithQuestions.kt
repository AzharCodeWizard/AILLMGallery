package com.azhar.aillmgallery.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class QuizWithQuestions(
    @Embedded val quiz: QuizEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "quizId"
    )
    val questions: List<QuestionEntity>
)
