package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "dreams")
data class Dream(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val rawText: String,
    val surrealImagePath: String? = null,
    val emotionalTheme: String? = null,
    val structuredInterpretation: String? = null,
    val audioPath: String? = null,
    val tags: String = "",
    val artworkStatus: String = "complete",
    val artworkFallbackUsed: Boolean = false
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = Dream::class,
            parentColumns = ["id"],
            childColumns = ["dreamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dreamId")]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dreamId: Long,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DreamDao {
    @Query("SELECT * FROM dreams ORDER BY timestamp DESC")
    fun getAllDreams(): Flow<List<Dream>>

    @Query("SELECT * FROM dreams WHERE id = :id")
    fun getDreamById(id: Long): Flow<Dream?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDream(dream: Dream): Long

    @Update
    suspend fun updateDream(dream: Dream)

    @Query("DELETE FROM dreams WHERE id = :id")
    suspend fun deleteDream(id: Long)

    @Query("SELECT * FROM chat_messages WHERE dreamId = :dreamId ORDER BY timestamp ASC")
    fun getChatMessagesForDream(dreamId: Long): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage): Long
}

@Database(entities = [Dream::class, ChatMessage::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dreamDao(): DreamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dream_journal_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class DreamRepository(private val dreamDao: DreamDao) {
    val allDreams: Flow<List<Dream>> = dreamDao.getAllDreams()

    fun getDreamById(id: Long): Flow<Dream?> = dreamDao.getDreamById(id)

    suspend fun insertDream(dream: Dream): Long = dreamDao.insertDream(dream)

    suspend fun updateDream(dream: Dream) = dreamDao.updateDream(dream)

    suspend fun deleteDream(id: Long) = dreamDao.deleteDream(id)

    fun getChatMessages(dreamId: Long): Flow<List<ChatMessage>> = dreamDao.getChatMessagesForDream(dreamId)

    suspend fun insertChatMessage(message: ChatMessage): Long = dreamDao.insertChatMessage(message)
}
