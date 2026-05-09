package com.azhar.aillmgallery.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azhar.aillmgallery.data.model.CapabilityCategory

@Composable
fun CategoryChips(
    selectedCategory: CapabilityCategory?,
    onCategorySelected: (CapabilityCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(null) + CapabilityCategory.entries

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val label = category?.let { stringResource(id = it.displayNameRes) } ?: stringResource(id = com.azhar.aillmgallery.R.string.category_all)
            val isSelected = selectedCategory == category

            val icon = when (category) {
                null -> Icons.Outlined.GridView
                CapabilityCategory.TEXT -> Icons.Outlined.AutoAwesome
                CapabilityCategory.VISION -> Icons.Outlined.Image
                CapabilityCategory.MULTIMODAL -> Icons.Outlined.Layers
                CapabilityCategory.CODE -> Icons.Outlined.Code
            }

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
