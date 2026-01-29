# Architecture Documentation

## Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Database**: Room Persistence Library
- **Image Loading**: Coil
- **Background Work**: AlarmManager (for precise reminders)

## Project Structure

### Data Layer
- **Entity**: `TodoItem` (Represents a single task).
- **DAO**: `TodoDao` (Data Access Object for database operations).
- **Database**: `TodoDatabase` (Room database definition).
- **Repository**: `TodoRepository` (Single source of truth, manages data flow).

### Domain/Business Logic
- **ViewModel**: `TodoViewModel`
  - Manages UI state (`todos`, `logs`).
  - Handles business logic (Add, Update, Delete, Mark Done).
  - Scheduling logic (interacts with `AlarmManager`).
- **Receiver**: `ReminderReceiver`
  - BroadcastReceiver that triggers notifications.
  - Handles "Retry" logic when alarms fire.

### UI Layer (Compose)
- **MainActivity**: Host activity.
- **Theme**: `SimpleTodoTheme` (M3, Dynamic Colors).
- **Screens**:
  - `TaskListScreen`: Main list view with grouping.
  - `LogViewerScreen`: Debugging log view.
- **Components**:
  - `TaskItemRow`: Individual task card.
  - `AddEditTodoDialog`: Dialog for creating/editing tasks.
  - `ImagePreviewDialog`: Full-screen image viewer.

## Key Workflows

### 1. Reminder Scheduling
When a task is added/updated:
1. `TodoViewModel` calls `scheduleAlarm`.
2. `AlarmManager` sets an exact alarm.
3. `ReminderReceiver` fires at the due time.
4. Notification is shown.
5. If ignored/not done, `ReminderReceiver` checks `remindCount < maxRetries` and reschedules based on `retryIntervalHours`.

### 2. Recurring Tasks
When a recurring task (`repeatMode > 0`) is marked as Done:
1. `TodoViewModel` calculates the next due date (Add 7 days or 1 Month).
2. Database is updated with the new time.
3. `isDone` is reset to `false`.
4. Alarm is rescheduled for the new time.
