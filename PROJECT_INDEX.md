# Book's Story - Project Index

## Project Overview
**Book's Story** is a Material You eBook reader for Android built with Jetpack Compose and Kotlin.

- **Package Name**: `ua.acclorite.book_story`
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **Version**: 1.8.0 (versionCode 14)
- **Total Kotlin Files**: 558
- **Build System**: Gradle with Kotlin DSL
- **Architecture**: Clean Architecture (Domain, Data, Presentation layers)

## Key Technologies
- **UI Framework**: Jetpack Compose
- **Dependency Injection**: Dagger Hilt
- **Database**: Room
- **Language**: Kotlin
- **Build Tool**: Gradle 8.11.1
- **Kotlin Version**: 2.2.0

## Project Structure

### 1. Core Module (`/core`)
Core utilities and helpers used across the application.

#### Directories:
- **`crash/`** - Crash reporting and handling
- **`data/`** - Core data structures
- **`helpers/`** - Utility helper functions
- **`language/`** - Language and localization utilities
- **`log/`** - Logging functionality
- **`ui/`** - Core UI components and utilities

### 2. Data Layer (`/data`)
Handles data operations, storage, and parsing.

#### Key Subdirectories:

**`converter/`** - Data type converters for Room database

**`di/`** - Dependency injection modules

**`local/`** - Local data sources
- `data_store/` - DataStore preferences
- `dto/` - Data Transfer Objects
- `room/` - Room database entities and DAOs

**`mapper/`** - Entity to domain model mappers
- `book/` - Book entity mappers
- `category/` - Category mappers
- `color_preset/` - Color preset mappers
- `file/` - File mappers
- `history/` - History mappers

**`model/`** - Data models
- `file/` - File-related models

**`parser/`** - Document parsing
- `cover/` - Book cover extraction
- `document/` - Document parsing (EPUB, PDF, etc.)
- `file/` - File parsing utilities
- `text/` - Text parsing and processing

**`repository/`** - Repository implementations

**`service/`** - Background services

**`settings/`** - Settings management
- `model/` - Settings models

### 3. Domain Layer (`/domain`)
Business logic and use cases.

#### Key Subdirectories:

**`model/`** - Domain models
- `file/` - File domain models
- `history/` - Reading history models
- `library/` - Library models
- `reader/` - Reader models

**`repository/`** - Repository interfaces

**`service/`** - Service interfaces

**`use_case/`** - Application use cases
- `book/` - Book-related operations (CRUD, search, etc.)
- `category/` - Category management
- `color_preset/` - Color theme management
- `file_system/` - File system operations
- `history/` - Reading history tracking
- `permission/` - Permission handling
- `settings/` - Settings operations

### 4. Presentation Layer (`/presentation`)
ViewModels and screen state management.

#### Screens:
- **`about/`** - About screen
- **`book_info/`** - Book details and information
- **`browse/`** - File browser for adding books
  - `model/` - Browse screen models
- **`crash/`** - Crash report screen
- **`credits/`** - Credits screen
- **`history/`** - Reading history screen
  - `model/` - History models
- **`library/`** - Main library screen
  - `model/` - Library models
- **`license_info/`** - Individual license information
- **`licenses/`** - Open source licenses
- **`main/`** - Main activity
- **`navigator/`** - Navigation logic
- **`reader/`** - Book reader screen
  - `model/` - Reader models
- **`settings/`** - Settings screens
- **`start/`** - Onboarding/welcome screen

### 5. UI Layer (`/ui`)
Jetpack Compose UI components and screens.

#### Key Subdirectories:

**`about/`** - About screen UI
- `data/` - About screen data
- `model/` - About screen models

**`book_info/`** - Book information UI

**`browse/`** - Browse screen UI

**`common/`** - Shared UI components
- `components/` - Reusable UI components
  - `common/` - General components
  - `dialog/` - Dialog components
  - `modal_bottom_sheet/` - Bottom sheet components
  - `modal_drawer/` - Drawer components
  - `navigation_bar/` - Bottom navigation
  - `navigation_rail/` - Side navigation rail
  - `placeholder/` - Placeholder states
  - `progress_indicator/` - Loading indicators
  - `settings/` - Settings UI components
  - `top_bar/` - Top app bar components
- `data/` - Common data structures
- `helpers/` - UI helper functions
- `model/` - Common UI models

**`crash/`** - Crash screen UI

**`credits/`** - Credits screen UI
- `data/` - Credits data

**`history/`** - History screen UI

**`library/`** - Library screen UI

**`license_info/`** - License details UI

**`licenses/`** - Licenses list UI

**`main/`** - Main activity UI

**`navigator/`** - Navigation components

**`reader/`** - Reader screen UI (Main reading interface)
- `data/` - Reader data structures
- `model/` - Reader models
- Key components:
  - Reading layout and text rendering
  - Chapter navigation
  - Bottom bar controls
  - Settings bottom sheet
  - Progress indicators
  - Gestures and interactions

**`settings/`** - Settings screens UI

#### Settings Categories:

**`appearance/`** - Appearance settings
- `colors/` - Color customization
- `theme_preferences/` - Theme options

**`browse/`** - Browse settings
- `display/` - Display options
- `filter/` - File filtering
- `scan/` - Scanning options
- `sort/` - Sort preferences

