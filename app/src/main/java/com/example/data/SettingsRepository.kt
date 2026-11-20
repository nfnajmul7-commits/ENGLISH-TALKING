package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.LanguageLevel
import com.example.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lingopulse_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val AVATAR_URI = stringPreferencesKey("avatar_uri")
        val AVATAR_ACCENT = intPreferencesKey("avatar_accent")
        val STUDY_IN_NATIVE = booleanPreferencesKey("study_in_native")
        val TARGET_LANG = stringPreferencesKey("target_lang")
        val NATIVE_LANG = stringPreferencesKey("native_lang")
        val LEVEL = stringPreferencesKey("level")
        val INTERESTS = stringSetPreferencesKey("interests")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            userName = prefs[Keys.USER_NAME] ?: "Alex Rahman",
            userEmail = prefs[Keys.USER_EMAIL] ?: "alex.rahman@lingopulse.app",
            avatarUri = prefs[Keys.AVATAR_URI]?.takeIf { it.isNotBlank() },
            avatarAccentIndex = prefs[Keys.AVATAR_ACCENT] ?: 0,
            studyInNativeLanguage = prefs[Keys.STUDY_IN_NATIVE] ?: true,
            targetLanguageCode = prefs[Keys.TARGET_LANG] ?: "en",
            nativeLanguageCode = prefs[Keys.NATIVE_LANG] ?: "bn",
            level = LanguageLevel.fromName(prefs[Keys.LEVEL] ?: LanguageLevel.INTERMEDIATE.name),
            interests = prefs[Keys.INTERESTS] ?: setOf("Cooking", "Travel", "Tech", "Daily Life"),
            dailyGoalMinutes = prefs[Keys.DAILY_GOAL] ?: 15,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: true,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 9,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            autoPlayAudio = prefs[Keys.AUTO_PLAY] ?: true
        )
    }

    suspend fun updateProfile(name: String, email: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name.trim().ifEmpty { "Learner" }
            prefs[Keys.USER_EMAIL] = email.trim()
        }
    }

    suspend fun updateAvatarUri(uri: String?) {
        context.settingsDataStore.edit { prefs ->
            if (uri.isNullOrBlank()) prefs.remove(Keys.AVATAR_URI)
            else prefs[Keys.AVATAR_URI] = uri
        }
    }

    suspend fun updateAvatarAccent(index: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AVATAR_ACCENT] = index
        }
    }

    suspend fun setStudyInNativeLanguage(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.STUDY_IN_NATIVE] = enabled
        }
    }

    suspend fun setTargetLanguage(code: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.TARGET_LANG] = code
        }
    }

    suspend fun setNativeLanguage(code: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.NATIVE_LANG] = code
        }
    }

    suspend fun setLevel(level: LanguageLevel) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LEVEL] = level.name
        }
    }

    suspend fun toggleInterest(interest: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.INTERESTS] ?: setOf("Cooking", "Travel", "Tech", "Daily Life")
            val updated = if (current.contains(interest)) {
                if (current.size > 1) current - interest else current
            } else {
                current + interest
            }
            prefs[Keys.INTERESTS] = updated
        }
    }

    suspend fun setDailyGoal(minutes: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DAILY_GOAL] = minutes
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.REMINDER_HOUR] = hour
            prefs[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SPEECH_RATE] = rate
        }
    }

    suspend fun setAutoPlayAudio(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AUTO_PLAY] = enabled
        }
    }
}
