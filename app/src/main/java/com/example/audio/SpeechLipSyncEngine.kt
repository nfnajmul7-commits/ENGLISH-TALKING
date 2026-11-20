package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

data class LipSyncFrame(
    val mouthOpenness: Float = 0f,     // 0f closed .. 1f wide open vowel
    val mouthWidth: Float = 1f,        // 0.72f rounded (o/u) .. 1.18f spread (e/i)
    val speechEnergy: Float = 0f,      // 0f..1f drives speaking ring & audio waveform
    val isQuestionTone: Boolean = false,
    val isEmphasis: Boolean = false
)

/**
 * Manages real Android [TextToSpeech] with real-time vowel/syllable lip-sync telemetry
 * (combining [UtteranceProgressListener.onRangeStart] with a speech-rate-calibrated syllable
 * cadence fallback) and real Android [SpeechRecognizer] voice input with live partial transcripts
 * and microphone RMS amplitude.
 */
class SpeechLipSyncEngine(private val context: Context) {

    companion object {
        private const val TAG = "SpeechLipSyncEngine"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    @Volatile
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var lipSyncJob: Job? = null

    @Volatile
    private var currentUtteranceText: String = ""
    @Volatile
    private var currentSpeechRate: Float = 1.0f
    @Volatile
    private var activeRangeStart: Int = -1
    @Volatile
    private var activeRangeEnd: Int = -1
    @Volatile
    private var lastRangeTimestampMs: Long = 0L

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    val speakingMessageId: StateFlow<Long?> = _speakingMessageId.asStateFlow()

    private val _lipSyncFrame = MutableStateFlow(LipSyncFrame())
    val lipSyncFrame: StateFlow<LipSyncFrame> = _lipSyncFrame.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _micAmplitude = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    private var onFinalTranscript: ((String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                isTtsReady = status == TextToSpeech.SUCCESS
                if (isTtsReady) {
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            mainHandler.post {
                                _isSpeaking.value = true
                                startLipSyncDriver()
                            }
                        }

                        override fun onRangeStart(
                            utteranceId: String?,
                            start: Int,
                            end: Int,
                            frame: Int
                        ) {
                            activeRangeStart = start
                            activeRangeEnd = end
                            lastRangeTimestampMs = System.currentTimeMillis()
                        }

                        override fun onDone(utteranceId: String?) {
                            mainHandler.post { finishSpeaking() }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            mainHandler.post { finishSpeaking() }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            mainHandler.post { finishSpeaking() }
                        }
                    })
                } else {
                    Log.w(TAG, "TextToSpeech init failed with status=$status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating TextToSpeech", e)
        }
    }

    fun speak(
        text: String,
        bcp47Locale: String,
        speechRate: Float,
        messageId: Long? = null
    ) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        stopListening()
        stopSpeaking()

        currentUtteranceText = cleanText
        currentSpeechRate = speechRate.coerceIn(0.6f, 1.5f)
        activeRangeStart = -1
        activeRangeEnd = -1
        lastRangeTimestampMs = 0L
        _speakingMessageId.value = messageId

        val engine = tts
        if (engine != null && isTtsReady) {
            try {
                val locale = Locale.forLanguageTag(bcp47Locale)
                val result = engine.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.US)
                }
                engine.setSpeechRate(currentSpeechRate)
                engine.setPitch(1.04f)
                val utteranceId = "lp_${System.currentTimeMillis()}"
                val speakStatus = engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (speakStatus == TextToSpeech.SUCCESS) {
                    _isSpeaking.value = true
                    startLipSyncDriver()
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "TTS speak failed, falling back to timed visual lip-sync", e)
            }
        }

        // Visual-only speech cadence fallback if device has no TTS voices installed
        _isSpeaking.value = true
        startLipSyncDriver(autoFinishOnEnd = true)
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        finishSpeaking()
    }

    private fun finishSpeaking() {
        lipSyncJob?.cancel()
        lipSyncJob = null
        _isSpeaking.value = false
        _speakingMessageId.value = null
        _lipSyncFrame.value = LipSyncFrame()
    }

    private fun startLipSyncDriver(autoFinishOnEnd: Boolean = false) {
        lipSyncJob?.cancel()
        val text = currentUtteranceText
        val rate = currentSpeechRate
        val startTime = System.currentTimeMillis()
        // Estimate ~14.5 characters per second at 1.0x speed
        val charsPerSec = 14.5f * rate
        val estimatedDurationMs = ((text.length / charsPerSec) * 1000f).toLong().coerceAtLeast(1400L)

        lipSyncJob = scope.launch {
            var smoothedOpen = 0f
            var smoothedWidth = 1f
            var smoothedEnergy = 0f

            while (isActive && _isSpeaking.value) {
                val now = System.currentTimeMillis()
                val elapsedMs = now - startTime

                // Determine active character window: prefer real TTS onRangeStart when recent (< 650ms)
                val useRealRange = activeRangeStart >= 0 &&
                    activeRangeStart < text.length &&
                    (now - lastRangeTimestampMs) < 650L

                val cursorIdx = if (useRealRange) {
                    val wordLen = (activeRangeEnd - activeRangeStart).coerceAtLeast(1)
                    val sinceRangeSec = (now - lastRangeTimestampMs) / 1000f
                    val subOffset = (sinceRangeSec * charsPerSec).toInt().coerceIn(0, wordLen - 1)
                    (activeRangeStart + subOffset).coerceIn(0, text.lastIndex)
                } else {
                    ((elapsedMs / 1000f) * charsPerSec).toInt().coerceIn(0, text.lastIndex.coerceAtLeast(0))
                }

                val ch = text.getOrNull(cursorIdx) ?: ' '
                val currentWord = extractWordAt(text, cursorIdx)
                val currentSentence = extractSentenceAt(text, cursorIdx)

                val (targetOpen, targetWidth, isPause) = classifyPhoneme(ch, elapsedMs, rate)
                val syllablePulse = if (isPause) {
                    0.02f
                } else {
                    val wave = 0.78f + 0.22f * sin((elapsedMs / 1000.0) * rate * 17.5 * PI / 2.0).toFloat()
                    (targetOpen * wave).coerceIn(0f, 1f)
                }

                // Smooth envelope so lips open briskly on vowels and close cleanly on pauses
                val alphaOpen = if (syllablePulse > smoothedOpen) 0.42f else 0.30f
                smoothedOpen += (syllablePulse - smoothedOpen) * alphaOpen
                smoothedWidth += (targetWidth - smoothedWidth) * 0.25f

                val targetEnergy = if (isPause) 0.12f else (0.35f + syllablePulse * 0.65f)
                smoothedEnergy += (targetEnergy - smoothedEnergy) * 0.32f

                _lipSyncFrame.value = LipSyncFrame(
                    mouthOpenness = smoothedOpen.coerceIn(0f, 1f),
                    mouthWidth = smoothedWidth.coerceIn(0.72f, 1.22f),
                    speechEnergy = smoothedEnergy.coerceIn(0f, 1f),
                    isQuestionTone = currentSentence.contains('?') || currentSentence.contains('？'),
                    isEmphasis = currentWord.length >= 7 || currentSentence.contains('!')
                )

                if (autoFinishOnEnd && elapsedMs >= estimatedDurationMs + 250L) {
                    finishSpeaking()
                    break
                }

                delay(24L) // ~42fps smooth lip & waveform animation
            }
        }
    }

    private fun classifyPhoneme(ch: Char, elapsedMs: Long, rate: Float): Triple<Float, Float, Boolean> {
        val lower = ch.lowercaseChar()
        return when {
            lower in setOf('.', '!', '?', ',', ';', ':', '।', '。', '？', '！', '\n') ->
                Triple(0.03f, 1.0f, true)
            lower.isWhitespace() ->
                Triple(0.08f, 1.0f, true)
            // Wide open vowels (Latin, Bengali, Japanese, Arabic, etc.)
            lower in setOf('a', 'á', 'à', 'â', 'ä', 'o', 'ó', 'ò', 'ô', 'অ', 'আ', 'া', 'あ', 'お', 'ا') -> {
                val rounded = lower in setOf('o', 'ó', 'ò', 'ô', 'অ', 'お')
                Triple(0.92f, if (rounded) 0.78f else 1.08f, false)
            }
            // Mid / front / rounded vowels
            lower in setOf('e', 'é', 'è', 'i', 'í', 'u', 'ú', 'ü', 'y', 'ই', 'ঈ', 'উ', 'এ', 'ি', 'ী', 'ু', 'ে', 'い', 'う', 'え') -> {
                val rounded = lower in setOf('u', 'ú', 'ü', 'উ', 'ু', 'う')
                val spread = lower in setOf('e', 'é', 'i', 'í', 'ই', 'এ', 'い', 'え')
                Triple(0.60f, if (rounded) 0.74f else if (spread) 1.18f else 1.0f, false)
            }
            // Bilabial closures (m, b, p) close briefly
            lower in setOf('m', 'b', 'p', 'ম', 'ব', 'প') ->
                Triple(0.14f, 0.94f, false)
            // Soft consonants
            else -> {
                val mod = abs(sin(elapsedMs * 0.015 * rate)).toFloat()
                Triple(0.24f + mod * 0.18f, 1.0f, false)
            }
        }
    }

    private fun extractWordAt(text: String, index: Int): String {
        if (text.isEmpty() || index !in text.indices) return ""
        var start = index
        var end = index
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        while (end < text.length && !text[end].isWhitespace()) end++
        return text.substring(start, end)
    }

    private fun extractSentenceAt(text: String, index: Int): String {
        if (text.isEmpty() || index !in text.indices) return ""
        val delimiters = setOf('.', '!', '?', '।', '。', '？', '！', '\n')
        var start = index
        var end = index
        while (start > 0 && text[start - 1] !in delimiters) start--
        while (end < text.length && text[end] !in delimiters) end++
        val inclusiveEnd = (end + 1).coerceAtMost(text.length)
        return text.substring(start, inclusiveEnd)
    }

    fun startListening(bcp47Locale: String, onResult: (String) -> Unit) {
        stopSpeaking()
        _voiceError.value = null
        _partialTranscript.value = ""
        onFinalTranscript = onResult

        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _voiceError.value = "Speech recognition service is unavailable on this device. Tap Type to reply!"
                _isListening.value = false
                return@post
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                        }

                        override fun onBeginningOfSpeech() {
                            _isListening.value = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Normalize typical -2..10 dB SpeechRecognizer range to 0..1
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _micAmplitude.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) = Unit

                        override fun onEndOfSpeech() {
                            _micAmplitude.value = 0f
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            _micAmplitude.value = 0f
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — try speaking again or tap Type."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard — tap Speak to try again."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                                SpeechRecognizer.ERROR_NETWORK,
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice recognition needs a network connection. You can tap Type anytime."
                                else -> "Voice input paused — tap Speak to retry or use Type."
                            }
                            _voiceError.value = msg
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            _micAmplitude.value = 0f
                            val text = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                ?.trim()
                            _partialTranscript.value = ""
                            if (!text.isNullOrEmpty()) {
                                onFinalTranscript?.invoke(text)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                ?.trim()
                            if (!partial.isNullOrEmpty()) {
                                _partialTranscript.value = partial
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, bcp47Locale)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, bcp47Locale)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                _isListening.value = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                _isListening.value = false
                _voiceError.value = "Could not start voice input. Please tap Type to chat."
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {
            }
            _isListening.value = false
            _micAmplitude.value = 0f
            _partialTranscript.value = ""
        }
    }

    fun clearVoiceError() {
        _voiceError.value = null
    }

    fun release() {
        stopSpeaking()
        stopListening()
        try {
            tts?.shutdown()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        scope.cancel()
    }
}
