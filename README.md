# SimpleTodo for HyperOS

## Project Setup
This is a simplified Android project structure.

### Prerequisites
- Android Studio Hedgehog or newer (recommended).
- JDK 17.

### How to Open
1. Open Android Studio.
2. Select **Open** and choose the `SimpleTodo` folder (`c:\pro\yt\SimpleTodo`).
3. Android Studio will sync Gradle and generate necessary wrapper files.

### Features
- **Add Todo**: Name, Time, Monthly Cycle.
- **Waterfall View**: Tasks sorted by time (Nearest first).
- **Reminders**:
  - Exact Alarm (requires permission on Android 12+).
  - Notification with "Done" action.
  - **Retry Logic**: If not marked done, retries every 1 hour (up to 3 times total).
- **Cycle**: Monthly tasks automatically reschedule to next month upon completion.

### Permissions
- `POST_NOTIFICATIONS`: For reminders.
- `SCHEDULE_EXACT_ALARM`: For precise timing.

### Note on HyperOS
On HyperOS (Android 14/15), you may need to manually allow "Autostart" for the app in system settings if background alarms are unreliable, although `SCHEDULE_EXACT_ALARM` usually bypasses battery optimization for the alarm delivery.
