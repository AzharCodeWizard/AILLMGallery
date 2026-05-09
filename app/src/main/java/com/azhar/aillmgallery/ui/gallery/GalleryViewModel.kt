package com.azhar.aillmgallery.ui.gallery

import androidx.lifecycle.ViewModel
import com.azhar.aillmgallery.data.model.AiCapabilities
import com.azhar.aillmgallery.data.model.AiCapability
import com.azhar.aillmgallery.data.model.CapabilityCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class GalleryViewModel : ViewModel() {

    private val _selectedCategory = MutableStateFlow<CapabilityCategory?>(null)
    val selectedCategory: StateFlow<CapabilityCategory?> = _selectedCategory.asStateFlow()

    private val _allCapabilities = MutableStateFlow(AiCapabilities.all)

    val filteredCapabilities: StateFlow<List<AiCapability>> = MutableStateFlow(AiCapabilities.all)

    fun selectCategory(category: CapabilityCategory?) {
        _selectedCategory.value = category
        val filtered = if (category == null) {
            _allCapabilities.value
        } else {
            _allCapabilities.value.filter { it.category == category }
        }
        (filteredCapabilities as MutableStateFlow).value = filtered
    }
}
