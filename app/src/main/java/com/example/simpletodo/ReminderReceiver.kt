package com.example.simpletodo

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "com.example.simpletodo.ACTION_REMINDER"
        const val ACTION_DONE = "com.example.simpletodo.ACTION_DONE"
        const val EXTRA_ID = "extra_id"
        const val CHANNEL_ID = "todo_channel"
        const val MAX_RETRIES = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "SimpleTodo:ReminderWakeLock")
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        
        AppLogger.log(context, "ReminderReceiver", "onReceive triggered. Action: ${intent.action}")

        scope.launch {
            try {
                val db = TodoDatabase.getDatabase(context)

                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    AppLogger.log(context, "ReminderReceiver", "Handling Boot Completed")
                    rescheduleAllAlarms(context, db)
                } else {
                    val id = intent.getLongExtra(EXTRA_ID, -1)
                    AppLogger.log(context, "ReminderReceiver", "Received ID: $id")
                    if (id != -1L) {
                        val todo = db.todoDao().getTodoById(id)
                        if (todo != null) {
                            if (intent.action == ACTION_DONE) {
                                AppLogger.log(context, "ReminderReceiver", "Marking DONE for: ${todo.name}")
                                handleDone(context, todo, db)
                                NotificationManagerCompat.from(context).cancel(id.toInt())
                            } else {
                                AppLogger.log(context, "ReminderReceiver", "Handling Reminder for: ${todo.name}")
                                handleReminder(context, todo, db)
                            }
                        } else {
                            AppLogger.log(context, "ReminderReceiver", "Todo not found for ID: $id")
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.log(context, "ReminderReceiver", "Error: ${e.message}")
                e.printStackTrace()
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleAllAlarms(context: Context, db: TodoDatabase) {
        try {
            val todos = db.todoDao().getAllTodosList() 
            val now = System.currentTimeMillis()
            var count = 0
            todos.forEach { todo ->
                if (!todo.isDone && todo.timeInMillis > now) {
                    scheduleAlarm(context, todo.id, todo.timeInMillis)
                    count++
                }
            }
            AppLogger.log(context, "ReminderReceiver", "Rescheduled $count alarms")
        } catch (e: Exception) {
            AppLogger.log(context, "ReminderReceiver", "Reschedule Error: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun handleDone(context: Context, todo: TodoItem, db: TodoDatabase) {
        if (todo.isMonthly) {
            // Move to next month
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = todo.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            
            val nextTime = calendar.timeInMillis
            val updatedTodo = todo.copy(
                timeInMillis = nextTime,
                remindCount = 0,
                isDone = false
            )
            db.todoDao().update(updatedTodo)
            scheduleAlarm(context, updatedTodo.id, nextTime)
            AppLogger.log(context, "ReminderReceiver", "Rescheduled Monthly Task to: ${Date(nextTime)}")
        } else {
            // Mark as done
            val updatedTodo = todo.copy(isDone = true)
            db.todoDao().update(updatedTodo)
            AppLogger.log(context, "ReminderReceiver", "Task Marked Done")
        }
    }

    private suspend fun handleReminder(context: Context, todo: TodoItem, db: TodoDatabase) {
        if (todo.isDone) return

        // Show Notification
        showNotification(context, todo)

        // Schedule next retry if needed
        if (todo.remindCount < todo.maxRetries) {
            val intervalMillis = todo.retryIntervalHours * 60 * 60 * 1000L
            val nextRetryTime = System.currentTimeMillis() + intervalMillis
            // Update count
            val updatedTodo = todo.copy(remindCount = todo.remindCount + 1)
            db.todoDao().update(updatedTodo)
            
            // Schedule Retry Alarm
            scheduleAlarm(context, todo.id, nextRetryTime)
            AppLogger.log(context, "ReminderReceiver", "Scheduled Retry #${updatedTodo.remindCount} at ${Date(nextRetryTime)} (Interval: ${todo.retryIntervalHours}h)")
        } else {
            AppLogger.log(context, "ReminderReceiver", "Max Retries Reached for: ${todo.name}")
        }
    }

    private fun showNotification(context: Context, todo: TodoItem) {
        createChannel(context)

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DONE
            putExtra(EXTRA_ID, todo.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            todo.id.toInt() + 10000, // Unique Request Code
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Open App Intent
        val appIntent = Intent(context, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            appIntent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Todo Reminder")
            .setContentText(todo.name)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Done", donePendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(todo.id.toInt(), builder.build())
            AppLogger.log(context, "ReminderReceiver", "Notification Posted for ID: ${todo.id}")
        } else {
            AppLogger.log(context, "ReminderReceiver", "Permission POST_NOTIFICATIONS Denied!")
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Todo Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for todo tasks"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleAlarm(context: Context, id: Long, timeInMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_ID, id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        AppLogger.log(context, "ReminderReceiver", "Scheduling Alarm for ID: $id at ${Date(timeInMillis)}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                 alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                AppLogger.log(context, "ReminderReceiver", "Cannot schedule exact alarm! Using inexact.")
                 alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } else {
             alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }
}
