package com.azhar.aillmgallery.ai

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.inferenceDataStore by preferencesDataStore(name = "inference_prefs")

/**
 * Persists the user's model selection and backend preference across launches.
 */
class InferencePreferences(context: Context) {

    private val store = context.applicationContext.inferenceDataStore

    suspend fun selectedModelPath(): String? = store.data.first()[KEY_MODEL_PATH]

    suspend fun setSelectedModelPath(path: String?) {
        store.edit { prefs ->
            if (path == null) prefs.remove(KEY_MODEL_PATH) else prefs[KEY_MODEL_PATH] = path
        }
    }

    /** GPU is the default; the engine falls back to CPU automatically if the GPU delegate fails. */
    suspend fun useGpu(): Boolean = store.data.first()[KEY_USE_GPU] ?: true

    suspend fun setUseGpu(enabled: Boolean) {
        store.edit { it[KEY_USE_GPU] = enabled }
    }

    private companion object {
        val KEY_MODEL_PATH = stringPreferencesKey("selected_model_path")
        val KEY_USE_GPU = booleanPreferencesKey("use_gpu")
    }
}
