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
import android.speech.tts.Voice
import android.util.Log
import com.example.model.SpeechSegment
import com.example.model.VoiceTone
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
 * Manages high-quality bilingual Android [TextToSpeech] with smart neural voice selection
 * (picking the sweetest, clearest high-quality voice per language — e.g. Bengali `bn-BD`/`bn-IN`
 * alongside English `en-US` or any world language) and real-time vowel/syllable lip-sync telemetry,
 * plus [SpeechRecognizer] voice input with live partial transcripts and RMS amplitude.
 */
class SpeechLipSyncEngine(private val context: Context) {

    companion object {
        private const val TAG = "SpeechLipSyncEngine"

        // Known warm/natural Google TTS voice identifiers prioritized over harsh legacy voices
        private val PREFERRED_WARM_VOICE_HINTS = listOf(
            "x-ban", "x-bnf", "x-bnd",       // Warm Bengali neural voices (BD / IN)
            "x-sfg", "x-tpf", "x-iob", "x-tpc", "x-iol", // Warm English female neural voices
            "x-eef", "x-vlf", "x-jdf", "x-kof", "x-hif", // Warm Spanish/French/JP/KR/HI neural
            "female", "wavenet", "neural"
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    @Volatile
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var lipSyncJob: Job? = null

    private var pendingQueue: ArrayDeque<SpeechSegment> = ArrayDeque()
    private var activeVoiceTone: VoiceTone = VoiceTone.SWEET_WARM
    @Volatile
    private var currentUtteranceText: String = ""
    @Volatile
    private var currentSpeechRate: Float = 0.92f
    @Volatile
    private var activeRangeStart: Int = -1
    @Volatile
    private var activeRangeEnd: Int = -1
    @Volatile
    private var lastRangeTimestampMs: Long = 0L
    @Volatile
    private var activeSequenceToken: Long = 0L

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
            // Prefer Google's high-quality TTS engine when available on device for sweeter voices
            val googleTtsPkg = "com.google.android.tts"
            val hasGoogleTts = try {
                context.packageManager.getPackageInfo(googleTtsPkg, 0)
                true
            } catch (_: Exception) {
                false
            }

            val listener = TextToSpeech.OnInitListener { status ->
                isTtsReady = status == TextToSpeech.SUCCESS
                if (isTtsReady) {
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            mainHandler.post {
                                if (utteranceId?.startsWith("lp_${activeSequenceToken}_") == true) {
                                    _isSpeaking.value = true
                                    startLipSyncDriver()
                                }
                            }
                        }

                        override fun onRangeStart(
                            utteranceId: String?,
                            start: Int,
                            end: Int,
                            frame: Int
                        ) {
                            if (utteranceId?.startsWith("lp_${activeSequenceToken}_") == true) {
                                activeRangeStart = start
                                activeRangeEnd = end
                                lastRangeTimestampMs = System.currentTimeMillis()
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            mainHandler.post {
                                if (utteranceId?.startsWith("lp_${activeSequenceToken}_") == true) {
                                    playNextQueuedSegment(activeSequenceToken)
                                }
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            mainHandler.post {
                                if (utteranceId?.startsWith("lp_${activeSequenceToken}_") == true) {
                                    playNextQueuedSegment(activeSequenceToken)
                                }
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            mainHandler.post {
                                if (utteranceId?.startsWith("lp_${activeSequenceToken}_") == true) {
                                    playNextQueuedSegment(activeSequenceToken)
                                }
                            }
                        }
                    })
                } else {
                    Log.w(TAG, "TextToSpeech init failed with status=$status")
                }
            }

            tts = if (hasGoogleTts) {
                TextToSpeech(context.applicationContext, listener, googleTtsPkg)
            } else {
                TextToSpeech(context.applicationContext, listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating TextToSpeech", e)
        }
    }

    fun speak(
        text: String,
        bcp47Locale: String,
        speechRate: Float,
        voiceTone: VoiceTone = VoiceTone.SWEET_WARM,
        messageId: Long? = null
    ) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        speakSegments(
            segments = listOf(SpeechSegment(text = cleanText, bcp47Locale = bcp47Locale)),
            speechRate = speechRate,
            voiceTone = voiceTone,
            messageId = messageId
        )
    }

    /**
     * Speaks a bilingual sequence of [SpeechSegment]s smoothly, switching to the highest-quality
     * warm voice for each segment's language (e.g., sweet Bengali explanation + crisp English
     * pronunciation + sweet Bengali meaning).
     */
    fun speakSegments(
        segments: List<SpeechSegment>,
        speechRate: Float,
        voiceTone: VoiceTone = VoiceTone.SWEET_WARM,
        messageId: Long? = null
    ) {
        val validSegments = segments
            .map { it.copy(text = cleanForSpeech(it.text)) }
            .filter { it.text.isNotBlank() }
        if (validSegments.isEmpty()) return

        stopListening()
        stopSpeaking()

        activeSequenceToken = System.currentTimeMillis()
        activeVoiceTone = voiceTone
        currentSpeechRate = (speechRate * voiceTone.rateMultiplier).coerceIn(0.65f, 1.35f)
        _speakingMessageId.value = messageId

        pendingQueue = ArrayDeque(validSegments)
        playNextQueuedSegment(activeSequenceToken)
    }

    private fun playNextQueuedSegment(token: Long) {
        if (token != activeSequenceToken) return
        val next = pendingQueue.removeFirstOrNull()
        if (next == null) {
            finishSpeaking()
            return
        }

        currentUtteranceText = next.text
        activeRangeStart = -1
        activeRangeEnd = -1
        lastRangeTimestampMs = 0L

        val engine = tts
        if (engine != null && isTtsReady) {
            try {
                applySweetVoiceForLocale(engine, next.bcp47Locale)
                engine.setSpeechRate(currentSpeechRate)
                engine.setPitch(activeVoiceTone.pitch)

                val utteranceId = "lp_${token}_${System.nanoTime()}"
                val status = engine.speak(next.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (status == TextToSpeech.SUCCESS) {
                    _isSpeaking.value = true
                    startLipSyncDriver()
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "TTS speak segment failed, falling back to visual cadence", e)
            }
        }

        // Visual-only fallback if TTS voice data isn't available on the device
        _isSpeaking.value = true
        startLipSyncDriver(autoAdvanceToken = token)
    }

    /**
     * Picks the sweetest, clearest, highest-quality neural voice installed on the Android device
     * for [bcp47Locale], with smart fallback across regional variants (e.g. `bn-BD` <-> `bn-IN`,
     * `en-US` <-> `en-GB`).
     */
    private fun applySweetVoiceForLocale(engine: TextToSpeech, bcp47Locale: String) {
        val targetLocale = Locale.forLanguageTag(bcp47Locale)
        val langCode = targetLocale.language.lowercase(Locale.US)

        val voices = try {
            engine.voices?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        val matchingVoices = voices.filter { voice ->
            !voice.isNetworkConnectionRequired.let { false } &&
                voice.locale != null &&
                voice.locale.language.equals(langCode, ignoreCase = true) &&
                !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }

        val bestVoice = matchingVoices.maxByOrNull { scoreVoiceSweetness(it, targetLocale) }
        if (bestVoice != null) {
            engine.voice = bestVoice
            return
        }

        // Fallback to setLanguage trying exact locale first, then alternate regional variant
        val exactRes = engine.setLanguage(targetLocale)
        if (exactRes == TextToSpeech.LANG_MISSING_DATA || exactRes == TextToSpeech.LANG_NOT_SUPPORTED) {
            val fallbackLocale = when (langCode) {
                "bn" -> Locale("bn", "IN")
                "en" -> Locale.US
                "es" -> Locale("es", "MX")
                "pt" -> Locale("pt", "BR")
                "zh" -> Locale.SIMPLIFIED_CHINESE
                else -> Locale(langCode)
            }
            val fallbackRes = engine.setLanguage(fallbackLocale)
            if (fallbackRes == TextToSpeech.LANG_MISSING_DATA || fallbackRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.US)
            }
        }
    }

    private fun scoreVoiceSweetness(voice: Voice, preferredLocale: Locale): Int {
        var score = 0
        val nameLower = voice.name.lowercase(Locale.US)

        // 1. Strongly prefer high/very-high neural quality over harsh legacy voices
        score += when (voice.quality) {
            Voice.QUALITY_VERY_HIGH -> 100
            Voice.QUALITY_HIGH -> 80
            Voice.QUALITY_NORMAL -> 40
            Voice.QUALITY_LOW -> 5
            else -> 0
        }

        // 2. Prefer exact country match (e.g. bn-BD over bn-IN, en-US over en-AU), slight bonus
        if (voice.locale.country.equals(preferredLocale.country, ignoreCase = true)) {
            score += 18
        }

        // 3. Prefer locally installed low-latency neural voices (no stutter)
        if (!voice.isNetworkConnectionRequired) {
            score += 16
        }

        // 4. Prefer warm/melodic female & neural voice profiles
        if (PREFERRED_WARM_VOICE_HINTS.any { nameLower.contains(it) }) {
            score += 35
        }
        if (nameLower.contains("local")) {
            score += 10
        }
        // Avoid robotic/novelty/legacy low-fidelity engine tags
        if (nameLower.contains("espeak") || nameLower.contains("pico") || nameLower.contains("legacy")) {
            score -= 80
        }

        return score
    }

    private fun cleanForSpeech(raw: String): String {
        return raw
            .replace("•", "")
            .replace("—", ", ")
            .replace("–", ", ")
            .replace("...", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun stopSpeaking() {
        activeSequenceToken = System.currentTimeMillis() + 1L
        pendingQueue.clear()
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

    private fun startLipSyncDriver(autoAdvanceToken: Long? = null) {
        lipSyncJob?.cancel()
        val text = currentUtteranceText
        val rate = currentSpeechRate
        val startTime = System.currentTimeMillis()
        val charsPerSec = 13.2f * rate
        val estimatedDurationMs = ((text.length / charsPerSec) * 1000f).toLong().coerceAtLeast(1100L)

        lipSyncJob = scope.launch {
            var smoothedOpen = 0f
            var smoothedWidth = 1f
            var smoothedEnergy = 0f

            while (isActive && _isSpeaking.value) {
                val now = System.currentTimeMillis()
                val elapsedMs = now - startTime

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
                    val wave = 0.78f + 0.22f * sin((elapsedMs / 1000.0) * rate * 16.5 * PI / 2.0).toFloat()
                    (targetOpen * wave).coerceIn(0f, 1f)
                }

                val alphaOpen = if (syllablePulse > smoothedOpen) 0.42f else 0.30f
                smoothedOpen += (syllablePulse - smoothedOpen) * alphaOpen
                smoothedWidth += (targetWidth - smoothedWidth) * 0.25f

                val targetEnergy = if (isPause) 0.10f else (0.34f + syllablePulse * 0.66f)
                smoothedEnergy += (targetEnergy - smoothedEnergy) * 0.30f

                _lipSyncFrame.value = LipSyncFrame(
                    mouthOpenness = smoothedOpen.coerceIn(0f, 1f),
                    mouthWidth = smoothedWidth.coerceIn(0.72f, 1.22f),
                    speechEnergy = smoothedEnergy.coerceIn(0f, 1f),
                    isQuestionTone = currentSentence.contains('?') || currentSentence.contains('？'),
                    isEmphasis = currentWord.length >= 7 || currentSentence.contains('!')
                )

                if (autoAdvanceToken != null && elapsedMs >= estimatedDurationMs + 180L) {
                    playNextQueuedSegment(autoAdvanceToken)
                    break
                }

                delay(24L)
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
            lower in setOf('a', 'á', 'à', 'â', 'ä', 'o', 'ó', 'ò', 'ô', 'অ', 'আ', 'া', 'ও', 'ো', 'あ', 'お', 'ا') -> {
                val rounded = lower in setOf('o', 'ó', 'ò', 'ô', 'অ', 'ও', 'ো', 'お')
                Triple(0.90f, if (rounded) 0.78f else 1.08f, false)
            }
            lower in setOf('e', 'é', 'è', 'i', 'í', 'u', 'ú', 'ü', 'y', 'ই', 'ঈ', 'উ', 'এ', 'ি', 'ী', 'ু', 'ে', 'い', 'う', 'え') -> {
                val rounded = lower in setOf('u', 'ú', 'ü', 'উ', 'ু', 'う')
                val spread = lower in setOf('e', 'é', 'i', 'í', 'ই', 'এ', 'い', 'え')
                Triple(0.58f, if (rounded) 0.74f else if (spread) 1.18f else 1.0f, false)
            }
            lower in setOf('m', 'b', 'p', 'ম', 'ব', 'প') ->
                Triple(0.14f, 0.94f, false)
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
