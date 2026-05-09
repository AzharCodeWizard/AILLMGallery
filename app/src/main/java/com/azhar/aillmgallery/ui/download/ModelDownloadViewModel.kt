package com.azhar.aillmgallery.ui.download

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azhar.aillmgallery.ai.DownloadState
import com.azhar.aillmgallery.ai.DownloadableModel
import com.azhar.aillmgallery.ai.ModelCatalog
import com.azhar.aillmgallery.ai.ModelDownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = ModelDownloadManager(application)

    val downloadStates: StateFlow<Map<String, DownloadState>> = downloadManager.downloadStates

    val models: List<DownloadableModel> = ModelCatalog.models

    private val _downloadedModelIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedModelIds: StateFlow<Set<String>> = _downloadedModelIds.asStateFlow()

    init {
        refreshDownloadedModels()
    }

    fun refreshDownloadedModels() {
        _downloadedModelIds.value = downloadManager.getDownloadedModelIds()
    }

    fun downloadModel(model: DownloadableModel) {
        viewModelScope.launch {
            downloadManager.downloadModel(model)
            refreshDownloadedModels()
        }
    }

    fun cancelDownload(modelId: String) {
        downloadManager.cancelDownload(modelId)
    }

    fun deleteModel(model: DownloadableModel) {
        downloadManager.deleteModel(model)
        refreshDownloadedModels()
    }

    fun isModelDownloaded(model: DownloadableModel): Boolean {
        return model.id in _downloadedModelIds.value
    }
}
