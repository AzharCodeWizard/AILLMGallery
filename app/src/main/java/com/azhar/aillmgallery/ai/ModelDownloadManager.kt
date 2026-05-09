package com.azhar.aillmgallery.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Manages downloading of AI model files with progress tracking.
 */
class ModelDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloadManager"
        private const val MODELS_DIR = "models"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    /**
     * Returns IDs of models that are already downloaded.
     */
    fun getDownloadedModelIds(): Set<String> {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists()) return emptySet()

        val downloadedFiles = modelsDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
        return ModelCatalog.models
            .filter { it.fileName in downloadedFiles }
            .map { it.id }
            .toSet()
    }

    /**
     * Check if a specific model is already downloaded.
     */
    fun isModelDownloaded(model: DownloadableModel): Boolean {
        val file = File(context.filesDir, "$MODELS_DIR/${model.fileName}")
        return file.exists() && file.length() > 0
    }

    /**
     * Download a model file with progress tracking.
     */
    suspend fun downloadModel(model: DownloadableModel): Result<File> = withContext(Dispatchers.IO) {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val targetFile = File(modelsDir, model.fileName)
        val tempFile = File(modelsDir, "${model.fileName}.tmp")

        // If already downloaded, return immediately
        if (targetFile.exists() && targetFile.length() > 0) {
            updateState(model.id, DownloadState.Completed(targetFile))
            return@withContext Result.success(targetFile)
        }

        try {
            updateState(model.id, DownloadState.Downloading(0f, 0L, model.sizeBytes))
            Log.d(TAG, "Starting download: ${model.name} from ${model.url}")

            val request = Request.Builder()
                .url(model.url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val error = "HTTP ${response.code}: ${response.message}"
                updateState(model.id, DownloadState.Failed(error))
                return@withContext Result.failure(Exception(error))
            }

            val body = response.body ?: run {
                updateState(model.id, DownloadState.Failed("Empty response body"))
                return@withContext Result.failure(Exception("Empty response body"))
            }

            val totalBytes = body.contentLength().let {
                if (it > 0) it else model.sizeBytes
            }

            var downloadedBytes = 0L

            body.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            downloadedBytes.toFloat() / totalBytes.toFloat()
                        } else 0f

                        updateState(
                            model.id,
                            DownloadState.Downloading(
                                progress = progress.coerceIn(0f, 1f),
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes
                            )
                        )
                    }
                }
            }

            // Rename temp file to final file
            if (tempFile.renameTo(targetFile)) {
                Log.d(TAG, "Download complete: ${model.name} (${targetFile.length()} bytes)")
                updateState(model.id, DownloadState.Completed(targetFile))
                Result.success(targetFile)
            } else {
                updateState(model.id, DownloadState.Failed("Failed to save file"))
                Result.failure(Exception("Failed to save file"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${model.name}", e)
            tempFile.delete()
            updateState(model.id, DownloadState.Failed(e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    /**
     * Cancel a download by cleaning up temp files.
     */
    fun cancelDownload(modelId: String) {
        val model = ModelCatalog.models.find { it.id == modelId } ?: return
        val tempFile = File(context.filesDir, "$MODELS_DIR/${model.fileName}.tmp")
        tempFile.delete()
        updateState(modelId, DownloadState.Idle)
    }

    /**
     * Delete a downloaded model.
     */
    fun deleteModel(model: DownloadableModel): Boolean {
        val file = File(context.filesDir, "$MODELS_DIR/${model.fileName}")
        val result = file.delete()
        if (result) {
            updateState(model.id, DownloadState.Idle)
        }
        return result
    }

    private fun updateState(modelId: String, state: DownloadState) {
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(modelId, state)
        }
    }
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState() {
        val progressPercent: Int get() = (progress * 100).toInt()

        val downloadedDisplay: String
            get() {
                val mb = downloadedBytes / (1024.0 * 1024.0)
                return if (mb >= 1024) "%.1f GB".format(mb / 1024.0)
                else "%.0f MB".format(mb)
            }

        val totalDisplay: String
            get() {
                val mb = totalBytes / (1024.0 * 1024.0)
                return if (mb >= 1024) "%.1f GB".format(mb / 1024.0)
                else "%.0f MB".format(mb)
            }
    }
    data class Completed(val file: File) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}
