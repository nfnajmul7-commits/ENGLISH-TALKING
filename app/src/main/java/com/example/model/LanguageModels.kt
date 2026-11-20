package com.example.model

import androidx.annotation.DrawableRes
import com.example.R

data class WorldLanguage(
    val code: String,
    val bcp47: String,
    val englishName: String,
    val nativeName: String,
    val flag: String
) {
    val displayLabel: String
        get() = if (nativeName.equals(englishName, ignoreCase = true)) {
            "$flag $englishName"
        } else {
            "$flag $nativeName ($englishName)"
        }
}

enum class LanguageLevel(val label: String, val cefr: String, val shortDesc: String) {
    BEGINNER("Beginner", "A1", "Core greetings, everyday phrases & slow pacing"),
    ELEMENTARY("Elementary", "A2", "Simple daily conversations & common grammar"),
    INTERMEDIATE("Intermediate", "B1", "Comfortable opinions, travel & workplace topics"),
    UPPER_INTERMEDIATE("Upper-Intermediate", "B2", "Nuanced discussions, idioms & natural speed"),
    ADVANCED("Advanced", "C1", "Fluent nuance, persuasion & native expressions");

    companion object {
        fun fromName(value: String?): LanguageLevel =
            entries.firstOrNull { it.name == value || it.label == value } ?: BEGINNER
    }
}

enum class TutorMode {
    LECTURE,
    PRACTICE
}

enum class AvatarState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

enum class AppTab {
    HOME,
    ROLE_PLAY,
    PROGRESS,
    PROFILE
}

data class UserSettings(
    val userName: String = "Alex Rahman",
    val userEmail: String = "alex.rahman@lingopulse.app",
    val avatarUri: String? = null,
    val avatarAccentIndex: Int = 0,
    val studyInNativeLanguage: Boolean = true,
    val targetLanguageCode: String = "en",
    val nativeLanguageCode: String = "bn",
    val level: LanguageLevel = LanguageLevel.INTERMEDIATE,
    val interests: Set<String> = setOf("Cooking", "Travel", "Tech", "Daily Life"),
    val dailyGoalMinutes: Int = 15,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val speechRate: Float = 1.0f,
    val autoPlayAudio: Boolean = true
) {
    val targetLanguage: WorldLanguage
        get() = WorldLanguagesCatalog.byCode(targetLanguageCode)

    val nativeLanguage: WorldLanguage
        get() = WorldLanguagesCatalog.byCode(nativeLanguageCode)
}

data class VocabPhrase(
    val targetText: String,
    val pronunciation: String,
    val nativeMeaning: String
)

data class TutorResponse(
    val spokenText: String,
    val nativeExplanation: String,
    val phrases: List<VocabPhrase>,
    val practicePrompt: String,
    val grammarFeedback: String? = null,
    val isSampleResponse: Boolean = true
)

data class InspireSuggestion(
    val targetReply: String,
    val nativeHint: String
)

data class ChatMessageUi(
    val id: Long,
    val sessionKey: String,
    val isUser: Boolean,
    val spokenText: String,
    val nativeExplanation: String,
    val phrases: List<VocabPhrase>,
    val practicePrompt: String,
    val grammarFeedback: String?,
    val timestamp: Long,
    val isSampleResponse: Boolean = false
)

data class RolePlayScenario(
    val id: String,
    val title: String,
    val category: String,
    val matchingInterests: List<String>,
    val difficulty: LanguageLevel,
    val aiRoleTitle: String,
    val mission: String,
    @DrawableRes val imageRes: Int,
    val durationMinutes: Int
)

