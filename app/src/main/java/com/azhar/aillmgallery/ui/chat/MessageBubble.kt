package com.azhar.aillmgallery.ui.chat

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.azhar.aillmgallery.data.model.ChatMessage
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun MessageBubble(
    message: ChatMessage,
    isLast: Boolean,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message.isUser) {
        UserMessage(message, modifier)
    } else {
        AssistantMessage(message, isLast, onRegenerate, modifier)
    }
}

@Composable
private fun UserMessage(message: ChatMessage, modifier: Modifier) {
    val maxWidth = LocalConfiguration.current.screenWidthDp.dp * 0.8f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.End
    ) {
        message.imageBitmap?.let { bitmap ->
            ImageThumbnail(bitmap = bitmap)
            Spacer(modifier = Modifier.height(6.dp))
        }
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(max = maxWidth)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun AssistantMessage(
    message: ChatMessage,
    isLast: Boolean,
    onRegenerate: () -> Unit,
    modifier: Modifier
) {
    val clipboard = LocalClipboardManager.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 24.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AssistantAvatar(isThinking = message.isStreaming)

        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                color = if (message.isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.animateContentSize()
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    when {
                        message.isStreaming && message.text.isEmpty() -> TypingIndicator()

                        // Plain text while streaming: re-parsing Markdown on every token is the
                        // main source of jank, so render Markdown once the answer is complete.
                        message.isStreaming -> StreamingText(message.text)

                        message.isError -> Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        else -> MarkdownText(
                            markdown = message.text.trim(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            fontResource = null,
                            isTextSelectable = true
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !message.isStreaming && !message.isError, enter = fadeIn()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    message.inferenceTimeMs?.let { StatsBadge(it, message.tokensPerSecond) }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(message.text)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isLast) {
                        IconButton(onClick = onRegenerate, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Regenerate",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingText(text: String) {
    val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursorAlpha"
    )
    val cursorColor = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha)
    Text(
        text = buildAnnotatedString {
            append(text)
            withStyle(SpanStyle(color = cursorColor)) { append(" ●") }
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun AssistantAvatar(isThinking: Boolean) {
    val colors = MaterialTheme.colorScheme
    val brush = remember(colors.primary, colors.tertiary) {
        Brush.linearGradient(listOf(colors.primary, colors.tertiary))
    }
    val alpha by rememberInfiniteTransition(label = "avatar").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "avatarAlpha"
    )
    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .size(32.dp)
            .alpha(if (isThinking) alpha else 1f)
            .clip(CircleShape)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ImageThumbnail(bitmap: Bitmap) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
        bitmap = image,
        contentDescription = "Attached image",
        modifier = Modifier
            .size(180.dp)
            .clip(RoundedCornerShape(20.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 160),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun StatsBadge(timeMs: Long, tokensPerSecond: Float?) {
    val time = if (timeMs >= 1000) "%.1fs".format(timeMs / 1000f) else "${timeMs}ms"
    val label = tokensPerSecond?.let { "$time · %.1f tok/s".format(it) } ?: time

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Speed,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
