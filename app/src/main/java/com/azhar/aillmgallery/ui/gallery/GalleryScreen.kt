package com.azhar.aillmgallery.ui.gallery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.azhar.aillmgallery.ai.EngineStatus
import com.azhar.aillmgallery.ui.components.BackendBadge
import com.azhar.aillmgallery.ui.components.CategoryChips

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onCapabilitySelected: (id: String, title: String) -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onQuizClick: () -> Unit,
    onStoryClick: () -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    val capabilities by viewModel.filteredCapabilities.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val engineStatus by viewModel.engineStatus.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI LLM Gallery",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Private AI, running on your phone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        // One scrolling grid (headers as full-width items) so the whole page scrolls
        // together instead of squeezing the grid under a fixed header on small screens.
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 4.dp,
                bottom = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "engine", span = StaggeredGridItemSpan.FullLine) {
                EngineHeroCard(status = engineStatus, onDownloadClick = onDownloadClick)
            }

            item(key = "features-title", span = StaggeredGridItemSpan.FullLine) {
                SectionTitle("Create")
            }

            item(key = "features", span = StaggeredGridItemSpan.FullLine) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureCard(
                        title = "AI Quizzes",
                        subtitle = "Generate & learn",
                        icon = Icons.Outlined.School,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = onQuizClick,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        title = "Audio Scripts",
                        subtitle = "Write & listen",
                        icon = Icons.Outlined.Headphones,
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = onStoryClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "capabilities-title", span = StaggeredGridItemSpan.FullLine) {
                Column {
                    SectionTitle("Capabilities")
                    CategoryChips(
                        selectedCategory = selectedCategory,
                        onCategorySelected = viewModel::selectCategory,
                        // Cancel the grid's side padding so chips scroll edge to edge.
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .layoutFullBleed()
                    )
                }
            }

            items(items = capabilities, key = { it.id }) { capability ->
                val title = stringResource(id = capability.titleRes)
                CapabilityCard(
                    capability = capability,
                    onClick = { onCapabilitySelected(capability.id, title) },
                    modifier = Modifier.animateItem()
                )
            }

            item(key = "bottom-inset", span = StaggeredGridItemSpan.FullLine) {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

private fun Modifier.layoutFullBleed(): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val extra = 32.dp.roundToPx()
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = constraints.minWidth + extra,
                maxWidth = constraints.maxWidth + extra
            )
        )
        layout(placeable.width - extra, placeable.height) {
            placeable.place(-extra / 2, 0)
        }
    }
)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun EngineHeroCard(
    status: EngineStatus,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val brush = remember(colors.primaryContainer, colors.tertiaryContainer) {
        Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .background(brush)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            status.isLoading -> "Warming up the model…"
                            status.isDemo -> "Demo engine active"
                            else -> "Ready to go"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (status.isDemo || status.modelName.isBlank()) "No model loaded" else status.modelName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!status.isLoading && !status.isDemo) {
                            Spacer(Modifier.width(8.dp))
                            BackendBadge(backend = status.backend)
                        }
                    }
                }
            }

            if (status.isLoading) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape),
                    trackColor = colors.surface.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (status.isDemo) {
                        "Download a model to unlock real on-device AI."
                    } else {
                        "100% offline · nothing leaves your device"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                FilledTonalButton(onClick = onDownloadClick) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Models")
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "featureScale")

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(onContainer.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.75f)
                )
            }
        }
    }
}
