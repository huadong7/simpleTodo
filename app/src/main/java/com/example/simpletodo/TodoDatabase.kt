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
import kotlinx.coroutines.flow.Flow

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

@Database(entities = [TodoItem::class, LogEntry::class], version = 2, exportSchema = false)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                .fallbackToDestructiveMigration() // Simple migration strategy for dev
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
