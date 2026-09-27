package com.azhar.aillmgallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.azhar.aillmgallery.ai.EngineStatus

/** A compact "● Model · GPU" line with a pulsing dot while the model loads. */
@Composable
fun EngineStatusLine(
    status: EngineStatus,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = when {
            status.isLoading -> MaterialTheme.colorScheme.outline
            status.isDemo -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        label = "statusDot"
    )
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(if (status.isLoading) pulse else 1f)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = when {
                status.isLoading -> "Loading model…"
                status.isDemo -> "Demo mode · download a model"
                else -> status.modelName
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (!status.isLoading && !status.isDemo && status.backend.isNotBlank()) {
            BackendBadge(backend = status.backend)
        }
    }
}

@Composable
fun BackendBadge(backend: String, modifier: Modifier = Modifier) {
    val isGpu = backend.equals("GPU", ignoreCase = true)
    Text(
        text = backend.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = if (isGpu) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .clip(CircleShape)
            .background(if (isGpu) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
