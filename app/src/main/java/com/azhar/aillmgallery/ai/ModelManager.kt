package com.azhar.aillmgallery.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages model file discovery and status.
 * Models should be pushed to the app's files directory via:
 *   adb push model.task /data/local/tmp/llm/
 * or placed in the app's filesDir.
 */
class ModelManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelManager"
        private const val MODELS_DIR = "models"
        private val SUPPORTED_EXTENSIONS = listOf(".bin", ".task", ".tflite")
    }

    private val _modelState = MutableStateFlow<ModelState>(ModelState.Checking)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels.asStateFlow()

    /**
     * Scan for available model files in the app's directory.
     */
    suspend fun scanForModels() = withContext(Dispatchers.IO) {
        _modelState.value = ModelState.Checking

        try {
            val modelsDir = File(context.filesDir, MODELS_DIR)
            val externalModelsDir = File("/data/local/tmp/llm")

            val modelFiles = mutableListOf<ModelInfo>()

            // Check internal app storage
            if (modelsDir.exists() && modelsDir.isDirectory) {
                modelsDir.listFiles()?.filter { file ->
                    SUPPORTED_EXTENSIONS.any { file.name.endsWith(it, ignoreCase = true) }
                }?.forEach { file ->
                    modelFiles.add(
                        ModelInfo(
                            name = file.nameWithoutExtension,
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            location = "Internal Storage"
                        )
                    )
                }
            }

            // Check external /data/local/tmp/llm (dev convenience)
            if (externalModelsDir.exists() && externalModelsDir.isDirectory) {
                externalModelsDir.listFiles()?.filter { file ->
                    SUPPORTED_EXTENSIONS.any { file.name.endsWith(it, ignoreCase = true) }
                }?.forEach { file ->
                    modelFiles.add(
                        ModelInfo(
                            name = file.nameWithoutExtension,
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            location = "ADB Push"
                        )
                    )
                }
            }

            _availableModels.value = modelFiles
            _modelState.value = if (modelFiles.isNotEmpty()) {
                ModelState.Ready(modelFiles.first())
            } else {
                ModelState.NotFound
            }

            Log.d(TAG, "Found ${modelFiles.size} models")
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning for models", e)
            _modelState.value = ModelState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Get the models directory path for display in the UI.
     */
    fun getModelsDirectoryPath(): String {
        return File(context.filesDir, MODELS_DIR).absolutePath
    }

    /**
     * Ensure the models directory exists.
     */
    fun ensureModelsDirectory() {
        val dir = File(context.filesDir, MODELS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }
}

sealed class ModelState {
    data object Checking : ModelState()
    data object NotFound : ModelState()
    data class Ready(val model: ModelInfo) : ModelState()
    data class Loading(val progress: Float = 0f) : ModelState()
    data class Error(val message: String) : ModelState()
}

data class ModelInfo(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val location: String
) {
    val sizeDisplay: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024) {
                "%.1f GB".format(mb / 1024.0)
            } else {
                "%.0f MB".format(mb)
            }
        }
}
