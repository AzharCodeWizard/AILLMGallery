package com.azhar.aillmgallery.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation keys for the AI LLM Gallery app.
 * Navigation 3 uses state-driven navigation with serializable keys.
 */

@Serializable
data object GalleryKey

@Serializable
data class ChatKey(
    val capabilityId: String,
    val capabilityTitle: String
)

@Serializable
data object SettingsKey

@Serializable
data object ModelDownloadKey

@Serializable
data object QuizListKey

@Serializable
data class QuizPlayKey(val quizId: Long)

@Serializable
data object StoryKey
