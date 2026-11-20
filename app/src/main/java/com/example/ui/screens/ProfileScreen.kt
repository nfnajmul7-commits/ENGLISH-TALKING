package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.LanguageLevel
import com.example.model.UserSettings
import com.example.model.WorldLanguage
import com.example.model.WorldLanguagesCatalog
import com.example.ui.theme.CoralTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.WarmGold
import java.util.Locale

private val AvatarGradients = listOf(
    listOf(Color(0xFF4F5BFF), Color(0xFF2DD4BF)),
    listOf(Color(0xFFFF6B6B), Color(0xFFF59E0B)),
    listOf(Color(0xFF0EA5A0), Color(0xFF3B82F6)),
    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
    listOf(Color(0xFF10B981), Color(0xFF06B6D4))
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    settings: UserSettings,
    isPlaceholderAi: Boolean,
    onUpdateProfile: (String, String) -> Unit,
    onUpdateAvatarUri: (String?) -> Unit,
    onUpdateAvatarAccent: (Int) -> Unit,
    onSetStudyInNative: (Boolean) -> Unit,
    onSetTargetLanguage: (String) -> Unit,
    onSetNativeLanguage: (String) -> Unit,
    onSetLevel: (LanguageLevel) -> Unit,
    onToggleInterest: (String) -> Unit,
    onSetDailyGoal: (Int) -> Unit,
    onSetReminderEnabled: (Boolean) -> Unit,
    onSetReminderTime: (Int, Int) -> Unit,
    onSetAutoPlayAudio: (Boolean) -> Unit,
    onCycleSpeechRate: () -> Unit,
    onResetProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAvatarSheet by remember { mutableStateOf(false) }
    var showAppSettingsSheet by remember { mutableStateOf(false) }
    var languagePickerTarget by remember { mutableStateOf<String?>(null) } // "target" or "native"
    var showLevelSheet by remember { mutableStateOf(false) }
    var showInterestsSheet by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker (Play Policy compliant!)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateAvatarUri(uri.toString())
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "profile_header") {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 1. User Profile Card (avatar + edit badge, editable name & email, settings gear)
        item(key = "user_profile_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with Edit Badge
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clickable { showAvatarSheet = true }
                            .testTag("profile_avatar_button")
                    ) {
                        val grad = AvatarGradients[settings.avatarAccentIndex.mod(AvatarGradients.size)]
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(grad)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!settings.avatarUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = settings.avatarUri,
                                    contentDescription = "Profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val initials = settings.userName
                                    .split(" ")
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .take(2)
                                    .joinToString("")
                                    .ifEmpty { "LP" }
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                            }
                        }
                        // Edit Badge
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .size(26.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = stringResource(R.string.cd_edit_avatar),
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    // Editable Name & Email
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showEditProfileDialog = true }
                            .padding(vertical = 4.dp)
                            .testTag("edit_profile_identity_row")
                    ) {
                        Text(
                            text = settings.userName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = settings.userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Settings Gear
                    IconButton(
                        onClick = { showAppSettingsSheet = true },
                        modifier = Modifier.testTag("app_settings_gear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.cd_app_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Study in Native Language Toggle Card
        item(key = "study_native_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconMedallion(icon = Icons.Rounded.Translate, tint = TealSecondary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setting_study_native),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.setting_study_native_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = settings.studyInNativeLanguage,
                        onCheckedChange = onSetStudyInNative,
                        modifier = Modifier.testTag("study_in_native_switch")
                    )
                }
            }
        }

        // 3. Language & Personalization Settings Card
        item(key = "learning_settings_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRowItem(
                        icon = Icons.Rounded.Language,
                        iconTint = IndigoPrimary,
                        title = stringResource(R.string.setting_target_lang),
                        value = settings.targetLanguage.displayLabel,
                        onClick = { languagePickerTarget = "target" },
                        tag = "target_language_row"
                    )
                    SettingsRowDivider()
                    SettingsRowItem(
                        icon = Icons.Rounded.SignalCellularAlt,
                        iconTint = EmeraldSuccess,
                        title = stringResource(R.string.setting_level),
                        value = "${settings.level.label} (${settings.level.cefr})",
                        onClick = { showLevelSheet = true },
                        tag = "language_level_row"
                    )
                    SettingsRowDivider()
                    SettingsRowItem(
                        icon = Icons.Rounded.Flag,
                        iconTint = CoralTertiary,
                        title = stringResource(R.string.setting_native_lang),
                        value = settings.nativeLanguage.displayLabel,
                        onClick = { languagePickerTarget = "native" },
                        tag = "native_language_row"
                    )
                    SettingsRowDivider()
                    SettingsRowItem(
                        icon = Icons.Rounded.FavoriteBorder,
                        iconTint = Color(0xFFEC4899),
                        title = stringResource(R.string.setting_interests),
                        value = settings.interests.take(3).joinToString(", ") +
                            if (settings.interests.size > 3) " +${settings.interests.size - 3}" else "",
                        onClick = { showInterestsSheet = true },
                        tag = "interests_row"
                    )
                    SettingsRowDivider()
                    SettingsRowItem(
                        icon = Icons.Rounded.AccessTime,
                        iconTint = WarmGold,
                        title = stringResource(R.string.setting_daily_goal),
                        value = "${settings.dailyGoalMinutes} min/day",
                        onClick = { showGoalDialog = true },
                        tag = "daily_goal_row"
                    )
                    SettingsRowDivider()

                    // Daily Reminder row with toggle + time pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showReminderTimeDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("daily_reminder_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconMedallion(icon = Icons.Rounded.NotificationsActive, tint = Color(0xFF8B5CF6))
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_daily_reminder),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (settings.reminderEnabled) {
                                    "Daily at ${formatReminderTime(settings.reminderHour, settings.reminderMinute)} · Tap to edit"
                                } else {
                                    "Reminders off"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = onSetReminderEnabled,
                            modifier = Modifier.testTag("daily_reminder_switch")
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(14.dp)) }
    }

    // --- Dialogs & Bottom Sheets ---

    // Edit Name & Email Dialog
    if (showEditProfileDialog) {
        var draftName by remember(settings.userName) { mutableStateOf(settings.userName) }
        var draftEmail by remember(settings.userEmail) { mutableStateOf(settings.userEmail) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text(stringResource(R.string.edit_profile_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftEmail,
                        onValueChange = { draftEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateProfile(draftName, draftEmail)
                    showEditProfileDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Avatar Photo Picker / Accent Color Sheet
    if (showAvatarSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Customize Avatar", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        showAvatarSheet = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose Photo from Gallery")
                }
                if (!settings.avatarUri.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onUpdateAvatarUri(null)
                            showAvatarSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Use Initials Monogram")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Monogram Accent", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvatarGradients.forEachIndexed { idx, colors ->
                        val selected = idx == settings.avatarAccentIndex && settings.avatarUri.isNullOrBlank()
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(colors))
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdateAvatarUri(null)
                                    onUpdateAvatarAccent(idx)
                                    showAvatarSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Searchable World Language Picker Bottom Sheet (Target or Native)
    languagePickerTarget?.let { kind ->
        val selectedCode = if (kind == "target") settings.targetLanguageCode else settings.nativeLanguageCode
        SearchableLanguageSheet(
            title = if (kind == "target") stringResource(R.string.setting_target_lang) else stringResource(R.string.setting_native_lang),
            selectedCode = selectedCode,
            onSelect = { lang ->
                if (kind == "target") onSetTargetLanguage(lang.code) else onSetNativeLanguage(lang.code)
                languagePickerTarget = null
            },
            onDismiss = { languagePickerTarget = null }
        )
    }

    // Language Level Picker Bottom Sheet
    if (showLevelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLevelSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.setting_level), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                LanguageLevel.entries.forEach { lvl ->
                    val isSelected = lvl == settings.level
                    Surface(
                        onClick = {
                            onSetLevel(lvl)
                            showLevelSheet = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = lvl.cefr,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(lvl.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    lvl.shortDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }
    }

    // Multi-Select Interests Sheet
    if (showInterestsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInterestsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(stringResource(R.string.setting_interests), style = MaterialTheme.typography.titleLarge)
                Text(
                    "Aria tailors lessons and Role-Play scenarios to these topics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorldLanguagesCatalog.interestsCatalog.forEach { (interest, emoji) ->
                        val selected = settings.interests.contains(interest)
                        FilterChip(
                            selected = selected,
                            onClick = { onToggleInterest(interest) },
                            label = { Text("$emoji $interest") }
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { showInterestsSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Done") }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // Daily Goal Picker Dialog
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text(stringResource(R.string.setting_daily_goal)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorldLanguagesCatalog.dailyGoalOptions.forEach { mins ->
                        val selected = settings.dailyGoalMinutes == mins
                        Surface(
                            onClick = {
                                onSetDailyGoal(mins)
                                showGoalDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$mins min/day",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Close") }
            }
        )
    }

    // Daily Reminder Time Picker Dialog
    if (showReminderTimeDialog) {
        val timeState = rememberTimePickerState(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showReminderTimeDialog = false },
            title = { Text("Daily Reminder Time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                Button(onClick = {
                    onSetReminderTime(timeState.hour, timeState.minute)
                    onSetReminderEnabled(true)
                    showReminderTimeDialog = false
                }) { Text("Set Time") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // App & Tutor Audio Settings Sheet
    if (showAppSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAppSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_sheet_title), style = MaterialTheme.typography.titleLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Play Tutor Voice", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Automatically speak and lip-sync new tutor replies",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.autoPlayAudio,
                        onCheckedChange = onSetAutoPlayAudio
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tutor Speaking Speed", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap to cycle between 0.8x, 1x, and 1.2x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = onCycleSpeechRate, shape = RoundedCornerShape(50)) {
                        val label = when {
                            settings.speechRate < 0.9f -> "0.8x"
                            settings.speechRate > 1.1f -> "1.2x"
                            else -> "1.0x"
                        }
                        Text(label)
                    }
                }

                if (isPlaceholderAi) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI Tutor Status: Running in local Sample Tutor Mode. Live Gemini AI responses in Android apps require enabling Firebase AI Logic.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        onResetProgress()
                        showAppSettingsSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = CoralTertiary)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset Local Progress & Chat History", color = CoralTertiary)
                }
                Spacer(Modifier.height(22.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchableLanguageSheet(
    title: String,
    selectedCode: String,
    onSelect: (WorldLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) WorldLanguagesCatalog.all
        else WorldLanguagesCatalog.all.filter {
            it.englishName.contains(q, ignoreCase = true) ||
                it.nativeName.contains(q, ignoreCase = true) ||
                it.code.equals(q, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_language_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("language_search_input")
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.code }) { lang ->
                    val isSelected = lang.code == selectedCode
                    Surface(
                        onClick = { onSelect(lang) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lang_option_${lang.code}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.flag,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = lang.englishName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsIconMedallion(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconMedallion(icon = icon, tint = iconTint)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

private fun formatReminderTime(hour24: Int, minute: Int): String {
    val amPm = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
}
