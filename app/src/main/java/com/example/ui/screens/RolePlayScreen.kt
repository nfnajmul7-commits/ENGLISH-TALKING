package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ChatMessageUi
import com.example.model.RolePlayScenario
import com.example.model.TutorMode
import com.example.model.VocabPhrase
import com.example.model.WorldLanguagesCatalog
import com.example.ui.components.ChatWorkspace
import com.example.ui.components.TalkingAvatarStage
import com.example.viewmodel.LingoPulseUiState

@Composable
fun RolePlayScreen(
    uiState: LingoPulseUiState,
    onOpenScenario: (RolePlayScenario) -> Unit,
    onExitScenario: () -> Unit,
    onModeChange: (TutorMode) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleStageExpanded: () -> Unit,
    onToggleStageCollapsed: () -> Unit,
    onToggleVoice: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    val activeScenario = uiState.activeScenario
    val settings = uiState.settings

    if (activeScenario == null) {
        // Scenario Gallery sorted by user interests
        val sortedScenarios = WorldLanguagesCatalog.rolePlayScenarios.sortedByDescending { scenario ->
            scenario.matchingInterests.count { settings.interests.contains(it) }
        }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "roleplay_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.roleplay_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${settings.targetLanguage.flag} ${settings.targetLanguage.englishName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.roleplay_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(sortedScenarios, key = { it.id }) { scenario ->
                val matchesInterests = scenario.matchingInterests.any { settings.interests.contains(it) }
                RolePlayScenarioCard(
                    scenario = scenario,
                    matchesInterests = matchesInterests,
                    onClick = { onOpenScenario(scenario) }
                )
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
        return
    }

    // Active Role-Play Stage + Chat Workspace
    BackHandler {
        if (uiState.isStageExpanded) onToggleStageExpanded()
        else onExitScenario()
    }

    val languageBadge = "${settings.nativeLanguage.flag} → ${settings.targetLanguage.flag} ${settings.level.cefr}"
    val liveCaption = uiState.partialTranscript.takeIf { it.isNotBlank() }
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
        // Active Scenario Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExitScenario,
                modifier = Modifier.testTag("exit_roleplay_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeScenario.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Aria as ${activeScenario.aiRoleTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Surface(
                onClick = onLogLessonCompleted,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .testTag("roleplay_log_complete_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
        }

        // Talking AI Avatar Stage
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

        // Scenario Mission Strip
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ) {
            Text(
                text = "Mission: ${activeScenario.mission}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Role-Play Conversation Workspace
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
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RolePlayScenarioCard(
    scenario: RolePlayScenario,
    matchesInterests: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("roleplay_card_${scenario.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                Image(
                    painter = painterResource(id = scenario.imageRes),
                    contentDescription = scenario.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.62f))
                            )
                        )
                )

                // Top-left match badge
                if (matchesInterests) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.roleplay_for_you),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                // Top-right CEFR level pill
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${scenario.difficulty.label} · ${scenario.difficulty.cefr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }

                // Bottom-left title overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = scenario.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = scenario.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = scenario.mission,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${scenario.durationMinutes} min · ${scenario.aiRoleTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.roleplay_start),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
