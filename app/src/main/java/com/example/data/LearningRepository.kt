package com.example.data

import com.example.model.ChatMessageUi
import com.example.model.VocabPhrase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyBarEntry(
    val dayLabel: String,
    val dateKey: String,
    val minutes: Int,
    val isToday: Boolean
)

data class ProgressSummary(
    val todayMinutes: Int = 0,
    val totalMinutes: Int = 0,
    val totalLessonsCompleted: Int = 0,
    val streakDays: Int = 0,
    val weeklyBars: List<DailyBarEntry> = emptyList()
)

class LearningRepository(private val database: AppDatabase) {

    private val chatDao = database.chatDao()
    private val vocabDao = database.vocabDao()
    private val progressDao = database.progressDao()

    fun observeChat(sessionKey: String): Flow<List<ChatMessageUi>> =
        chatDao.observeMessages(sessionKey).map { entities ->
            entities.map { entity ->
                ChatMessageUi(
                    id = entity.id,
                    sessionKey = entity.sessionKey,
                    isUser = entity.isUser,
                    spokenText = entity.spokenText,
                    nativeExplanation = entity.nativeExplanation,
                    phrases = decodePhrases(entity.phrasesEncoded),
                    practicePrompt = entity.practicePrompt,
                    grammarFeedback = entity.grammarFeedback,
                    timestamp = entity.timestamp,
                    isSampleResponse = entity.isSampleResponse
                )
            }
        }

    suspend fun saveUserMessage(sessionKey: String, text: String): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                sessionKey = sessionKey,
                isUser = true,
                spokenText = text,
                nativeExplanation = "",
                phrasesEncoded = "",
                practicePrompt = "",
                grammarFeedback = null,
                isSampleResponse = false
            )
        )
    }

    suspend fun saveTutorMessage(
        sessionKey: String,
        spokenText: String,
        nativeExplanation: String,
        phrases: List<VocabPhrase>,
        practicePrompt: String,
        grammarFeedback: String?,
        isSampleResponse: Boolean
    ): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                sessionKey = sessionKey,
                isUser = false,
                spokenText = spokenText,
                nativeExplanation = nativeExplanation,
                phrasesEncoded = encodePhrases(phrases),
                practicePrompt = practicePrompt,
                grammarFeedback = grammarFeedback,
                isSampleResponse = isSampleResponse
            )
        )
    }

    suspend fun clearSession(sessionKey: String) {
        chatDao.clearSession(sessionKey)
    }

    val allVocab: Flow<List<VocabItemEntity>> = vocabDao.observeAllVocab()

    suspend fun saveOrPracticeVocab(phrase: VocabPhrase, targetLangCode: String) {
        val existing = vocabDao.findByPhrase(phrase.targetText, targetLangCode)
        if (existing != null) {
            vocabDao.upsertVocab(
                existing.copy(
                    practiceCount = existing.practiceCount + 1,
                    lastPracticedAt = System.currentTimeMillis()
                )
            )
        } else {
            vocabDao.upsertVocab(
                VocabItemEntity(
                    targetText = phrase.targetText,
                    pronunciation = phrase.pronunciation,
                    nativeMeaning = phrase.nativeMeaning,
                    targetLangCode = targetLangCode,
                    practiceCount = 1,
                    lastPracticedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun removeVocab(id: Long) {
        vocabDao.deleteById(id)
    }

    val progressSummary: Flow<ProgressSummary> = progressDao.observeAllProgress().map { rows ->
        val byDate = rows.associateBy { it.dateKey }
        val todayKey = todayDateKey()
        val todayEntry = byDate[todayKey]
        val totalMinutes = rows.sumOf { it.minutesStudied }
        val totalLessons = rows.sumOf { it.lessonsCompleted }

        // Compute consecutive active streak ending today (or yesterday if not studied yet today)
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var streak = 0
        if ((byDate[todayKey]?.minutesStudied ?: 0) == 0 && (byDate[todayKey]?.lessonsCompleted ?: 0) == 0) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        while (true) {
            val key = dateFormat.format(cal.time)
            val row = byDate[key]
            if (row != null && (row.minutesStudied > 0 || row.lessonsCompleted > 0)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        // Build last 7 days bar entries
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val bars = (6 downTo 0).map { daysAgo ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val key = dateFormat.format(c.time)
            DailyBarEntry(
                dayLabel = dayFormat.format(c.time).take(3),
                dateKey = key,
                minutes = byDate[key]?.minutesStudied ?: 0,
                isToday = daysAgo == 0
            )
        }

        ProgressSummary(
            todayMinutes = todayEntry?.minutesStudied ?: 0,
            totalMinutes = totalMinutes,
            totalLessonsCompleted = totalLessons,
            streakDays = streak,
            weeklyBars = bars
        )
    }

    suspend fun logStudyMinutes(minutesDelta: Int) {
        if (minutesDelta <= 0) return
        val key = todayDateKey()
        val current = progressDao.getForDate(key) ?: DailyProgressEntity(dateKey = key)
        progressDao.upsertProgress(
            current.copy(minutesStudied = current.minutesStudied + minutesDelta)
        )
    }

    suspend fun logCompletedLesson(bonusMinutes: Int = 2) {
        val key = todayDateKey()
        val current = progressDao.getForDate(key) ?: DailyProgressEntity(dateKey = key)
        progressDao.upsertProgress(
            current.copy(
                minutesStudied = current.minutesStudied + bonusMinutes,
                lessonsCompleted = current.lessonsCompleted + 1
            )
        )
    }

    suspend fun resetAllProgress() {
        chatDao.clearAll()
        vocabDao.clearAll()
        progressDao.clearAll()
    }

    private fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun encodePhrases(phrases: List<VocabPhrase>): String =
        phrases.joinToString("\n") { p ->
            listOf(p.targetText, p.pronunciation, p.nativeMeaning)
                .joinToString("|||") { it.replace("\n", " ").replace("|||", " ") }
        }

    private fun decodePhrases(raw: String): List<VocabPhrase> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|||")
            if (parts.size >= 3) {
                VocabPhrase(
                    targetText = parts[0],
                    pronunciation = parts[1],
                    nativeMeaning = parts[2]
                )
            } else null
        }
    }
}
