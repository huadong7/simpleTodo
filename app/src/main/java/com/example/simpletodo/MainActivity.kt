package com.example.simpletodo

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import android.net.Uri
import android.os.PowerManager
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var database: TodoDatabase
    private lateinit var viewModel: TodoViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications needed for reminders", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        database = TodoDatabase.getDatabase(this)
        val repository = TodoRepository(database.todoDao(), database.logDao())
        viewModel = ViewModelProvider(this, TodoViewModelFactory(repository, applicationContext))[TodoViewModel::class.java]

        checkPermissions()
        checkBatteryOptimization()

        setContent {
            SimpleTodoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TodoApp(viewModel)
                }
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }
    
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                // We should ideally show a rationale dialog first, but for now let's just launch it
                // or handle it in the UI. 
                // Let's launch it directly for simplicity as "Ask Every Time" is not ideal.
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to generic settings if package specific fails
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }
    }
}

// --- Theme ---
@Composable
fun SimpleTodoTheme(content: @Composable () -> Unit) {
    val primaryColor = Color(0xFF6750A4)
    val secondaryColor = Color(0xFF625B71)
    val tertiaryColor = Color(0xFF7D5260)

    val colorScheme = lightColorScheme(
        primary = primaryColor,
        secondary = secondaryColor,
        tertiary = tertiaryColor,
        background = Color(0xFFFFFBFE),
        surface = Color(0xFFFFFBFE),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

// --- ViewModel ---

class TodoViewModel(private val repository: TodoRepository, private val context: Context) : ViewModel() {
    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allTodos.collectLatest {
                _todos.value = it
            }
        }
        viewModelScope.launch {
            repository.allLogs.collectLatest {
                _logs.value = it
            }
        }
    }

    fun addTodo(name: String, timeInMillis: Long, isMonthly: Boolean) {
        viewModelScope.launch {
            val item = TodoItem(name = name, timeInMillis = timeInMillis, isMonthly = isMonthly)
            val id = repository.insert(item)
            AppLogger.log(context, "TodoViewModel", "Added new todo: $name (ID: $id)")
            scheduleAlarm(context, id, timeInMillis)
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.delete(todo)
            AppLogger.log(context, "TodoViewModel", "Deleted todo: ${todo.name}")
            cancelAlarm(context, todo.id)
        }
    }

    fun markDone(todo: TodoItem) {
        viewModelScope.launch {
            if (todo.isMonthly) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = todo.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                
                val nextTime = calendar.timeInMillis
                val updated = todo.copy(timeInMillis = nextTime, remindCount = 0, isDone = false)
                repository.update(updated)
                scheduleAlarm(context, updated.id, nextTime)
                AppLogger.log(context, "TodoViewModel", "Marked monthly done: ${todo.name}, rescheduled to ${Date(nextTime)}")
            } else {
                val updated = todo.copy(isDone = true)
                repository.update(updated)
                cancelAlarm(context, todo.id)
                AppLogger.log(context, "TodoViewModel", "Marked done: ${todo.name}")
            }
        }
    }
    
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
    
    private fun scheduleAlarm(context: Context, id: Long, timeInMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_ID, id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        AppLogger.log(context, "TodoViewModel", "Scheduling alarm for ID $id at ${Date(timeInMillis)}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                 alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                 AppLogger.log(context, "TodoViewModel", "Cannot schedule exact alarm! Using inexact.")
                 alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } else {
             alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }
    
    private fun cancelAlarm(context: Context, id: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java)
        intent.action = ReminderReceiver.ACTION_REMINDER
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        AppLogger.log(context, "TodoViewModel", "Cancelled alarm for ID $id")
    }
}

class TodoRepository(private val dao: TodoDao, private val logDao: LogDao) {
    val allTodos = dao.getAllTodos()
    val allLogs = logDao.getAllLogs()
    
    suspend fun insert(todo: TodoItem) = dao.insert(todo)
    suspend fun update(todo: TodoItem) = dao.update(todo)
    suspend fun delete(todo: TodoItem) = dao.delete(todo)
    
    suspend fun clearLogs() = logDao.clearLogs()
}

class TodoViewModelFactory(private val repository: TodoRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- UI ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(viewModel: TodoViewModel) {
    val todos by viewModel.todos.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }

    if (showLogs) {
        LogViewerScreen(
            viewModel = viewModel,
            onBack = { showLogs = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "My Tasks", 
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        ) 
                    },
                    actions = {
                        IconButton(onClick = { showLogs = true }) {
                            Icon(Icons.Default.Info, contentDescription = "View Logs")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Todo")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Warning Banner for Xiaomi/HyperOS users
                if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "On HyperOS/MIUI, you MUST enable 'Autostart' in App Info settings for reminders to work properly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                val sortedTodos = todos.sortedBy { if (it.isDone) Long.MAX_VALUE else it.timeInMillis }

                if (sortedTodos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Done, 
                                contentDescription = null, 
                                modifier = Modifier.size(64.dp), 
                                tint = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No tasks yet", 
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedTodos, key = { it.id }) { todo ->
                            TodoCard(
                                todo, 
                                onDelete = { viewModel.deleteTodo(todo) },
                                onDone = { viewModel.markDone(todo) }
                            )
                        }
                    }
                }
            }

            if (showDialog) {
                AddTodoDialog(
                    onDismiss = { showDialog = false },
                    onConfirm = { name, time, isMonthly ->
                        viewModel.addTodo(name, time, isMonthly)
                        showDialog = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(viewModel: TodoViewModel, onBack: () -> Unit) {
    val logs by viewModel.logs.collectAsState()
    val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs.size) { index ->
                val log = logs[index]
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = formatter.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = log.tag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.message,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodoCard(todo: TodoItem, onDelete: () -> Unit, onDone: () -> Unit) {
    val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val isOverdue = !todo.isDone && System.currentTimeMillis() > todo.timeInMillis
    
    val cardColor = if (todo.isDone) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else if (isOverdue) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (todo.isDone) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    } else if (isOverdue) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (todo.isDone) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = todo.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (todo.isDone) TextDecoration.LineThrough else null
                    ),
                    color = contentColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Checkbox(
                    checked = todo.isDone,
                    onCheckedChange = { onDone() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = contentColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Notifications, 
                    contentDescription = null, 
                    modifier = Modifier.size(14.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = format.format(Date(todo.timeInMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }

            if (todo.isMonthly) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = null, 
                        modifier = Modifier.size(14.dp),
                        tint = contentColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Monthly",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                 IconButton(
                     onClick = onDelete,
                     modifier = Modifier.size(32.dp)
                 ) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "Delete", 
                        tint = contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(onDismiss: () -> Unit, onConfirm: (String, Long, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var isMonthly by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    // Default to next hour
    calendar.add(Calendar.HOUR_OF_DAY, 1)
    calendar.set(Calendar.MINUTE, 0)
    
    var selectedTime by remember { mutableStateOf(calendar.timeInMillis) }
    
    val dateFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = selectedTime
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            selectedTime = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = selectedTime
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            selectedTime = cal.timeInMillis
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("What needs to be done?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Remind me at", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateFormatter.format(Date(selectedTime)), style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    OutlinedButton(
                        onClick = { timePickerDialog.show() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(timeFormatter.format(Date(selectedTime)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isMonthly = !isMonthly }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(checked = isMonthly, onCheckedChange = { isMonthly = it })
                    Text("Repeat every month", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedTime, isMonthly)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
            ) {
                Text("Create Task")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}
