# SimpleTodo Requirements Document

## Project Overview
SimpleTodo is a minimalist Android To-Do application focused on task management with custom reminder capabilities, image attachments, and categorization based on time proximity.

## Core Features

### 1. Task Management
- **Create Task**: Title, Remarks (optional), Due Date/Time.
- **Attachments**: Support for up to 3 image attachments per task.
- **Categorization**: Tasks are automatically grouped into:
  - **Within 3 Days** (High Priority/Urgent)
  - **Next 7 Days** (Medium Term)
  - **Further** (Long Term)
- **Status**: Mark tasks as Done/Undone.
- **Edit/Delete**: Full edit capability and deletion with confirmation.

### 2. Reminder System
- **Exact Alarms**: Uses `AlarmManager` for precise notifications.
- **Retry Mechanism**:
  - Customizable **Max Retries** (0-5 times).
  - Customizable **Retry Interval** (1-12 hours).
  - Automatically reschedules reminders if not acted upon.

### 3. Recurring Tasks
- **Repeat Modes**:
  - **None**: Single-time task.
  - **Weekly**: Repeats every 7 days from the due date.
  - **Monthly**: Repeats on the same day next month.
- **Completion Logic**: Marking a recurring task as done automatically reschedules it to the next cycle and resets retry counts.

### 4. UI/UX
- **Material Design 3**: Modern, clean interface adhering to Google's latest design standards.
- **Dynamic Theme**: Supports Light/Dark mode and Dynamic Colors (Android 12+).
- **Visual Cues**:
  - Priority Color Tags (Red/Orange/Green).
  - Image thumbnails with click-to-zoom.
  - Expandable task cards for details.

## Technical Constraints
- **Database**: Local storage using Room Database.
- **Images**: Images are copied to app-private storage to prevent loss if source files are deleted.
- **Android Version**: Min SDK 26 (Android 8.0), Target SDK 34.
