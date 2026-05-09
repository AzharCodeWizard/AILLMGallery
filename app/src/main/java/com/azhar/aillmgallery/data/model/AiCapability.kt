package com.azhar.aillmgallery.data.model

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.azhar.aillmgallery.R

/**
 * Represents a single AI capability that the app can demonstrate.
 */
data class AiCapability(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val category: CapabilityCategory,
    @ArrayRes val examplePromptsRes: Int,
    val requiresImage: Boolean = false
)

enum class CapabilityCategory(@StringRes val displayNameRes: Int) {
    TEXT(R.string.category_text),
    VISION(R.string.category_vision),
    MULTIMODAL(R.string.category_multimodal),
    CODE(R.string.category_code)
}

/**
 * Predefined AI capabilities to showcase in the gallery.
 */
object AiCapabilities {
    val all = listOf(
        AiCapability(
            id = "text_generation",
            titleRes = R.string.capability_title_creative_writing,
            descriptionRes = R.string.capability_desc_creative_writing,
            icon = Icons.Outlined.AutoAwesome,
            category = CapabilityCategory.TEXT,
            examplePromptsRes = R.array.capability_prompts_creative_writing
        ),
        AiCapability(
            id = "summarization",
            titleRes = R.string.capability_title_summarization,
            descriptionRes = R.string.capability_desc_summarization,
            icon = Icons.Outlined.Description,
            category = CapabilityCategory.TEXT,
            examplePromptsRes = R.array.capability_prompts_summarization
        ),
        AiCapability(
            id = "code_assistant",
            titleRes = R.string.capability_title_code_assistant,
            descriptionRes = R.string.capability_desc_code_assistant,
            icon = Icons.Outlined.Code,
            category = CapabilityCategory.CODE,
            examplePromptsRes = R.array.capability_prompts_code_assistant
        ),
        AiCapability(
            id = "image_captioning",
            titleRes = R.string.capability_title_image_captioning,
            descriptionRes = R.string.capability_desc_image_captioning,
            icon = Icons.Outlined.Image,
            category = CapabilityCategory.VISION,
            examplePromptsRes = R.array.capability_prompts_image_captioning,
            requiresImage = true
        ),
        AiCapability(
            id = "visual_qa",
            titleRes = R.string.capability_title_visual_qa,
            descriptionRes = R.string.capability_desc_visual_qa,
            icon = Icons.Outlined.QuestionAnswer,
            category = CapabilityCategory.VISION,
            examplePromptsRes = R.array.capability_prompts_visual_qa,
            requiresImage = true
        ),
        AiCapability(
            id = "object_detection",
            titleRes = R.string.capability_title_object_detection,
            descriptionRes = R.string.capability_desc_object_detection,
            icon = Icons.Outlined.Search,
            category = CapabilityCategory.VISION,
            examplePromptsRes = R.array.capability_prompts_object_detection,
            requiresImage = true
        )
    )
}
