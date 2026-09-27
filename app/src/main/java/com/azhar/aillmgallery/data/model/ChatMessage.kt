package com.azhar.aillmgallery.data.model

import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicLong

private val nextMessageId = AtomicLong(0)

/**
 * Represents a single message in the chat conversation.
 */
data class ChatMessage(
    val id: String = "msg-${nextMessageId.incrementAndGet()}",
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val imageBitmap: Bitmap? = null,
    val inferenceTimeMs: Long? = null,
    val tokensPerSecond: Float? = null,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)
