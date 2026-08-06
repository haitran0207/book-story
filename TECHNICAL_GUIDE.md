# Book's Story - Technical Navigation Guide

## Quick Reference: Key Entry Points

### 1. Application Entry Point
**File**: `app/src/main/java/ua/acclorite/book_story/Application.kt`
- Main application class
- Hilt initialization
- App-wide configuration

### 2. Main Activity
**File**: `app/src/main/java/ua/acclorite/book_story/presentation/main/MainActivity.kt`
- Single activity architecture
- Root navigation setup
- Compose theme application

### 3. Reader Implementation

#### Reader ViewModel (State Management)
**Location**: `app/src/main/java/ua/acclorite/book_story/presentation/reader/`

Key files:
- **`ReaderModel.kt`** - ViewModel for reader screen
- **`ReaderState.kt`** - UI state data class
- **`ReaderEvent.kt`** - User events/actions
- **`ReaderEffect.kt`** - Side effects (one-time events)

#### Reader UI Components
**Location**: `app/src/main/java/ua/acclorite/book_story/ui/reader/`

Key components:
- **`ReaderScaffold.kt`** - Main reader scaffold
- **`ReaderContent.kt`** - Content rendering
- **`ReaderLayout.kt`** - Layout management
- **`ReaderLayoutText.kt`** - Text rendering
- **`ReaderTopBar.kt`** - Top app bar
- **`ReaderBottomBar.kt`** - Bottom controls
- **`ReaderBottomSheet.kt`** - Settings bottom sheet
- **`ReaderProgressBar.kt`** - Progress indicator
- **`ReaderDrawer.kt`** - Side drawer (if any)
- **`ReaderChaptersDrawer.kt`** - Chapter list drawer
- **`ReaderEffects.kt`** - Effect handlers

#### Reader Models
**Location**: `app/src/main/java/ua/acclorite/book_story/ui/reader/model/`
- Various reader-specific data models
- Reading configurations

### 4. Domain Layer - Use Cases

#### Book Operations
**Location**: `app/src/main/java/ua/acclorite/book_story/domain/use_case/book/`

Common use cases:
- Get book content
- Update book progress
- Parse book chapters
- Manage bookmarks

#### File System Operations
**Location**: `app/src/main/java/ua/acclorite/book_story/domain/use_case/file_system/`

Operations for file handling and access.

### 5. Data Layer

#### Repositories
**Location**: `app/src/main/java/ua/acclorite/book_story/data/repository/`

Repository implementations for data access.

#### Parsers
**Location**: `app/src/main/java/ua/acclorite/book_story/data/parser/`

Book format parsers:
- **`document/`** - Document parsing (EPUB, PDF, FB2, etc.)
- **`text/`** - Text extraction and processing
- **`cover/`** - Cover image extraction

#### Room Database
**Location**: `app/src/main/java/ua/acclorite/book_story/data/local/room/`

Database entities and DAOs.

## Architecture Flow

### Reading Flow
```
User Action (UI)
    ↓
ReaderEvent (Event)
    ↓
ReaderModel (ViewModel)
    ↓
Use Case (Domain)
    ↓
Repository (Data)
    ↓
Room/DataStore/Parser
    ↓
StateFlow Update
    ↓
UI Recomposition
```

### State Management Pattern
```kotlin
// 1. User interacts with UI
ReaderScreen(state, onEvent)

// 2. Event sent to ViewModel
onEvent(ReaderEvent.SomeAction)

// 3. ViewModel processes
viewModelScope.launch {
    // Use case invocation
    val result = useCase()
    
    // State update
    _state.update { it.copy(newValue) }
    
    // Or effect for one-time events
    _effect.send(ReaderEffect.SomeEffect)
}

// 4. UI observes state changes
val state by viewModel.state.collectAsState()
```

## Common Tasks Navigation

### Task: Adding a New Reader Feature

1. **Define Domain Model** (if needed)
   - Location: `domain/model/reader/`
   - Add data classes for your feature

2. **Create Use Case** (if needed)
   - Location: `domain/use_case/book/` or create new category
   - Implement business logic

3. **Update Repository** (if needed)
   - Location: `data/repository/`
   - Add data access methods

4. **Update Reader State**
   - File: `presentation/reader/ReaderState.kt`
   - Add state properties

5. **Add Events**
   - File: `presentation/reader/ReaderEvent.kt`
   - Define user actions

6. **Add Effects** (for one-time events)
   - File: `presentation/reader/ReaderEffect.kt`
   - Define side effects

