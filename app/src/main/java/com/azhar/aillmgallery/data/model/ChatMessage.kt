package com.azhar.aillmgallery.data.model

import android.graphics.Bitmap

/**
 * Represents a single message in the chat conversation.
 */
data class ChatMessage(
    val id: String = System.nanoTime().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val imageBitmap: Bitmap? = null,
    val inferenceTimeMs: Long? = null,
    val isStreaming: Boolean = false
)
