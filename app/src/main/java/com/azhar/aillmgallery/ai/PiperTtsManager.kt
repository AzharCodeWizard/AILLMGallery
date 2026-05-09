package com.azhar.aillmgallery.ai

import android.content.Context
import android.media.AudioAttributes
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Manager for Kokoro TTS via sherpa-onnx.
 * Provides the highest-quality on-device neural text-to-speech with 11 English speaker voices.
 *
 * Voice model is downloaded on demand to filesDir/tts_models/
 * espeak-ng-data is included in the model archive.
 */
class PiperTtsManager(private val context: Context) {

    companion object {
        private const val TAG = "KokoroTtsManager"
        private const val MODELS_DIR = "tts_models"

        // Kokoro English-only model (11 speakers)
        const val DEFAULT_MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-en-v0_19.tar.bz2"
        const val DEFAULT_MODEL_DIR = "kokoro-en-v0_19"
        const val DEFAULT_MODEL_FILE = "model.onnx"
        const val DEFAULT_VOICES_FILE = "voices.bin"
        const val DEFAULT_TOKENS_FILE = "tokens.txt"
        const val DEFAULT_ESPEAK_DIR = "espeak-ng-data"

        // Speaker name → ID map for Kokoro English v0.19
        val SPEAKERS = mapOf(
            "Alloy (Female)" to 0,
            "Bella (Female)" to 1,
            "Heart (Female)" to 2,
            "Nicole (Female)" to 3,
            "Nova (Female)" to 4,
            "River (Female)" to 5,
            "Sarah (Female)" to 6,
            "Sky (Female)" to 7,
            "Adam (Male)" to 8,
            "Echo (Male)" to 9,
            "Eric (Male)" to 10
        )
        val DEFAULT_SPEAKER_ID = 2 // Heart — warm, expressive female voice
    }

    private var tts: OfflineTts? = null
    private var mediaPlayer: android.media.MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private var selectedSpeakerId: Int = DEFAULT_SPEAKER_ID

    fun setSpeaker(speakerId: Int) {
        selectedSpeakerId = speakerId
    }

    fun getSpeakers(): Map<String, Int> = SPEAKERS

    fun getModelDir(): File = File(context.filesDir, "$MODELS_DIR/$DEFAULT_MODEL_DIR")

    fun isModelDownloaded(): Boolean {
        val dir = getModelDir()
        val hasModel = File(dir, DEFAULT_MODEL_FILE).exists()
        val hasVoices = File(dir, DEFAULT_VOICES_FILE).exists()
        val hasTokens = File(dir, DEFAULT_TOKENS_FILE).exists()
        Log.d(TAG, "isModelDownloaded: model=$hasModel, voices=$hasVoices, tokens=$hasTokens (dir=${dir.absolutePath})")
        return hasModel && hasVoices && hasTokens
    }

    /**
     * Downloads the Kokoro voice model to internal storage.
     */
    suspend fun downloadVoiceModel(
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _isDownloading.value = true
            _downloadProgress.value = 0f

            val modelsDir = File(context.filesDir, MODELS_DIR)
            modelsDir.mkdirs()

            val tarFile = File(modelsDir, "voice-model.tar.bz2")

            Log.d(TAG, "Downloading Kokoro model from $DEFAULT_MODEL_URL")

            // Download using OkHttp
            val client = okhttp3.OkHttpClient.Builder()
                .followRedirects(true)
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val request = okhttp3.Request.Builder().url(DEFAULT_MODEL_URL).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Download HTTP error: ${response.code}")
                return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response"))
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            Log.d(TAG, "Download started: $totalBytes bytes total")

            body.byteStream().use { input ->
                FileOutputStream(tarFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                        _downloadProgress.value = progress
                        onProgress(progress)
                    }
                }
            }

            Log.d(TAG, "Download complete: ${tarFile.length()} bytes. Extracting...")

            // Extract tar.bz2 using pure Java (Android doesn't have tar command)
            extractTarBz2(tarFile, modelsDir)
            tarFile.delete()

            // Verify files
            val modelDir = getModelDir()
            Log.d(TAG, "Extraction complete. Contents of ${modelDir.absolutePath}:")
            modelDir.listFiles()?.forEach { f ->
                Log.d(TAG, "  ${f.name} (${if (f.isDirectory) "dir" else "${f.length()} bytes"})")
            }

            if (!isModelDownloaded()) {
                return@withContext Result.failure(Exception("Extraction failed: model files not found"))
            }

