package com.example.simpletodo

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun log(context: Context, tag: String, message: String) {
        scope.launch {
            try {
                val db = TodoDatabase.getDatabase(context)
                db.logDao().insert(LogEntry(tag = tag, message = message))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
