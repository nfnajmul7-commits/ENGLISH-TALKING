package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.AvatarState
import com.example.model.ChatMessageUi
import com.example.model.InspireSuggestion
import com.example.model.VocabPhrase
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.StageCoralPulse

@Composable
fun ChatWorkspace(
    messages: List<ChatMessageUi>,
    savedVocabTexts: Set<String>,
    avatarState: AvatarState,
    speakingMessageId: Long?,
    isThinking: Boolean,
    partialTranscript: String,
    voiceError: String?,
    isTextInputVisible: Boolean,
    textDraft: String,
    isInspireVisible: Boolean,
    inspireSuggestions: List<InspireSuggestion>,
    hiddenHelperMessageIds: Set<Long>,
    showNativeByDefault: Boolean,
    isPlaceholderAi: Boolean,
    onPlayMessageAudio: (ChatMessageUi) -> Unit,
    onSpeakPhrase: (String) -> Unit,
    onSpeakPhraseWithMeaning: (VocabPhrase) -> Unit,
    onSaveVocab: (VocabPhrase) -> Unit,
    onToggleHelper: (Long) -> Unit,
    onToggleVoice: () -> Unit,
    onClearVoiceError: () -> Unit,
    onToggleTextInput: () -> Unit,
    onTextDraftChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onToggleInspire: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var permissionDeniedNotice by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNotice = false
            onToggleVoice()
        } else {
            permissionDeniedNotice = true
        }
    }

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Transparent notice when Firebase AI Logic is not provisioned
        if (isPlaceholderAi) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.sample_ai_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Messages Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                if (msg.isUser) {
                    UserMessageBubble(text = msg.spokenText)
                } else {
                    val isHelperVisible = if (showNativeByDefault) {
                        !hiddenHelperMessageIds.contains(msg.id)
                    } else {
                        hiddenHelperMessageIds.contains(msg.id)
                    }
                    TutorMessageCard(
                        message = msg,
                        isPlayingThis = avatarState == AvatarState.SPEAKING && speakingMessageId == msg.id,
                        isHelperVisible = isHelperVisible,
                        savedVocabTexts = savedVocabTexts,
                        onPlayAudio = { onPlayMessageAudio(msg) },
                        onToggleHelper = { onToggleHelper(msg.id) },
                        onSpeakPhrase = onSpeakPhraseWithMeaning,
                        onSaveVocab = onSaveVocab
                    )
                }
            }

            if (isThinking) {
                item(key = "thinking_indicator") {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Aria is preparing your lesson response…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Live Listening Transcript Banner
        AnimatedVisibility(
            visible = avatarState == AvatarState.LISTENING,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                color = StageCoralPulse.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StageCoralPulse.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = StageCoralPulse,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = partialTranscript.ifBlank { "Listening… speak naturally now" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Voice / Permission Notice Banner
        val activeError = if (permissionDeniedNotice) {
            stringResource(R.string.mic_permission_needed)
        } else voiceError

        AnimatedVisibility(visible = !activeError.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            permissionDeniedNotice = false
                            onClearVoiceError()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Inspire / Hint Suggestions Strip
        AnimatedVisibility(
            visible = isInspireVisible && inspireSuggestions.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.inspire_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onToggleInspire, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close hints", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    inspireSuggestions.forEachIndexed { idx, suggestion ->
                        Surface(
                            onClick = { onSendMessage(suggestion.targetReply) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .testTag("inspire_suggestion_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = suggestion.targetReply,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = suggestion.nativeHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onSpeakPhrase(suggestion.targetReply) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = "Preview pronunciation",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expandable Text Input Bar ("Type" mode)
        AnimatedVisibility(
            visible = isTextInputVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textDraft,
                    onValueChange = onTextDraftChange,
                    placeholder = { Text(stringResource(R.string.placeholder_message)) },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendMessage(textDraft) }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_text_input")
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = { onSendMessage(textDraft) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_message_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = stringResource(R.string.action_send),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Primary Action Dock: Type | Speak (Mic) | Inspire
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type toggle button
                DockSecondaryButton(
                    icon = Icons.Rounded.Keyboard,
                    label = stringResource(R.string.action_type),
                    active = isTextInputVisible,
                    onClick = onToggleTextInput,
                    tag = "type_toggle_button"
                )

                // Prominent Speak / Stop Voice Button
                val isListening = avatarState == AvatarState.LISTENING
                Surface(
                    onClick = {
                        if (isListening) {
                            onToggleVoice()
                        } else {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                permissionDeniedNotice = false
                                onToggleVoice()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    color = if (isListening) StageCoralPulse else MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("speak_mic_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 26.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(if (isListening) R.string.action_stop else R.string.action_speak),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }

                // Inspire / Hint button
                DockSecondaryButton(
                    icon = Icons.Rounded.AutoAwesome,
                    label = stringResource(R.string.action_inspire),
                    active = isInspireVisible,
                    onClick = onToggleInspire,
                    tag = "inspire_button"
                )
            }
        }
    }
}

@Composable
private fun DockSecondaryButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .height(48.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(0.84f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun TutorMessageCard(
    message: ChatMessageUi,
    isPlayingThis: Boolean,
    isHelperVisible: Boolean,
    savedVocabTexts: Set<String>,
    onPlayAudio: () -> Unit,
    onToggleHelper: () -> Unit,
    onSpeakPhrase: (VocabPhrase) -> Unit,
    onSaveVocab: (VocabPhrase) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Gentle Grammar/Pronunciation Feedback Pill (when replying to user)
        if (!message.grammarFeedback.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = EmeraldSuccess.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(16.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = message.grammarFeedback,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 1. Warm Native/Target Opener Bubble + Side Translate & Play Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (isPlayingThis) 1.5.dp else 1.dp,
                    color = if (isPlayingThis) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    val primaryHeader = if (isHelperVisible && message.nativeExplanation.isNotBlank()) {
                        message.nativeExplanation
                    } else {
                        message.spokenText
                    }
                    Text(
                        text = primaryHeader,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Bulleted Target–Native Phrase List (clean Learna-inspired format: "• Hello – হ্যালো বলার জন্য।")
                    if (message.phrases.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            message.phrases.forEach { phrase ->
                                val isSaved = savedVocabTexts.contains(phrase.targetText)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSpeakPhrase(phrase) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            val formattedLine = androidx.compose.ui.text.buildAnnotatedString {
                                                pushStyle(
                                                    androidx.compose.ui.text.SpanStyle(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                                append("• ${phrase.targetText}")
                                                pop()
                                                if (isHelperVisible && phrase.nativeMeaning.isNotBlank()) {
                                                    pushStyle(
                                                        androidx.compose.ui.text.SpanStyle(
                                                            fontWeight = FontWeight.Normal,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                    append(" – ${phrase.nativeMeaning}")
                                                    pop()
                                                }
                                            }
                                            Text(
                                                text = formattedLine,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            if (isHelperVisible && phrase.pronunciation.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = phrase.pronunciation,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onSpeakPhrase(phrase) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                                contentDescription = stringResource(R.string.action_play_audio),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onSaveVocab(phrase) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                                contentDescription = stringResource(
                                                    if (isSaved) R.string.cd_saved_vocab else R.string.cd_save_vocab
                                                ),
                                                tint = if (isSaved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            // Side Circular Action Buttons (Translate Helper & Play Audio — matching screenshot!)
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    onClick = onPlayAudio,
                    shape = CircleShape,
                    color = if (isPlayingThis) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("play_audio_msg_${message.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlayingThis) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = stringResource(
                                if (isPlayingThis) R.string.action_stop_audio else R.string.action_play_audio
                            ),
                            tint = if (isPlayingThis) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Surface(
                    onClick = onToggleHelper,
                    shape = CircleShape,
                    color = if (isHelperVisible) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("toggle_helper_msg_${message.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = stringResource(
                                if (isHelperVisible) R.string.action_hide_helper else R.string.action_show_helper
                            ),
                            tint = if (isHelperVisible) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Clean Conversational Follow-up Bubble (e.g., "এখানে কিছু জানতে চান? কিছু জিজ্ঞেস করতে চান?")
        if (message.practicePrompt.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(0.86f)
            ) {
                Text(
                    text = message.practicePrompt,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}
