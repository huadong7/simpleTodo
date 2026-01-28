package com.example.simpletodo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val timeInMillis: Long,
    val isMonthly: Boolean = false,
    val remindCount: Int = 0,
    val isDone: Boolean = false,
    
    // New fields for v1.3
    val remarks: String = "",
    val imagePaths: List<String> = emptyList(),
    val maxRetries: Int = 3,
    val retryIntervalHours: Int = 1
)
