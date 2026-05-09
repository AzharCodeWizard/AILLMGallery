package com.azhar.aillmgallery.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            
            // Set language to English
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsManager", "Language not supported")
            } else {
                selectBestVoice()
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isPlaying.value = false
                }

                @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                }
                
                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isPlaying.value = false
                }
            })
        } else {
            Log.e("TtsManager", "Initialization Failed!")
        }
    }

    private fun selectBestVoice() {
        // Attempt to find a high-quality "network" voice which sounds more human
        val voices = tts?.voices ?: return
        
        // Find a network voice for English US
        val bestVoice = voices.firstOrNull { voice ->
            voice.locale.language == Locale.US.language &&
                    voice.isNetworkConnectionRequired && // Network voices are usually higher quality (wavenet)
                    !voice.features.contains("notInstalled")
        } ?: voices.firstOrNull { voice -> 
            voice.locale.language == Locale.US.language && 
            voice.quality >= Voice.QUALITY_HIGH 
        }

        if (bestVoice != null) {
            tts?.voice = bestVoice
            Log.d("TtsManager", "Selected voice: ${bestVoice.name}")
        } else {
            Log.d("TtsManager", "Falling back to default voice")
        }
    }

    fun speak(text: String) {
        if (!isInitialized) return
        
        // We use QUEUE_FLUSH to immediately start speaking new text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID_${System.currentTimeMillis()}")
    }

    fun stop() {
        if (!isInitialized) return
        tts?.stop()
        _isPlaying.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
