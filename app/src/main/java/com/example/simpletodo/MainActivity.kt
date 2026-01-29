package com.example.simpletodo

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
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
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }
    }
}

// --- Theme ---
@Composable
fun SimpleTodoTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    
    val primaryColor = Color(0xFF4A7DFF)
    val secondaryColor = Color(0xFF625B71)
    val tertiaryColor = Color(0xFF7D5260)

    val lightColors = lightColorScheme(
        primary = primaryColor,
        secondary = secondaryColor,
        tertiary = tertiaryColor,
        background = Color(0xFFF5F5F5),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
    )
    
    val darkColors = darkColorScheme(
        primary = Color(0xFFB3C5FF),
        secondary = Color(0xFFCCC2DC),
        tertiary = Color(0xFFEFB8C8),
        background = Color(0xFF141218),
        surface = Color(0xFF141218),
        onPrimary = Color(0xFF002A78),
        onSecondary = Color(0xFF332D41),
        onTertiary = Color(0xFF492532),
        onBackground = Color(0xFFE6E1E5),
        onSurface = Color(0xFFE6E1E5),
    )

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColors
        else -> lightColors
    }

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

    fun addTodo(name: String, timeInMillis: Long, repeatMode: Int, remarks: String, imageUris: List<Uri>, maxRetries: Int, retryIntervalHours: Int) {
        viewModelScope.launch {
            val imagePaths = imageUris.mapNotNull { uri ->
                ImageStorageManager.copyImageToInternalStorage(context, uri)
            }
            
            val item = TodoItem(
                name = name, 
                timeInMillis = timeInMillis, 
                // isMonthly deprecated, use repeatMode. 0=None, 1=Weekly, 2=Monthly
                repeatMode = repeatMode,
                isMonthly = repeatMode == 2, 
                remarks = remarks,
                imagePaths = imagePaths,
                maxRetries = maxRetries,
                retryIntervalHours = retryIntervalHours
            )
            val id = repository.insert(item)
            AppLogger.log(context, "TodoViewModel", "Added new todo: $name (ID: $id)")
            scheduleAlarm(context, id, timeInMillis)
        }
    }

    fun updateTodo(todo: TodoItem, name: String, timeInMillis: Long, repeatMode: Int, remarks: String, imageUris: List<Uri>, maxRetries: Int, retryIntervalHours: Int) {
        viewModelScope.launch {
            // Process new images if any
            val newImagePaths = imageUris.mapNotNull { uri ->
                ImageStorageManager.copyImageToInternalStorage(context, uri)
            }
            
            val updated = todo.copy(
                name = name,
                timeInMillis = timeInMillis,
                repeatMode = repeatMode,
                isMonthly = repeatMode == 2,
                remarks = remarks,
                imagePaths = todo.imagePaths + newImagePaths, // Append new images
                maxRetries = maxRetries,
                retryIntervalHours = retryIntervalHours
            )
            repository.update(updated)
            scheduleAlarm(context, updated.id, timeInMillis)
            AppLogger.log(context, "TodoViewModel", "Updated todo: ${updated.name}")
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
            if (todo.repeatMode > 0 || todo.isMonthly) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = todo.timeInMillis
                
                if (todo.repeatMode == 1) {
                     calendar.add(Calendar.DAY_OF_YEAR, 7) // Weekly
                } else {
                     calendar.add(Calendar.MONTH, 1) // Monthly (Default or repeatMode 2)
                }
                
                val nextTime = calendar.timeInMillis
                val updated = todo.copy(timeInMillis = nextTime, remindCount = 0, isDone = false)
                repository.update(updated)
                scheduleAlarm(context, updated.id, nextTime)
                AppLogger.log(context, "TodoViewModel", "Marked recurring done: ${todo.name}, rescheduled to ${Date(nextTime)}")
            } else {
                val updated = todo.copy(isDone = !todo.isDone) // Toggle logic
                repository.update(updated)
                if (updated.isDone) {
                    cancelAlarm(context, todo.id)
                    AppLogger.log(context, "TodoViewModel", "Marked done: ${todo.name}")
                } else {
                    scheduleAlarm(context, todo.id, todo.timeInMillis)
                    AppLogger.log(context, "TodoViewModel", "Marked undone: ${todo.name}")
                }
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
    var editTodo by remember { mutableStateOf<TodoItem?>(null) }
    var showLogs by remember { mutableStateOf(false) }

    if (showLogs) {
        LogViewerScreen(
            viewModel = viewModel,
            onBack = { showLogs = false }
        )
    } else {
        Scaffold(
            topBar = {
                // Compact Top Bar with integrated Warning and Actions
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 4.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                             // Title/Hint Area
                             Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                 if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) {
                                     Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                     Spacer(modifier = Modifier.width(4.dp))
                                     Text(
                                         "Xiaomi: Enable Autostart",
                                         style = MaterialTheme.typography.labelMedium,
                                         color = MaterialTheme.colorScheme.error,
                                         maxLines = 1,
                                         overflow = TextOverflow.Ellipsis
                                     )
                                 } else {
                                     Text(
                                         "SimpleTodo",
                                         style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                     )
                                 }
                             }
                             
                             // Actions
                             IconButton(onClick = { showLogs = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Info, contentDescription = "Logs", tint = MaterialTheme.colorScheme.onSurface)
                             }
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { 
                        editTodo = null
                        showDialog = true 
                    },
                    containerColor = Color(0xFF4A7DFF),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Todo", modifier = Modifier.size(32.dp))
                }
            }
        ) { padding ->
            TaskListScreen(
                todos = todos, 
                viewModel = viewModel, 
                padding = padding, 
                onEdit = { todo ->
                    editTodo = todo
                    showDialog = true
                }
            )

            if (showDialog) {
                AddEditTodoDialog(
                    todo = editTodo,
                    onDismiss = { showDialog = false },
                    onConfirm = { name, time, repeatMode, remarks, images, maxRetries, retryInterval ->
                        if (editTodo == null) {
                            viewModel.addTodo(name, time, repeatMode, remarks, images, maxRetries, retryInterval)
                        } else {
                            viewModel.updateTodo(editTodo!!, name, time, repeatMode, remarks, images, maxRetries, retryInterval)
                        }
                        showDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun TaskListScreen(todos: List<TodoItem>, viewModel: TodoViewModel, padding: PaddingValues, onEdit: (TodoItem) -> Unit) {
    val now = System.currentTimeMillis()
    val threeDays = 3 * 24 * 60 * 60 * 1000L
    val sevenDays = 7 * 24 * 60 * 60 * 1000L

    // Categorize
    val within3Days = todos.filter { !it.isDone && it.timeInMillis <= now + threeDays }.sortedBy { it.timeInMillis }
    val next7Days = todos.filter { !it.isDone && it.timeInMillis > now + threeDays && it.timeInMillis <= now + sevenDays }.sortedBy { it.timeInMillis }
    val further = todos.filter { !it.isDone && it.timeInMillis > now + sevenDays }.sortedBy { it.timeInMillis }
    
    Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
        // Warning Banner removed from here as it's moved to TopBar
        
        if (within3Days.isNotEmpty()) {
            TaskGroupCard(title = "Within 3 Days", tasks = within3Days, viewModel = viewModel, onEdit = onEdit)
        }
        
        if (next7Days.isNotEmpty()) {
            TaskGroupCard(title = "Next 7 Days", tasks = next7Days, viewModel = viewModel, onEdit = onEdit)
        }
        
        if (further.isNotEmpty()) {
            TaskGroupCard(title = "Further", tasks = further, viewModel = viewModel, onEdit = onEdit)
        }
        
        if (within3Days.isEmpty() && next7Days.isEmpty() && further.isEmpty()) {
             Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No pending tasks", color = Color.Gray)
             }
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
    }
}

@Composable
fun TaskGroupCard(title: String, tasks: List<TodoItem>, viewModel: TodoViewModel, onEdit: (TodoItem) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(modifier = Modifier.weight(1f))
            Text("${tasks.size}", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                // Yellow Accent Bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFFFC107)) // Amber/Yellow
                )
                
                Column {
                    tasks.forEachIndexed { index, todo ->
                        TaskItemRow(todo, viewModel, onEdit)
                        if (index < tasks.size - 1) {
                            Divider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImagePreviewDialog(imagePath: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
            
            val state = rememberTransformableState { zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 3f)
                // offset += panChange 
            }
            
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state)
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close, 
                    contentDescription = "Close", 
                    tint = Color.White
                )
            }
        }
    }
}

// ...

@Composable
fun TaskItemRow(todo: TodoItem, viewModel: TodoViewModel, onEdit: (TodoItem) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var previewImage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val format = SimpleDateFormat("MMM dd", Locale.getDefault())
    val now = System.currentTimeMillis()
    val isOverdue = now > todo.timeInMillis
    val isDueToday = todo.timeInMillis - now < 24 * 60 * 60 * 1000L && !isOverdue
    val isDueSoon = todo.timeInMillis - now < 3 * 24 * 60 * 60 * 1000L
    
    val priorityColor = when {
        isOverdue || isDueToday -> Color(0xFFFF6B6B) // High: Red
        isDueSoon -> Color(0xFFFFA726) // Medium: Orange
        else -> Color(0xFF66BB6A) // Low: Green
    }
    
    if (previewImage != null) {
        ImagePreviewDialog(imagePath = previewImage!!, onDismiss = { previewImage = null })
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除任务") },
            text = { Text("确定要删除 '${todo.name}' 吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTodo(todo)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { expanded = !expanded }
        .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Checkbox
            Checkbox(
                checked = todo.isDone,
                onCheckedChange = { viewModel.markDone(todo) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = Color.Gray
                ),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else null
                )
                if (todo.remarks.isNotEmpty()) {
                    Text(
                        text = todo.remarks,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Metadata & Priority
            Column(horizontalAlignment = Alignment.End) {
                // Priority Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(priorityColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = format.format(Date(todo.timeInMillis)),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (todo.repeatMode > 0) {
                         Surface(
                             color = MaterialTheme.colorScheme.secondaryContainer,
                             shape = RoundedCornerShape(4.dp),
                             modifier = Modifier.padding(end = 4.dp)
                         ) {
                             Text(
                                 text = if (todo.repeatMode == 1) "每周" else "每月",
                                 style = MaterialTheme.typography.labelSmall,
                                 modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                 color = MaterialTheme.colorScheme.onSecondaryContainer
                             )
                         }
                    }
                    if (todo.imagePaths.isNotEmpty()) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    // Expand Arrow Hint
                    Icon(
                        Icons.Default.KeyboardArrowDown, 
                        contentDescription = "Expand", 
                        modifier = Modifier.size(16.dp),
                        tint = Color.LightGray
                    )
                }
            }
        }
        
        // Expanded Content (Images)
        if (expanded && todo.imagePaths.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                todo.imagePaths.forEach { path ->
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Attachment",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .clickable { previewImage = path }
                    )
                }
            }
        }
        
        // Expanded Content (Actions)
        if (expanded) {
             Spacer(modifier = Modifier.height(12.dp))
             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledTonalButton(
                    onClick = { onEdit(todo) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
             }
        }
    }
}

@Composable
fun NumericStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    unit: String = ""
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
    ) {
        IconButton(
            onClick = { if (value > range.first) onValueChange(value - 1) },
            enabled = value > range.first
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Decrease")
        }
        
        Text(
            text = "$value$unit",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 8.dp),
            fontWeight = FontWeight.Bold
        )
        
        IconButton(
            onClick = { if (value < range.last) onValueChange(value + 1) },
            enabled = value < range.last
        ) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Increase")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTodoDialog(
    todo: TodoItem? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String, Long, Int, String, List<Uri>, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf(todo?.name ?: "") }
    var remarks by remember { mutableStateOf(todo?.remarks ?: "") }
    var repeatMode by remember { mutableStateOf(todo?.repeatMode ?: 0) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var maxRetries by remember { mutableStateOf(todo?.maxRetries ?: 3) }
    var retryInterval by remember { mutableStateOf(todo?.retryIntervalHours ?: 1) }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    if (todo != null) {
        calendar.timeInMillis = todo.timeInMillis
    } else {
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
    }
    var selectedTime by remember { mutableStateOf(calendar.timeInMillis) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedImages = (selectedImages + uris).take(3 - (todo?.imagePaths?.size ?: 0))
    }

    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> 
        val c = Calendar.getInstance().apply { timeInMillis = selectedTime; set(Calendar.YEAR, y); set(Calendar.MONTH, m); set(Calendar.DAY_OF_MONTH, d) }
        selectedTime = c.timeInMillis
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    val timePickerDialog = TimePickerDialog(context, { _, h, m -> 
        val c = Calendar.getInstance().apply { timeInMillis = selectedTime; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
        selectedTime = c.timeInMillis
    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (todo == null) "New Task" else "Edit Task") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = remarks, 
                    onValueChange = { if (it.length <= 1000) remarks = it }, 
                    label = { Text("Remarks (Optional)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row {
                    OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.weight(1f)) {
                         Text(SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(selectedTime)))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { timePickerDialog.show() }, modifier = Modifier.weight(1f)) {
                         Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(selectedTime)))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Existing Images (for Edit mode)
                if (todo != null && todo.imagePaths.isNotEmpty()) {
                    Text("Existing Images:", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        todo.imagePaths.forEach { path ->
                            AsyncImage(model = File(path), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color.LightGray))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Images (${selectedImages.size + (todo?.imagePaths?.size ?: 0)}/3)")
                }
                if (selectedImages.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        selectedImages.forEach { uri ->
                            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Advanced Settings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Divider()
                
                // Repeat Mode Selection
                Text("Repeat:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = repeatMode == 0, onClick = { repeatMode = 0 })
                        Text("None", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = repeatMode == 1, onClick = { repeatMode = 1 })
                        Text("Weekly", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = repeatMode == 2, onClick = { repeatMode = 2 })
                        Text("Monthly", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Retries:", style = MaterialTheme.typography.bodyMedium)
                    NumericStepper(
                        value = maxRetries,
                        onValueChange = { maxRetries = it },
                        range = 0..5
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Interval:", style = MaterialTheme.typography.bodyMedium)
                    NumericStepper(
                        value = retryInterval,
                        onValueChange = { retryInterval = it },
                        range = 1..12,
                        unit = "h"
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, selectedTime, repeatMode, remarks, selectedImages, maxRetries, retryInterval) }) {
                Text(if (todo == null) "Save" else "Update")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