**`general/`** - General app settings

**`library/`** - Library settings
- `categories/` - Category management
- `display/` - Display preferences
- `sort/` - Sort options
- `tabs/` - Tab configuration

**`reader/`** - Reader settings (Most complex category)
- `chapters/` - Chapter display settings
- `font/` - Font customization
- `images/` - Image display settings
- `misc/` - Miscellaneous reader options
- `padding/` - Text padding settings
- `progress/` - Progress bar settings
- `reading_mode/` - Reading mode options
- `reading_speed/` - Speed reading features
- `system/` - System-level options
- `text/` - Text formatting
- `translator/` - Translation features

**`start/`** - Welcome screen UI

**`theme/`** - Theme system
- `color/` - Color definitions
- `model/` - Theme models

## Key Features

### Reading Features
1. **Multiple Format Support**: EPUB, PDF, FB2, TXT, ZIP, HTML, HTM
2. **Customizable Reader**: 
   - Font size, family, and style
   - Text alignment and spacing
   - Line and paragraph height
   - Text color and background
3. **Reading Modes**: Different layout modes for optimal reading
4. **Chapter Navigation**: Quick chapter switching
5. **Progress Tracking**: Visual progress indicators
6. **Bookmarks & History**: Track reading progress
7. **Reading Speed Tools**:
   - Highlighted reading
   - Horizontal limiter
   - Perception expander

### Library Features
1. **Library Organization**: Category-based organization
2. **Multiple Views**: Grid and list views
3. **Book Management**: Add, edit, delete books
4. **Search & Filter**: Find books quickly
5. **Reading Status**: Track reading progress

### Appearance
1. **Material You**: Dynamic color theming
2. **Dark Mode**: Full dark theme support
3. **Custom Color Presets**: User-defined color schemes
4. **Theme Contrast**: Adjustable contrast levels

### System Features
1. **Fullscreen Mode**: Immersive reading
2. **Keep Screen On**: Prevent screen timeout
3. **Multi-language**: 25+ language translations
4. **File Browser**: Built-in file picker
5. **Crash Reporting**: Error handling and reporting

## Database Schema

The app uses Room database with schema version 16. Schema files are located in:
```
app/schemas/ua.acclorite.book_story.data.local.room.BookDatabase/
```

Versions: 1 through 16 (migration history preserved)

## Resource Files

### Main Resources
- **`AndroidManifest.xml`** - App manifest
- **`res/values/strings.xml`** - String resources
- **`res/values/colors.xml`** - Color definitions
- **`res/values/themes.xml`** - Theme definitions
- **`res/drawable/`** - Drawable resources
- **`res/font/`** - Custom fonts
- **`res/raw/aboutlibraries.json`** - About libraries data

### Localization
Supports 25+ languages with string translations in:
- `values-ar/` (Arabic)
- `values-de/` (German)
- `values-es/` (Spanish)
- `values-fr/` (French)
- `values-it/` (Italian)
- `values-ja/` (Japanese)
- `values-ko/` (Korean)
- `values-pl/` (Polish)
- `values-pt-rBR/` (Brazilian Portuguese)
- `values-tr/` (Turkish)
- `values-uk/` (Ukrainian)
- `values-zh-rCN/` (Simplified Chinese)
- `values-zh-rTW/` (Traditional Chinese)
- And many more...

## Build Configuration

### Build Types
1. **debug**: Development build with `.debug` suffix
2. **release**: Production build with ProGuard
3. **release-debug**: Release build for debugging

### Dependencies (Key)
- AndroidX Core KTX: 1.17.0
- Lifecycle Runtime: 2.9.4
- Activity Compose: 1.11.0
- Compose Foundation: 1.9.3
- Room: 2.6.1
- Hilt: 2.57.2
- AboutLibraries: 11.1.3

### Gradle Files
- **`build.gradle.kts`** (root) - Project-level configuration
- **`app/build.gradle.kts`** - App module configuration
- **`settings.gradle.kts`** - Project settings
- **`gradle.properties`** - Gradle properties

## Testing & Distribution

### FastLane
Metadata and assets for F-Droid distribution:
```
fastlane/metadata/android/
  en-US/
  uk/
```

### ProGuard
- **`app/proguard-rules.pro`** - Code obfuscation rules

## Development Notes

### Architecture Pattern
The app follows **Clean Architecture** with clear separation:
- **Domain**: Business logic (use cases, repositories, models)
- **Data**: Data handling (repositories impl, parsers, database)
- **Presentation**: ViewModels and state management
- **UI**: Jetpack Compose components and screens

### Navigation
Uses Jetpack Compose Navigation with a custom navigator pattern.

### State Management
- ViewModels for screen state
- DataStore for preferences
- Room for persistent data
- Flow/StateFlow for reactive updates

### Dependency Injection
Dagger Hilt for dependency injection across all layers.

## How to Build

See the main README or build documentation for detailed build instructions.

## License
GPL-3.0-only

## Additional Resources
- **README.md** - Main project documentation
- **LICENSE** - License file
- **ICONS NOTICE.txt** - Icon attribution
- **.agents/BookStory.md** - AI agent instructions

---

**Last Updated**: 2026-08-06
**Total Files Indexed**: 558 Kotlin files across all modules
