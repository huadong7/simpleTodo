package com.example.simpletodo

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items ORDER BY timeInMillis ASC")
    fun getAllTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items ORDER BY timeInMillis ASC")
    suspend fun getAllTodosList(): List<TodoItem>

    @Query("SELECT * FROM todo_items WHERE id = :id")
    suspend fun getTodoById(id: Long): TodoItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoItem): Long

    @Update
    suspend fun update(todo: TodoItem)

    @Delete
    suspend fun delete(todo: TodoItem)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Insert
    suspend fun insert(log: LogEntry)
    
    @Query("DELETE FROM app_logs")
    suspend fun clearLogs()
}

class Converters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return Gson().toJson(list)
    }
}

@Database(entities = [TodoItem::class, LogEntry::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add repeatMode column with default value 0
                db.execSQL("ALTER TABLE todo_items ADD COLUMN repeatMode INTEGER NOT NULL DEFAULT 0")
                
                // Migrate isMonthly to repeatMode
                // If isMonthly was true (1), set repeatMode to 2 (Monthly)
                // We can't easily do conditional update based on boolean in raw SQL effectively cross-db but 
                // in SQLite: UPDATE table SET repeatMode = 2 WHERE isMonthly = 1
                db.execSQL("UPDATE todo_items SET repeatMode = 2 WHERE isMonthly = 1")
            }
        }

        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                .addMigrations(MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
