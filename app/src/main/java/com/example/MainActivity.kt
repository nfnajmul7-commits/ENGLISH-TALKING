package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.SpeechLipSyncEngine
import com.example.data.AppDatabase
import com.example.data.LearningRepository
import com.example.data.PlaceholderAiTutorRepository
import com.example.data.SettingsRepository
import com.example.model.AppTab
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.RolePlayScreen
import com.example.ui.theme.LingoPulseTheme
import com.example.viewmodel.LingoPulseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContext = applicationContext
        val settingsRepo = SettingsRepository(appContext)
        val learningRepo = LearningRepository(AppDatabase.getInstance(appContext))
        val aiTutorRepo = PlaceholderAiTutorRepository()
        val speechEngine = SpeechLipSyncEngine(appContext)
        val vmFactory = LingoPulseViewModel.Factory(
            settingsRepository = settingsRepo,
            learningRepository = learningRepo,
            aiTutorRepository = aiTutorRepo,
            speechEngine = speechEngine
        )

        setContent {
            LingoPulseTheme {
                val viewModel: LingoPulseViewModel = viewModel(factory = vmFactory)
                LingoPulseApp(viewModel = viewModel)
            }
        }
    }
}

private data class NavDestination(
    val tab: AppTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopLevelDestinations = listOf(
    NavDestination(AppTab.HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    NavDestination(AppTab.ROLE_PLAY, R.string.nav_roleplay, Icons.Rounded.TheaterComedy, Icons.Outlined.TheaterComedy, "nav_roleplay"),
    NavDestination(AppTab.PROGRESS, R.string.nav_progress, Icons.Rounded.BarChart, Icons.Outlined.BarChart, "nav_progress"),
    NavDestination(AppTab.PROFILE, R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person, "nav_profile")
)

@Composable
private fun LingoPulseApp(viewModel: LingoPulseViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useNavRail && !uiState.isStageExpanded) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = uiState.activeTab == dest.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && !uiState.isStageExpanded) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = uiState.activeTab == dest.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (uiState.activeTab) {
                        AppTab.HOME -> HomeScreen(
                            uiState = uiState,
                            onSelectTopic = viewModel::selectTopic,
                            onModeChange = viewModel::setTutorMode,
                            onCycleSpeed = viewModel::cycleSpeechRate,
                            onToggleStageExpanded = viewModel::toggleStageExpanded,
                            onToggleStageCollapsed = viewModel::toggleStageCollapsed,
                            onToggleVoice = viewModel::toggleVoiceListening,
                            onClearVoiceError = viewModel::clearVoiceError,
                            onStopAudio = viewModel::stopAudio,
                            onPlayMessageAudio = viewModel::playMessageAudio,
                            onSpeakPhrase = { text -> viewModel.speakPhrase(text) },
                            onSaveVocab = viewModel::saveVocabPhrase,
                            onToggleHelper = viewModel::toggleMessageHelper,
                            onToggleTextInput = viewModel::toggleTextInput,
                            onTextDraftChange = viewModel::updateTextDraft,
                            onSendMessage = viewModel::sendUserMessage,
                            onToggleInspire = viewModel::toggleInspireHints,
                            onLogLessonCompleted = viewModel::logLessonCompleted,
                            onClearChat = viewModel::clearCurrentChat
                        )

                        AppTab.ROLE_PLAY -> RolePlayScreen(
                            uiState = uiState,
                            onOpenScenario = viewModel::openRolePlayScenario,
                            onExitScenario = viewModel::exitRolePlayScenario,
                            onModeChange = viewModel::setTutorMode,
                            onCycleSpeed = viewModel::cycleSpeechRate,
                            onToggleStageExpanded = viewModel::toggleStageExpanded,
                            onToggleStageCollapsed = viewModel::toggleStageCollapsed,
                            onToggleVoice = viewModel::toggleVoiceListening,
                            onClearVoiceError = viewModel::clearVoiceError,
                            onStopAudio = viewModel::stopAudio,
                            onPlayMessageAudio = viewModel::playMessageAudio,
                            onSpeakPhrase = { text -> viewModel.speakPhrase(text) },
                            onSaveVocab = viewModel::saveVocabPhrase,
                            onToggleHelper = viewModel::toggleMessageHelper,
                            onToggleTextInput = viewModel::toggleTextInput,
                            onTextDraftChange = viewModel::updateTextDraft,
                            onSendMessage = viewModel::sendUserMessage,
                            onToggleInspire = viewModel::toggleInspireHints,
                            onLogLessonCompleted = viewModel::logLessonCompleted
                        )

                        AppTab.PROGRESS -> ProgressScreen(
                            uiState = uiState,
                            onSpeakVocab = { text, bcp -> viewModel.speakPhrase(text, bcp) },
                            onRemoveVocab = viewModel::removeVocab
                        )

                        AppTab.PROFILE -> ProfileScreen(
                            settings = uiState.settings,
                            isPlaceholderAi = uiState.isPlaceholderAi,
                            onUpdateProfile = viewModel::updateProfile,
                            onUpdateAvatarUri = viewModel::updateAvatarUri,
                            onUpdateAvatarAccent = viewModel::updateAvatarAccent,
                            onSetStudyInNative = viewModel::setStudyInNativeLanguage,
                            onSetTargetLanguage = viewModel::setTargetLanguage,
                            onSetNativeLanguage = viewModel::setNativeLanguage,
                            onSetLevel = viewModel::setLevel,
                            onToggleInterest = viewModel::toggleInterest,
                            onSetDailyGoal = viewModel::setDailyGoal,
                            onSetReminderEnabled = viewModel::setReminderEnabled,
                            onSetReminderTime = viewModel::setReminderTime,
                            onSetAutoPlayAudio = viewModel::setAutoPlayAudio,
                            onCycleSpeechRate = viewModel::cycleSpeechRate,
                            onResetProgress = viewModel::resetAllProgress
                        )
                    }

                    // Transient status toast pill
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !uiState.statusBannerMessage.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.inverseSurface,
                            shadowElevation = 6.dp
                        ) {
                            Text(
                                text = uiState.statusBannerMessage.orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