            _downloadProgress.value = 1f
            Log.d(TAG, "Kokoro voice model ready!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download voice model", e)
            Result.failure(e)
        } finally {
            _isDownloading.value = false
        }
    }

    /**
     * Extracts a .tar.bz2 archive using pure Java.
     */
    private fun extractTarBz2(archiveFile: File, destDir: File) {
        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis).use { bis ->
                BZip2CompressorInputStream(bis).use { bzis ->
                    TarArchiveInputStream(bzis).use { tarIn ->
                        var entry = tarIn.nextEntry
                        while (entry != null) {
                            val outFile = File(destDir, entry.name)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    tarIn.copyTo(fos)
                                }
                            }
                            entry = tarIn.nextEntry
                        }
                    }
                }
            }
        }
    }

    /**
     * Initializes the Kokoro TTS engine.
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (tts != null) {
                _isReady.value = true
                return@withContext Result.success(Unit)
            }

            if (!isModelDownloaded()) {
                Log.w(TAG, "Cannot initialize: model not downloaded")
                return@withContext Result.failure(Exception("Voice model not downloaded"))
            }

            val modelDir = getModelDir()
            val modelPath = File(modelDir, DEFAULT_MODEL_FILE).absolutePath
            val voicesPath = File(modelDir, DEFAULT_VOICES_FILE).absolutePath
            val tokensPath = File(modelDir, DEFAULT_TOKENS_FILE).absolutePath
            val espeakDataDir = File(modelDir, DEFAULT_ESPEAK_DIR).absolutePath

            Log.d(TAG, "Initializing Kokoro TTS:")
            Log.d(TAG, "  model=$modelPath (${File(modelPath).length()} bytes)")
            Log.d(TAG, "  voices=$voicesPath (${File(voicesPath).length()} bytes)")
            Log.d(TAG, "  tokens=$tokensPath (${File(tokensPath).length()} bytes)")
            Log.d(TAG, "  espeak=$espeakDataDir (exists=${File(espeakDataDir).exists()})")

            val kokoroConfig = OfflineTtsKokoroModelConfig(
                model = modelPath,
                voices = voicesPath,
                tokens = tokensPath,
                dataDir = espeakDataDir
            )

            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = kokoroConfig,
                    numThreads = 2,
                    debug = true
                )
            )

            Log.d(TAG, "Creating OfflineTts instance...")
            tts = OfflineTts(config = config)
            _isReady.value = true
            Log.d(TAG, "Kokoro TTS initialized! Sample rate: ${tts?.sampleRate()}, speakers: ${tts?.numSpeakers()}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Kokoro TTS", e)
            _isReady.value = false
            Result.failure(e)
        }
    }

    /**
     * Generates and plays speech from text using Kokoro TTS.
     */
    suspend fun speak(text: String, speed: Float = 1.0f) = withContext(Dispatchers.IO) {
        val engine = tts ?: run {
            Log.w(TAG, "TTS not initialized, cannot speak")
            return@withContext
        }

        stop()

        try {
            _isPlaying.value = true
            Log.d(TAG, "Generating speech: speaker=$selectedSpeakerId, text length=${text.length}")

            // Generate audio samples with the selected speaker
            val audio = engine.generate(text = text, speed = speed, sid = selectedSpeakerId)
            val samples = audio.samples

            if (samples.isEmpty()) {
                Log.w(TAG, "No audio samples generated!")
                _isPlaying.value = false
                return@withContext
            }

            Log.d(TAG, "Generated ${samples.size} samples at ${audio.sampleRate} Hz")

            // Convert float samples to PCM 16-bit
            val pcmData = ShortArray(samples.size) { i ->
                (samples[i] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            }

            // Write WAV file and play with MediaPlayer (more reliable than AudioTrack)
            val wavFile = File(context.cacheDir, "tts_output.wav")
            writeWav(wavFile, pcmData, audio.sampleRate)
            Log.d(TAG, "WAV written: ${wavFile.length()} bytes")

            // Play using MediaPlayer
            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(wavFile.absolutePath)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnCompletionListener {
                    _isPlaying.value = false
                    Log.d(TAG, "Playback complete")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    _isPlaying.value = false
                    true
                }
                prepare()
                start()
            }
            Log.d(TAG, "MediaPlayer started!")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to speak", e)
            _isPlaying.value = false
        }
    }

    /**
     * Writes PCM 16-bit mono data to a WAV file.
     */
    private fun writeWav(file: File, pcmData: ShortArray, sampleRate: Int) {
        val byteRate = sampleRate * 2 // 16-bit mono
        val dataSize = pcmData.size * 2
        val totalSize = 36 + dataSize

        FileOutputStream(file).use { fos ->
            val buffer = java.nio.ByteBuffer.allocate(44 + dataSize)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN)

            // RIFF header
            buffer.put("RIFF".toByteArray())
            buffer.putInt(totalSize)
            buffer.put("WAVE".toByteArray())

            // fmt chunk
            buffer.put("fmt ".toByteArray())
            buffer.putInt(16) // chunk size
            buffer.putShort(1) // PCM format
            buffer.putShort(1) // mono
            buffer.putInt(sampleRate)
            buffer.putInt(byteRate)
            buffer.putShort(2) // block align
            buffer.putShort(16) // bits per sample

            // data chunk
            buffer.put("data".toByteArray())
            buffer.putInt(dataSize)

            // PCM samples
            for (sample in pcmData) {
                buffer.putShort(sample)
            }

            fos.write(buffer.array())
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null
        } catch (_: Exception) {
        }
        _isPlaying.value = false
    }

    fun shutdown() {
        stop()
        tts?.release()
        tts = null
        _isReady.value = false
    }
}

