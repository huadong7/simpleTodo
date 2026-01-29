# Changelog

## [v1.6] - 2026-01-29
### Added
- **Documentation**: Added REQUIREMENTS.md, CHANGELOG.md, and ARCHITECTURE.md.
- **Theme**: Full Material Design 3 support with Dynamic Colors and Dark Mode.
- **UI**:
  - Priority Tags: Red (High), Orange (Medium), Green (Low).
  - Repeat Chip: Visual indicator for recurring tasks.
  - Expand Arrow: Subtle indicator for expandable rows.
  - Numeric Steppers: Replaced sliders for retry settings.
### Changed
- **Localization**: Action buttons (Edit/Delete) now use Chinese labels ("编辑", "删除").
- **Visuals**: Increased date font size for better readability.

## [v1.5] - 2026-01-29
### Added
- **Edit Functionality**: Ability to edit existing tasks including images.
- **Delete Confirmation**: Safety dialog before deleting tasks.
- **Weekly Repeat**: Added "Weekly" option alongside Monthly.
### Changed
- **UI Layout**: Removed Bottom Navigation Bar; Simplified Top Bar.
- **Database**: Migrated to Schema v4 (Added `repeatMode`).

## [v1.4] - 2026-01-29
### Added
- **Image Zoom**: Click-to-zoom for attached images.
- **App Icon**: New "Checklist" vector icon.
### Changed
- **UI Cleanup**: Hidden Calendar/Profile sections.
- **Database**: Optimized migration strategy (Non-destructive).

## [v1.3] - 2026-01-29
### Added
- **Grouping**: Tasks grouped by time (3 Days, 7 Days, Further).
- **Attachments**: Support for adding images.
- **Remarks**: Added remarks field.
- **Custom Retries**: Added configurable retry count and interval.
### Changed
- **Layout**: Switched from Grid to Vertical List.
- **Database**: Migrated to Schema v3.
