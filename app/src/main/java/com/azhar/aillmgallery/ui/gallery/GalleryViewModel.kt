package com.azhar.aillmgallery.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azhar.aillmgallery.ai.EngineStatus
import com.azhar.aillmgallery.ai.LlmInferenceManager
import com.azhar.aillmgallery.ai.engineStatusIn
import com.azhar.aillmgallery.data.model.AiCapabilities
import com.azhar.aillmgallery.data.model.AiCapability
import com.azhar.aillmgallery.data.model.CapabilityCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val llmManager = LlmInferenceManager.getInstance(application)

    val engineStatus: StateFlow<EngineStatus> = llmManager.engineStatusIn(viewModelScope)

    private val _selectedCategory = MutableStateFlow<CapabilityCategory?>(null)
    val selectedCategory: StateFlow<CapabilityCategory?> = _selectedCategory.asStateFlow()

    private val _filteredCapabilities = MutableStateFlow(AiCapabilities.all)
    val filteredCapabilities: StateFlow<List<AiCapability>> = _filteredCapabilities.asStateFlow()

    init {
        // Warm the model up while the user browses, so the first prompt answers immediately.
        viewModelScope.launch { llmManager.ensureInitialized() }
    }

    fun selectCategory(category: CapabilityCategory?) {
        _selectedCategory.value = category
        _filteredCapabilities.value = if (category == null) {
            AiCapabilities.all
        } else {
            AiCapabilities.all.filter { it.category == category }
        }
    }
}
