package com.azhar.aillmgallery.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azhar.aillmgallery.ai.LlmInferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val llmManager: LlmInferenceManager = LlmInferenceManager.getInstance(application)

    private val _downloadedModels = MutableStateFlow<List<File>>(emptyList())
    val downloadedModels: StateFlow<List<File>> = _downloadedModels.asStateFlow()

    val activeModelName = llmManager.activeModelName

    init {
        refreshModels()
    }

    fun refreshModels() {
        _downloadedModels.value = llmManager.getDownloadedModels(getApplication())
    }

    fun selectModel(file: File) {
        viewModelScope.launch {
            llmManager.initialize(getApplication(), file.absolutePath)
        }
    }

    fun deleteModel(file: File) {
        if (llmManager.deleteModel(getApplication(), file.name)) {
            refreshModels()
        }
    }

    fun clearAllModels() {
        _downloadedModels.value.forEach {
            llmManager.deleteModel(getApplication(), it.name)
        }
        refreshModels()
    }

    fun activateDemoMode() {
        viewModelScope.launch {
            llmManager.initializeMock("Demo Engine (Manual)")
        }
    }
}
