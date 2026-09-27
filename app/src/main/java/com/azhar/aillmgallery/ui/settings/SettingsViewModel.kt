package com.azhar.aillmgallery.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azhar.aillmgallery.ai.LlmInferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val llmManager: LlmInferenceManager = LlmInferenceManager.getInstance(application)

    private val _downloadedModels = MutableStateFlow<List<File>>(emptyList())
    val downloadedModels: StateFlow<List<File>> = _downloadedModels.asStateFlow()

    val activeModelName = llmManager.activeModelName
    val activeBackend = llmManager.activeBackend
    val isModelLoading = llmManager.isLoading
    val useGpu = llmManager.useGpu

    init {
        refreshModels()
        viewModelScope.launch { llmManager.ensureInitialized() }
    }

    fun refreshModels() {
        viewModelScope.launch {
            _downloadedModels.value = withContext(Dispatchers.IO) {
                llmManager.getDownloadedModels(getApplication())
            }
        }
    }

    fun selectModel(file: File) {
        viewModelScope.launch {
            llmManager.selectModel(file.absolutePath)
        }
    }

    fun setUseGpu(enabled: Boolean) {
        viewModelScope.launch {
            llmManager.setUseGpu(enabled)
        }
    }

    fun deleteModel(file: File) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { llmManager.deleteModel(getApplication(), file.name) }
            refreshModels()
            // If the active model was deleted, load the next one (or the demo engine).
            llmManager.ensureInitialized()
        }
    }

    fun clearAllModels() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _downloadedModels.value.forEach { llmManager.deleteModel(getApplication(), it.name) }
            }
            refreshModels()
            llmManager.ensureInitialized()
        }
    }

    fun activateDemoMode() {
        viewModelScope.launch { llmManager.initializeMock("Demo Engine (Manual)") }
    }
}
