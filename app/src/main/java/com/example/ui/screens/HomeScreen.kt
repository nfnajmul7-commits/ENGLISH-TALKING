package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ChatMessageUi
import com.example.model.LearningModule
import com.example.model.TutorMode
import com.example.model.VocabPhrase
import com.example.ui.components.ChatWorkspace
import com.example.ui.components.TalkingAvatarStage
import com.example.viewmodel.LingoPulseUiState

@Composable
fun HomeScreen(
    uiState: LingoPulseUiState,
    onSelectTopic: (String) -> Unit,
    onSelectModule: (LearningModule) -> Unit,
    onSelectLevel: (com.example.model.LanguageLevel) -> Unit,
    onModeChange: (TutorMode) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleStageExpanded: () -> Unit,
    onToggleStageCollapsed: () -> Unit,
    onToggleVoice: () -> Unit,
    onToggleMicLanguage: () -> Unit,
    onClearVoiceError: () -> Unit,
    onStopAudio: () -> Unit,
    onPlayMessageAudio: (ChatMessageUi) -> Unit,
    onSpeakPhrase: (String) -> Unit,
    onSpeakPhraseWithMeaning: (VocabPhrase) -> Unit,
    onSaveVocab: (VocabPhrase) -> Unit,
    onToggleHelper: (Long) -> Unit,
    onToggleTextInput: () -> Unit,
    onTextDraftChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onToggleInspire: () -> Unit,
    onLogLessonCompleted: () -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = uiState.isStageExpanded) {
        onToggleStageExpanded()
    }

    val settings = uiState.settings
    val isBn = settings.nativeLanguageCode == "bn"
    val languageBadge = "${settings.nativeLanguage.flag} → ${settings.targetLanguage.flag} ${settings.level.cefr}"
    val liveCaption = uiState.partialTranscript.takeIf { it.isNotBlank() }
        ?: uiState.messages.lastOrNull { !it.isUser }?.nativeExplanation?.takeIf { it.isNotBlank() }
        ?: uiState.messages.lastOrNull { !it.isUser }?.spokenText

    if (uiState.isStageExpanded) {
        TalkingAvatarStage(
            avatarState = uiState.avatarState,
            lipSyncFrame = uiState.lipSyncFrame,
            micAmplitude = uiState.micAmplitude,
            tutorMode = uiState.tutorMode,
            speechRate = settings.speechRate,
            sessionSeconds = uiState.sessionSeconds,
            languageBadge = languageBadge,
            isExpanded = true,
            isCollapsed = false,
            liveCaption = liveCaption,
            onModeChange = onModeChange,
            onCycleSpeed = onCycleSpeed,
            onToggleExpand = onToggleStageExpanded,
            onToggleCollapse = onToggleStageCollapsed,
            onToggleSpeak = onToggleVoice,
            onStopAudio = onStopAudio,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Sticky / Collapsible Talking AI Avatar Stage
        TalkingAvatarStage(
            avatarState = uiState.avatarState,
            lipSyncFrame = uiState.lipSyncFrame,
            micAmplitude = uiState.micAmplitude,
            tutorMode = uiState.tutorMode,
            speechRate = settings.speechRate,
            sessionSeconds = uiState.sessionSeconds,
            languageBadge = languageBadge,
            isExpanded = false,
            isCollapsed = uiState.isStageCollapsed,
            liveCaption = liveCaption,
            onModeChange = onModeChange,
            onCycleSpeed = onCycleSpeed,
            onToggleExpand = onToggleStageExpanded,
            onToggleCollapse = onToggleStageCollapsed,
            onToggleSpeak = onToggleVoice,
            onStopAudio = onStopAudio
        )

        // Progressive Level Stepper (Beginner A1 -> Advanced C1) + Log Lesson & Reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 6.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 6.dp)
            ) {
                items(com.example.model.LanguageLevel.entries) { lvl ->
                    val label = if (isBn) "${lvl.cefr} ${lvl.bnLabel}" else "${lvl.cefr} ${lvl.label}"
                    FilterChip(
                        selected = settings.level == lvl,
                        onClick = { onSelectLevel(lvl) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("level_chip_${lvl.cefr.lowercase()}")
                    )
                }
            }

            Surface(
                onClick = onLogLessonCompleted,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.testTag("log_lesson_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_complete_lesson),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            IconButton(
                onClick = onClearChat,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("refresh_lesson_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "New lesson chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 8 Interactive Learning Modules Strip (Conversation, Translate, Grammar, Vocab, Pronunciation, Quiz, Office, Travel)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(LearningModule.entries, key = { it.id }) { mod ->
                val selected = uiState.activeModule == mod
                val label = if (isBn) "${mod.emoji} ${mod.bnLabel}" else "${mod.emoji} ${mod.label}"
                FilterChip(
                    selected = selected,
                    onClick = { onSelectModule(mod) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.testTag("module_chip_${mod.id}")
                )
            }
        }

        // Lesson Chat Workspace
        ChatWorkspace(
            messages = uiState.messages,
            savedVocabTexts = uiState.savedVocab.map { it.targetText }.toSet(),
            avatarState = uiState.avatarState,
            speakingMessageId = uiState.speakingMessageId,
            isThinking = uiState.isThinking,
            partialTranscript = uiState.partialTranscript,
            voiceError = uiState.voiceError,
            isTextInputVisible = uiState.isTextInputVisible,
            textDraft = uiState.textInputDraft,
            isInspireVisible = uiState.isInspireVisible,
            inspireSuggestions = uiState.inspireSuggestions,
            hiddenHelperMessageIds = uiState.hiddenHelperMessageIds,
            showNativeByDefault = settings.studyInNativeLanguage,
            isPlaceholderAi = uiState.isPlaceholderAi,
            onPlayMessageAudio = onPlayMessageAudio,
            onSpeakPhrase = onSpeakPhrase,
            onSpeakPhraseWithMeaning = onSpeakPhraseWithMeaning,
            onSaveVocab = onSaveVocab,
            onToggleHelper = onToggleHelper,
            onToggleVoice = onToggleVoice,
            onClearVoiceError = onClearVoiceError,
            onToggleTextInput = onToggleTextInput,
            onTextDraftChange = onTextDraftChange,
            onSendMessage = onSendMessage,
            onToggleInspire = onToggleInspire,
            quickReplies = uiState.activeQuickReplies,
            micLanguageFlag = if (uiState.voiceInputUsesNative) settings.nativeLanguage.flag else settings.targetLanguage.flag,
            onToggleMicLanguage = onToggleMicLanguage,
            modifier = Modifier.weight(1f)
        )
    }
}