object WorldLanguagesCatalog {
    val all: List<WorldLanguage> = listOf(
        WorldLanguage("en", "en-US", "English", "English", "🇺🇸"),
        WorldLanguage("bn", "bn-BD", "Bengali", "বাংলা", "🇧🇩"),
        WorldLanguage("es", "es-ES", "Spanish", "Español", "🇪🇸"),
        WorldLanguage("fr", "fr-FR", "French", "Français", "🇫🇷"),
        WorldLanguage("de", "de-DE", "German", "Deutsch", "🇩🇪"),
        WorldLanguage("ja", "ja-JP", "Japanese", "日本語", "🇯🇵"),
        WorldLanguage("ko", "ko-KR", "Korean", "한국어", "🇰🇷"),
        WorldLanguage("zh", "zh-CN", "Chinese (Mandarin)", "中文", "🇨🇳"),
        WorldLanguage("ar", "ar-SA", "Arabic", "العربية", "🇸🇦"),
        WorldLanguage("hi", "hi-IN", "Hindi", "हिन्दी", "🇮🇳"),
        WorldLanguage("ur", "ur-PK", "Urdu", "اردو", "🇵🇰"),
        WorldLanguage("pt", "pt-BR", "Portuguese", "Português", "🇧🇷"),
        WorldLanguage("it", "it-IT", "Italian", "Italiano", "🇮🇹"),
        WorldLanguage("ru", "ru-RU", "Russian", "Русский", "🇷🇺"),
        WorldLanguage("tr", "tr-TR", "Turkish", "Türkçe", "🇹🇷"),
        WorldLanguage("vi", "vi-VN", "Vietnamese", "Tiếng Việt", "🇻🇳"),
        WorldLanguage("th", "th-TH", "Thai", "ไทย", "🇹🇭"),
        WorldLanguage("id", "id-ID", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
        WorldLanguage("ms", "ms-MY", "Malay", "Bahasa Melayu", "🇲🇾"),
        WorldLanguage("nl", "nl-NL", "Dutch", "Nederlands", "🇳🇱"),
        WorldLanguage("pl", "pl-PL", "Polish", "Polski", "🇵🇱"),
        WorldLanguage("sv", "sv-SE", "Swedish", "Svenska", "🇸🇪"),
        WorldLanguage("no", "nb-NO", "Norwegian", "Norsk", "🇳🇴"),
        WorldLanguage("el", "el-GR", "Greek", "Ελληνικά", "🇬🇷"),
        WorldLanguage("he", "he-IL", "Hebrew", "עברית", "🇮🇱"),
        WorldLanguage("fa", "fa-IR", "Persian", "فارسی", "🇮🇷"),
        WorldLanguage("ta", "ta-IN", "Tamil", "தமிழ்", "🇮🇳"),
        WorldLanguage("te", "te-IN", "Telugu", "తెలుగు", "🇮🇳"),
        WorldLanguage("sw", "sw-KE", "Swahili", "Kiswahili", "🇰🇪"),
        WorldLanguage("tl", "fil-PH", "Filipino", "Tagalog", "🇵🇭"),
        WorldLanguage("uk", "uk-UA", "Ukrainian", "Українська", "🇺🇦"),
        WorldLanguage("cs", "cs-CZ", "Czech", "Čeština", "🇨🇿"),
        WorldLanguage("ro", "ro-RO", "Romanian", "Română", "🇷🇴")
    )

    fun byCode(code: String): WorldLanguage =
        all.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: all.first()

    val interestsCatalog: List<Pair<String, String>> = listOf(
        "Cooking" to "🍳",
        "Travel" to "✈️",
        "Business" to "💼",
        "Sports" to "⚽",
        "Movies" to "🎬",
        "Tech" to "💻",
        "Daily Life" to "☕",
        "Music" to "🎵",
        "Shopping" to "🛍️",
        "Health" to "🏃"
    )

    val dailyGoalOptions: List<Int> = listOf(5, 10, 15, 30)

    val rolePlayScenarios: List<RolePlayScenario> = listOf(
        RolePlayScenario(
            id = "cafe_cooking",
            title = "Artisan Cafe & Cooking Chat",
            category = "Food & Dining",
            matchingInterests = listOf("Cooking", "Daily Life"),
            difficulty = LanguageLevel.BEGINNER,
            aiRoleTitle = "Friendly Barista & Chef",
            mission = "Order a custom coffee, ask about fresh ingredients, and describe a dish you love to cook.",
            imageRes = R.drawable.img_roleplay_cafe_1795143565304,
            durationMinutes = 5
        ),
        RolePlayScenario(
            id = "airport_travel",
            title = "International Airport Gate",
            category = "Travel & Transit",
            matchingInterests = listOf("Travel", "Daily Life"),
            difficulty = LanguageLevel.ELEMENTARY,
            aiRoleTitle = "Gate & Check-in Agent",
            mission = "Check in your luggage, request a window seat, and confirm your boarding gate and transfer time.",
            imageRes = R.drawable.img_roleplay_airport_1795143566474,
            durationMinutes = 7
        ),
        RolePlayScenario(
            id = "job_interview",
            title = "Tech & Career Interview",
            category = "Career & Business",
            matchingInterests = listOf("Business", "Tech"),
            difficulty = LanguageLevel.INTERMEDIATE,
            aiRoleTitle = "Hiring Manager",
            mission = "Introduce your strengths, walk through a recent project, and ask a sharp question about the team.",
            imageRes = R.drawable.img_roleplay_interview_1795143566508,
            durationMinutes = 10
        ),
        RolePlayScenario(
            id = "boutique_shopping",
            title = "Weekend Market & Boutique",
            category = "Shopping & Lifestyle",
            matchingInterests = listOf("Shopping", "Daily Life", "Movies"),
            difficulty = LanguageLevel.BEGINNER,
            aiRoleTitle = "Boutique Shopkeeper",
            mission = "Ask for a different size or color, compare quality, and politely negotiate a bundle price.",
            imageRes = R.drawable.img_roleplay_shopping_1795143603489,
            durationMinutes = 6
        )
    )
}
