package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionKey: String,
    val isUser: Boolean,
    val spokenText: String,
    val nativeExplanation: String,
    val phrasesEncoded: String,
    val practicePrompt: String,
    val grammarFeedback: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isSampleResponse: Boolean = false
)

@Entity(tableName = "vocab_items")
data class VocabItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetText: String,
    val pronunciation: String,
    val nativeMeaning: String,
    val targetLangCode: String,
    val practiceCount: Int = 1,
    val lastPracticedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_progress")
data class DailyProgressEntity(
    @PrimaryKey val dateKey: String, // yyyy-MM-dd
    val minutesStudied: Int = 0,
    val lessonsCompleted: Int = 0
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE sessionKey = :sessionKey ORDER BY timestamp ASC, id ASC")
    fun observeMessages(sessionKey: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE sessionKey = :sessionKey")
    suspend fun clearSession(sessionKey: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Dao
interface VocabDao {
    @Query("SELECT * FROM vocab_items ORDER BY lastPracticedAt DESC")
    fun observeAllVocab(): Flow<List<VocabItemEntity>>

    @Query("SELECT * FROM vocab_items WHERE targetText = :targetText AND targetLangCode = :langCode LIMIT 1")
    suspend fun findByPhrase(targetText: String, langCode: String): VocabItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVocab(item: VocabItemEntity)

    @Query("DELETE FROM vocab_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vocab_items")
    suspend fun clearAll()
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM daily_progress ORDER BY dateKey DESC")
    fun observeAllProgress(): Flow<List<DailyProgressEntity>>

    @Query("SELECT * FROM daily_progress WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getForDate(dateKey: String): DailyProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(entry: DailyProgressEntity)

    @Query("DELETE FROM daily_progress")
    suspend fun clearAll()
}

@Database(
    entities = [ChatMessageEntity::class, VocabItemEntity::class, DailyProgressEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun vocabDao(): VocabDao
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lingopulse_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
