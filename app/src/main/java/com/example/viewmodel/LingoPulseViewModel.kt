package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.LipSyncFrame
import com.example.audio.SpeechLipSyncEngine
import com.example.data.AiTutorRepository
import com.example.data.LearningRepository
import com.example.data.ProgressSummary
import com.example.data.SettingsRepository
import com.example.data.VocabItemEntity
import com.example.model.AppTab
import com.example.model.AvatarState
import com.example.model.ChatMessageUi
import com.example.model.InspireSuggestion
import com.example.model.LanguageLevel
import com.example.model.RolePlayScenario
import com.example.model.TutorMode
import com.example.model.UserSettings
import com.example.model.VocabPhrase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LingoPulseUiState(
    val activeTab: AppTab = AppTab.HOME,
    val settings: UserSettings = UserSettings(),
    val tutorMode: TutorMode = TutorMode.LECTURE,
    val activeTopic: String = "Daily Life",
    val activeScenario: RolePlayScenario? = null,
    val messages: List<ChatMessageUi> = emptyList(),
    val savedVocab: List<VocabItemEntity> = emptyList(),
    val progress: ProgressSummary = ProgressSummary(),
    val avatarState: AvatarState = AvatarState.IDLE,
    val lipSyncFrame: LipSyncFrame = LipSyncFrame(),
    val micAmplitude: Float = 0f,
    val partialTranscript: String = "",
    val voiceError: String? = null,
    val speakingMessageId: Long? = null,
    val isThinking: Boolean = false,
    val isStageExpanded: Boolean = false,
    val isStageCollapsed: Boolean = false,
    val isTextInputVisible: Boolean = false,
    val textInputDraft: String = "",
    val inspireSuggestions: List<InspireSuggestion> = emptyList(),
    val isInspireVisible: Boolean = false,
    val hiddenHelperMessageIds: Set<Long> = emptySet(),
    val sessionSeconds: Int = 0,
    val isPlaceholderAi: Boolean = true,
    val statusBannerMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class LingoPulseViewModel(
    private val settingsRepository: SettingsRepository,
    private val learningRepository: LearningRepository,
    private val aiTutorRepository: AiTutorRepository,
    private val speechEngine: SpeechLipSyncEngine
) : ViewModel() {

    private val _activeTab = MutableStateFlow(AppTab.HOME)
    private val _tutorMode = MutableStateFlow(TutorMode.LECTURE)
    private val _activeTopic = MutableStateFlow("Daily Life")
    private val _activeScenario = MutableStateFlow<RolePlayScenario?>(null)
    private val _isThinking = MutableStateFlow(false)
    private val _isStageExpanded = MutableStateFlow(false)
    private val _isStageCollapsed = MutableStateFlow(false)
    private val _isTextInputVisible = MutableStateFlow(false)
    private val _textInputDraft = MutableStateFlow("")
    private val _inspireSuggestions = MutableStateFlow<List<InspireSuggestion>>(emptyList())
    private val _isInspireVisible = MutableStateFlow(false)
    private val _hiddenHelperMessageIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _sessionSeconds = MutableStateFlow(0)
    private val _statusBannerMessage = MutableStateFlow<String?>(null)

    private val settingsFlow: StateFlow<UserSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings()
    )

    private val currentSessionKeyFlow = combine(_activeTab, _activeScenario, _activeTopic) { tab, scenario, topic ->
        if (tab == AppTab.ROLE_PLAY && scenario != null) {
            "roleplay_${scenario.id}"
        } else {
            "home_${topic.lowercase().replace(" ", "_")}"
        }
    }

    private val currentMessagesFlow = currentSessionKeyFlow.flatMapLatest { key ->
        learningRepository.observeChat(key)
    }

    private val avatarStateFlow = combine(
        speechEngine.isListening,
        _isThinking,
        speechEngine.isSpeaking
    ) { listening, thinking, speaking ->
        when {
            listening -> AvatarState.LISTENING
            thinking -> AvatarState.THINKING
            speaking -> AvatarState.SPEAKING
            else -> AvatarState.IDLE
        }
    }

    // Group intermediate UI sub-states cleanly to stay within combine arity
    private val stageUiFlow = combine(
        avatarStateFlow,
        speechEngine.lipSyncFrame,
        speechEngine.micAmplitude,
        speechEngine.partialTranscript,
        speechEngine.voiceError
    ) { avatarState, lipSync, micAmp, partial, voiceErr ->
        StageAudioBundle(avatarState, lipSync, micAmp, partial, voiceErr)
    }

    private val controlsFlow = combine(
        _isStageExpanded,
        _isStageCollapsed,
        _isTextInputVisible,
        _textInputDraft,
        _sessionSeconds
    ) { expanded, collapsed, textVisible, draft, seconds ->
        StageControlsBundle(expanded, collapsed, textVisible, draft, seconds)
    }

    private val helperFlow = combine(
        _inspireSuggestions,
        _isInspireVisible,
        _hiddenHelperMessageIds,
        speechEngine.speakingMessageId,
        _statusBannerMessage
    ) { suggestions, inspireVis, hiddenHelpers, speakingId, banner ->
        HelperBundle(suggestions, inspireVis, hiddenHelpers, speakingId, banner)
    }

    private val navContextFlow = combine(
        _activeTab,
        _tutorMode,
        _activeTopic,
        _activeScenario,
        _isThinking
    ) { tab, mode, topic, scenario, thinking ->
        NavContextBundle(tab, mode, topic, scenario, thinking)
    }

    val uiState: StateFlow<LingoPulseUiState> = combine(
        navContextFlow,
        settingsFlow,
        currentMessagesFlow,
        learningRepository.allVocab,
        learningRepository.progressSummary,
        stageUiFlow,
        controlsFlow,
        helperFlow
    ) { values ->
        val nav = values[0] as NavContextBundle
        val settings = values[1] as UserSettings
        @Suppress("UNCHECKED_CAST")
        val messages = values[2] as List<ChatMessageUi>
        @Suppress("UNCHECKED_CAST")
        val vocab = values[3] as List<VocabItemEntity>
        val progress = values[4] as ProgressSummary
        val stage = values[5] as StageAudioBundle
        val controls = values[6] as StageControlsBundle
        val helper = values[7] as HelperBundle

        LingoPulseUiState(
            activeTab = nav.tab,
            settings = settings,
            tutorMode = nav.mode,
            activeTopic = nav.topic,
            activeScenario = nav.scenario,
            messages = messages,
            savedVocab = vocab,
            progress = progress,
            avatarState = stage.avatarState,
            lipSyncFrame = stage.lipSync,
            micAmplitude = stage.micAmp,
            partialTranscript = stage.partial,
            voiceError = stage.voiceErr,
            speakingMessageId = helper.speakingId,
            isThinking = nav.thinking,
            isStageExpanded = controls.expanded,
            isStageCollapsed = controls.collapsed,
            isTextInputVisible = controls.textVisible,
            textInputDraft = controls.draft,
            inspireSuggestions = helper.suggestions,
            isInspireVisible = helper.inspireVisible,
            hiddenHelperMessageIds = helper.hiddenHelpers,
            sessionSeconds = controls.seconds,
            isPlaceholderAi = aiTutorRepository.isPlaceholderMode,
            statusBannerMessage = helper.banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LingoPulseUiState(isPlaceholderAi = aiTutorRepository.isPlaceholderMode)
    )

    private var timerJob: Job? = null

    init {
        startSessionTimer()
        viewModelScope.launch {
            val initialSettings = settingsRepository.settingsFlow.first()
            val preferredTopic = initialSettings.interests.firstOrNull() ?: "Daily Life"
            _activeTopic.value = preferredTopic
            ensureSessionSeeded(sessionKey = "home_${preferredTopic.lowercase().replace(" ", "_")}", scenario = null, autoSpeak = false)
        }
    }

    private fun startSessionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val tab = _activeTab.value
                val inActiveLearning = tab == AppTab.HOME || (tab == AppTab.ROLE_PLAY && _activeScenario.value != null)
                if (inActiveLearning) {
                    val nextSec = _sessionSeconds.value + 1
                    _sessionSeconds.value = nextSec
                    if (nextSec % 60 == 0) {
                        learningRepository.logStudyMinutes(1)
                    }
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        if (_activeTab.value != tab) {
            speechEngine.stopListening()
            _isInspireVisible.value = false
            _isStageExpanded.value = false
            _activeTab.value = tab
        }
    }

    fun selectTopic(topic: String) {
        if (_activeTopic.value == topic) return
        speechEngine.stopSpeaking()
        _activeTopic.value = topic
        _isInspireVisible.value = false
        viewModelScope.launch {
            val key = "home_${topic.lowercase().replace(" ", "_")}"
            ensureSessionSeeded(key, scenario = null, autoSpeak = settingsFlow.value.autoPlayAudio)
        }
    }

    fun setTutorMode(mode: TutorMode) {
        if (_tutorMode.value == mode) return
        _tutorMode.value = mode
        _isInspireVisible.value = false
        if (_activeTab.value == AppTab.HOME) {
            refreshCurrentLessonIntro()
        }
    }

    fun refreshCurrentLessonIntro() {
        val scenario = if (_activeTab.value == AppTab.ROLE_PLAY) _activeScenario.value else null
        val key = activeSessionKey()
        viewModelScope.launch {
            speechEngine.stopSpeaking()
            _isThinking.value = true
            val settings = settingsFlow.value
            val response = if (scenario != null) {
                aiTutorRepository.generateRolePlayIntro(settings, scenario)
            } else {
                aiTutorRepository.generateLessonIntro(settings, _tutorMode.value, _activeTopic.value)
            }
            val msgId = learningRepository.saveTutorMessage(
                sessionKey = key,
                spokenText = response.spokenText,
                nativeExplanation = response.nativeExplanation,
                phrases = response.phrases,
                practicePrompt = response.practicePrompt,
                grammarFeedback = response.grammarFeedback,
                isSampleResponse = response.isSampleResponse
            )
            _isThinking.value = false
            if (settings.autoPlayAudio) {
                speakTutorResponse(
                    spokenText = response.spokenText,
                    nativeExplanation = response.nativeExplanation,
                    phrases = response.phrases,
                    practicePrompt = response.practicePrompt,
                    settings = settings,
                    messageId = msgId
                )
            }
        }
    }

    fun openRolePlayScenario(scenario: RolePlayScenario) {
        speechEngine.stopSpeaking()
        speechEngine.stopListening()
        _activeScenario.value = scenario
        _isStageExpanded.value = false
        _isStageCollapsed.value = false
        _isInspireVisible.value = false
        viewModelScope.launch {
            ensureSessionSeeded("roleplay_${scenario.id}", scenario = scenario, autoSpeak = settingsFlow.value.autoPlayAudio)
        }
    }

    fun exitRolePlayScenario() {
        speechEngine.stopSpeaking()
        speechEngine.stopListening()
        _isStageExpanded.value = false
        _isInspireVisible.value = false
        _activeScenario.value = null
    }

    private suspend fun ensureSessionSeeded(
        sessionKey: String,
        scenario: RolePlayScenario?,
        autoSpeak: Boolean
    ) {
        val existing = learningRepository.observeChat(sessionKey).first()
        if (existing.isNotEmpty()) return
        _isThinking.value = true
        val settings = settingsFlow.value
        val response = if (scenario != null) {
            aiTutorRepository.generateRolePlayIntro(settings, scenario)
        } else {
            aiTutorRepository.generateLessonIntro(settings, _tutorMode.value, _activeTopic.value)
        }
        val msgId = learningRepository.saveTutorMessage(
            sessionKey = sessionKey,
            spokenText = response.spokenText,
            nativeExplanation = response.nativeExplanation,
            phrases = response.phrases,
            practicePrompt = response.practicePrompt,
            grammarFeedback = response.grammarFeedback,
            isSampleResponse = response.isSampleResponse
        )
        _isThinking.value = false
        if (autoSpeak) {
            speakTutorResponse(
                spokenText = response.spokenText,
                nativeExplanation = response.nativeExplanation,
                phrases = response.phrases,
                practicePrompt = response.practicePrompt,
                settings = settings,
                messageId = msgId
            )
        }
    }

    fun updateTextDraft(value: String) {
        _textInputDraft.value = value
    }

    fun toggleTextInput() {
        _isTextInputVisible.value = !_isTextInputVisible.value
        if (_isTextInputVisible.value) {
            speechEngine.stopListening()
        }
    }

    fun sendUserMessage(rawText: String = _textInputDraft.value) {
        val text = rawText.trim()
        if (text.isEmpty() || _isThinking.value) return
        _textInputDraft.value = ""
        _isInspireVisible.value = false
        speechEngine.stopListening()
        speechEngine.stopSpeaking()

        val key = activeSessionKey()
        val scenario = if (_activeTab.value == AppTab.ROLE_PLAY) _activeScenario.value else null

        viewModelScope.launch {
            learningRepository.saveUserMessage(key, text)
            _isThinking.value = true
            val settings = settingsFlow.value
            val turnCount = uiState.value.messages.count { it.isUser } + 1
            val response = aiTutorRepository.sendMessage(
                settings = settings,
                mode = _tutorMode.value,
                scenario = scenario,
                userText = text,
                turnCount = turnCount
            )
            val msgId = learningRepository.saveTutorMessage(
                sessionKey = key,
                spokenText = response.spokenText,
                nativeExplanation = response.nativeExplanation,
                phrases = response.phrases,
                practicePrompt = response.practicePrompt,
                grammarFeedback = response.grammarFeedback,
                isSampleResponse = response.isSampleResponse
            )
            _isThinking.value = false
            if (uiState.value.progress.todayMinutes == 0) {
                learningRepository.logStudyMinutes(1)
            }
            if (settings.autoPlayAudio) {
                speakTutorResponse(
                    spokenText = response.spokenText,
                    nativeExplanation = response.nativeExplanation,
                    phrases = response.phrases,
                    practicePrompt = response.practicePrompt,
                    settings = settings,
                    messageId = msgId
                )
            }
        }
    }

    fun toggleVoiceListening() {
        if (speechEngine.isListening.value) {
            speechEngine.stopListening()
        } else {
            _isInspireVisible.value = false
            val targetLocale = settingsFlow.value.targetLanguage.bcp47
            speechEngine.startListening(targetLocale) { transcript ->
                sendUserMessage(transcript)
            }
        }
    }

    fun clearVoiceError() {
        speechEngine.clearVoiceError()
    }

    fun playMessageAudio(message: ChatMessageUi) {
        if (speechEngine.isSpeaking.value && speechEngine.speakingMessageId.value == message.id) {
            speechEngine.stopSpeaking()
        } else {
            val settings = settingsFlow.value
            speakTutorResponse(
                spokenText = message.spokenText,
                nativeExplanation = message.nativeExplanation,
                phrases = message.phrases,
                practicePrompt = message.practicePrompt,
                settings = settings,
                messageId = message.id
            )
        }
    }

    private fun speakTutorResponse(
        spokenText: String,
        nativeExplanation: String,
        phrases: List<VocabPhrase>,
        practicePrompt: String,
        settings: UserSettings,
        messageId: Long?
    ) {
        val targetLocale = settings.targetLanguage.bcp47
        val nativeLocale = settings.nativeLanguage.bcp47
        val segments = mutableListOf<com.example.model.SpeechSegment>()

        if (settings.studyInNativeLanguage) {
            // Speak warm native intro first (e.g., sweet Bengali voice!)
            if (nativeExplanation.isNotBlank()) {
                segments.add(com.example.model.SpeechSegment(nativeExplanation, nativeLocale))
            } else if (spokenText.isNotBlank()) {
                segments.add(com.example.model.SpeechSegment(spokenText, targetLocale))
            }
            // Speak each Target Language phrase in Target voice + its Native meaning in Native voice
            phrases.forEach { phrase ->
                if (phrase.targetText.isNotBlank()) {
                    segments.add(com.example.model.SpeechSegment(phrase.targetText, targetLocale))
                }
                if (phrase.nativeMeaning.isNotBlank()) {
                    segments.add(com.example.model.SpeechSegment(phrase.nativeMeaning, nativeLocale))
                }
            }
            // Speak friendly follow-up question in Native voice
            if (practicePrompt.isNotBlank()) {
                segments.add(com.example.model.SpeechSegment(practicePrompt, nativeLocale))
            }
        } else {
            if (spokenText.isNotBlank()) {
                segments.add(com.example.model.SpeechSegment(spokenText, targetLocale))
            }
            phrases.forEach { phrase ->
                if (phrase.targetText.isNotBlank()) {
                    segments.add(com.example.model.SpeechSegment(phrase.targetText, targetLocale))
                }
            }
        }

        speechEngine.speakSegments(
            segments = segments,
            speechRate = settings.speechRate,
            voiceTone = settings.voiceTone,
            messageId = messageId
        )
    }

    fun speakPhrase(text: String, langBcp47: String = settingsFlow.value.targetLanguage.bcp47) {
        val settings = settingsFlow.value
        speechEngine.speak(
            text = text,
            bcp47Locale = langBcp47,
            speechRate = settings.speechRate,
            voiceTone = settings.voiceTone,
            messageId = null
        )
    }

    fun speakPhraseWithMeaning(phrase: VocabPhrase) {
        val settings = settingsFlow.value
        val segments = buildList {
            add(com.example.model.SpeechSegment(phrase.targetText, settings.targetLanguage.bcp47))
            if (settings.studyInNativeLanguage && phrase.nativeMeaning.isNotBlank()) {
                add(com.example.model.SpeechSegment(phrase.nativeMeaning, settings.nativeLanguage.bcp47))
            }
        }
        speechEngine.speakSegments(
            segments = segments,
            speechRate = settings.speechRate,
            voiceTone = settings.voiceTone,
            messageId = null
        )
    }

    fun testSweetVoice() {
        val settings = settingsFlow.value
        val isBn = settings.nativeLanguageCode == "bn"
        val segments = if (isBn) {
            listOf(
                com.example.model.SpeechSegment("নমস্কার! আমি আরিয়া। চলুন একসাথে সুন্দরভাবে ভাষা শিখি।", settings.nativeLanguage.bcp47),
                com.example.model.SpeechSegment("Hello! Good morning!", settings.targetLanguage.bcp47),
                com.example.model.SpeechSegment("সকালে শুভেচ্ছা জানাতে এটি ব্যবহার করা হয়।", settings.nativeLanguage.bcp47)
            )
        } else {
            listOf(
                com.example.model.SpeechSegment("Hello! I am Aria, your language tutor.", settings.targetLanguage.bcp47)
            )
        }
        speechEngine.speakSegments(
            segments = segments,
            speechRate = settings.speechRate,
            voiceTone = settings.voiceTone,
            messageId = null
        )
    }

    fun stopAudio() {
        speechEngine.stopSpeaking()
    }

    fun toggleInspireHints() {
        if (_isInspireVisible.value) {
            _isInspireVisible.value = false
            return
        }
        viewModelScope.launch {
            val settings = settingsFlow.value
            val scenario = if (_activeTab.value == AppTab.ROLE_PLAY) _activeScenario.value else null
            val lastAi = uiState.value.messages.lastOrNull { !it.isUser }?.spokenText
            val hints = aiTutorRepository.generateInspireHints(settings, _tutorMode.value, scenario, lastAi)
            _inspireSuggestions.value = hints
            _isInspireVisible.value = true
        }
    }

    fun toggleMessageHelper(messageId: Long) {
        val current = _hiddenHelperMessageIds.value
        _hiddenHelperMessageIds.value = if (current.contains(messageId)) current - messageId else current + messageId
    }

    fun toggleStageExpanded() {
        _isStageExpanded.value = !_isStageExpanded.value
        if (_isStageExpanded.value) _isStageCollapsed.value = false
    }

    fun toggleStageCollapsed() {
        _isStageCollapsed.value = !_isStageCollapsed.value
        if (_isStageCollapsed.value) _isStageExpanded.value = false
    }

    fun cycleSpeechRate() {
        val current = settingsFlow.value.speechRate
        val next = when {
            current < 0.9f -> 1.0f
            current < 1.1f -> 1.2f
            else -> 0.8f
        }
        viewModelScope.launch {
            settingsRepository.setSpeechRate(next)
        }
    }

    fun saveVocabPhrase(phrase: VocabPhrase) {
        viewModelScope.launch {
            learningRepository.saveOrPracticeVocab(phrase, settingsFlow.value.targetLanguageCode)
            showBanner("Saved to Vocabulary Bank!")
        }
    }

    fun removeVocab(id: Long) {
        viewModelScope.launch {
            learningRepository.removeVocab(id)
        }
    }

    fun logLessonCompleted() {
        viewModelScope.launch {
            learningRepository.logCompletedLesson(bonusMinutes = 3)
            showBanner("Lesson logged! +3 min added to today's goal.")
        }
    }

    fun clearCurrentChat() {
        val key = activeSessionKey()
        val scenario = if (_activeTab.value == AppTab.ROLE_PLAY) _activeScenario.value else null
        viewModelScope.launch {
            speechEngine.stopSpeaking()
            learningRepository.clearSession(key)
            ensureSessionSeeded(key, scenario = scenario, autoSpeak = false)
            showBanner("Started a fresh conversation.")
        }
    }

    // Profile & Learning Settings actions
    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            settingsRepository.updateProfile(name, email)
            showBanner("Profile updated")
        }
    }

    fun updateAvatarUri(uri: String?) {
        viewModelScope.launch { settingsRepository.updateAvatarUri(uri) }
    }

    fun updateAvatarAccent(index: Int) {
        viewModelScope.launch { settingsRepository.updateAvatarAccent(index) }
    }

    fun setStudyInNativeLanguage(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setStudyInNativeLanguage(enabled)
            delay(120)
            refreshCurrentLessonIntro()
        }
    }

    fun setTargetLanguage(code: String) {
        viewModelScope.launch {
            settingsRepository.setTargetLanguage(code)
            val lang = com.example.model.WorldLanguagesCatalog.byCode(code)
            showBanner("Target language: ${lang.flag} ${lang.englishName}")
            delay(150)
            refreshCurrentLessonIntro()
        }
    }

    fun setNativeLanguage(code: String) {
        viewModelScope.launch {
            settingsRepository.setNativeLanguage(code)
            val lang = com.example.model.WorldLanguagesCatalog.byCode(code)
            showBanner("Native language: ${lang.flag} ${lang.nativeName}")
            delay(150)
            refreshCurrentLessonIntro()
        }
    }

    fun setLevel(level: LanguageLevel) {
        viewModelScope.launch {
            settingsRepository.setLevel(level)
            val isBn = settingsFlow.value.nativeLanguageCode == "bn"
            showBanner(if (isBn) "স্তর: ${level.bnLabel} (${level.cefr})" else "Level: ${level.label} (${level.cefr})")
            delay(150)
            refreshCurrentLessonIntro()
        }
    }

    fun toggleInterest(interest: String) {
        viewModelScope.launch { settingsRepository.toggleInterest(interest) }
    }

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoal(minutes) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReminderEnabled(enabled) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { settingsRepository.setReminderTime(hour, minute) }
    }

    fun setAutoPlayAudio(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPlayAudio(enabled) }
    }

    fun setVoiceTone(tone: com.example.model.VoiceTone) {
        viewModelScope.launch {
            settingsRepository.setVoiceTone(tone)
            showBanner("Voice: ${tone.label} (${tone.bnLabel})")
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            speechEngine.stopSpeaking()
            learningRepository.resetAllProgress()
            _sessionSeconds.value = 0
            showBanner("Progress & chat history reset")
            ensureSessionSeeded(activeSessionKey(), scenario = _activeScenario.value, autoSpeak = false)
        }
    }

    private fun activeSessionKey(): String {
        val scenario = _activeScenario.value
        return if (_activeTab.value == AppTab.ROLE_PLAY && scenario != null) {
            "roleplay_${scenario.id}"
        } else {
            "home_${_activeTopic.value.lowercase().replace(" ", "_")}"
        }
    }

    private fun showBanner(message: String) {
        viewModelScope.launch {
            _statusBannerMessage.value = message
            delay(2600L)
            if (_statusBannerMessage.value == message) {
                _statusBannerMessage.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        speechEngine.release()
    }

    private data class StageAudioBundle(
        val avatarState: AvatarState,
        val lipSync: LipSyncFrame,
        val micAmp: Float,
        val partial: String,
        val voiceErr: String?
    )

    private data class StageControlsBundle(
        val expanded: Boolean,
        val collapsed: Boolean,
        val textVisible: Boolean,
        val draft: String,
        val seconds: Int
    )

    private data class HelperBundle(
        val suggestions: List<InspireSuggestion>,
        val inspireVisible: Boolean,
        val hiddenHelpers: Set<Long>,
        val speakingId: Long?,
        val banner: String?
    )

    private data class NavContextBundle(
        val tab: AppTab,
        val mode: TutorMode,
        val topic: String,
        val scenario: RolePlayScenario?,
        val thinking: Boolean
    )

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val learningRepository: LearningRepository,
        private val aiTutorRepository: AiTutorRepository,
        private val speechEngine: SpeechLipSyncEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LingoPulseViewModel(
                settingsRepository = settingsRepository,
                learningRepository = learningRepository,
                aiTutorRepository = aiTutorRepository,
                speechEngine = speechEngine
            ) as T
        }
    }
}