7. **Update ViewModel**
   - File: `presentation/reader/ReaderModel.kt`
   - Handle new events

8. **Create UI Component**
   - Location: `ui/reader/`
   - Build Compose component

9. **Integrate in ReaderScaffold**
   - File: `ui/reader/ReaderScaffold.kt`
   - Add component to layout

10. **Add Settings** (if configurable)
    - Location: `ui/settings/reader/[category]/`
    - Create settings UI

### Task: Adding Settings

1. **Define Setting in Domain**
   - Location: `data/settings/model/`
   - Add setting constant/enum

2. **Update DataStore**
   - Location: `data/local/data_store/`
   - Add preference key

3. **Create Use Case**
   - Location: `domain/use_case/settings/`
   - Get/Set use cases

4. **Create Settings Component**
   - Location: `ui/settings/reader/[category]/components/`
   - Build setting UI (e.g., `*Option.kt`)

5. **Add to Settings Screen**
   - Location: `ui/settings/reader/[category]/`
   - Add to subcategory file

### Task: Adding a New Screen

1. **Create Presentation Layer**
   - Location: `presentation/[screen_name]/`
   - Create Model, State, Event, Effect files

2. **Create UI Layer**
   - Location: `ui/[screen_name]/`
   - Build Compose screen

3. **Add Navigation**
   - Location: `ui/navigator/`
   - Add route and navigation logic

4. **Update Main Activity**
   - If needed, update navigation graph

## Dependency Injection (Hilt)

### Module Locations
**Location**: `data/di/`

Hilt modules for dependency provision:
- Repository modules
- Use case modules
- DataStore modules
- Database modules

### Common Annotations
```kotlin
@HiltViewModel // ViewModel
@Inject constructor // Constructor injection
@HiltAndroidApp // Application
@AndroidEntryPoint // Activities/Fragments
```

## Testing Locations

**Location**: `app/src/test/` (Unit tests)
**Location**: `app/src/androidTest/` (Instrumentation tests)

## Resource Management

### Strings
**File**: `app/src/main/res/values/strings.xml`

Add new strings here and Weblate will handle translations.

### Colors
**Location**: `ui/theme/color/`

Dynamic color system based on Material You.

### Icons/Drawables
**Location**: `app/src/main/res/drawable/`

Vector drawables and icons.

## Build & Run

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Install on Device
```bash
./gradlew installDebug
```

### Run Tests
```bash
./gradlew test
```

## Code Style

- **Language**: Kotlin
- **Formatting**: Follow Kotlin conventions
- **Compose**: Use Material 3 components
- **State**: Use StateFlow for state management
- **Effects**: Use Channel for one-time events
- **Coroutines**: Use viewModelScope for ViewModel operations

## Common Patterns

### State Update Pattern
```kotlin
_state.update { currentState ->
    currentState.copy(
        property = newValue
    )
}
```

### Effect Pattern (One-time Events)
```kotlin
// ViewModel
_effect.send(ReaderEffect.ShowToast("Message"))

// UI
LaunchedEffect(Unit) {
    viewModel.effect.collect { effect ->
        when (effect) {
            is ReaderEffect.ShowToast -> {
                // Show toast
            }
        }
    }
}
```

### Repository Pattern
```kotlin
interface BookRepository {
    suspend fun getBook(id: Int): Book
    fun observeBooks(): Flow<List<Book>>
}

class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao
) : BookRepository {
    // Implementation
}
```

## Useful Gradle Commands

```bash
# Clean build
./gradlew clean

# Build all variants
./gradlew build

# Generate debug APK
./gradlew assembleDebug

# Generate release APK (requires signing)
./gradlew assembleRelease

# Install debug build on connected device
./gradlew installDebug

# Run all tests
./gradlew test

# Run Android tests
./gradlew connectedAndroidTest

# Check for dependency updates
./gradlew dependencyUpdates

# Generate aboutLibraries metadata
./gradlew exportLibraryDefinitions
```

## Key Interfaces to Understand

### Reader System
- Text rendering engine
- Chapter parsing
- Progress tracking
- Bookmark management
- Settings synchronization

### Data Flow
- Room → Repository → Use Case → ViewModel → UI
- Settings: DataStore → Repository → Use Case → ViewModel → UI

### Event Flow
- User Interaction → Event → ViewModel → State Update → UI Recomposition

---

This guide should help you navigate the codebase and understand where to make changes for new features!
