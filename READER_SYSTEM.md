# Book's Story - Reader System Documentation

## Overview
The Reader is the core feature of Book's Story, responsible for displaying and interacting with book content. It uses a clean architecture pattern with clear separation of concerns.

## Reader Architecture

### 1. Presentation Layer (`presentation/reader/`)

#### ReaderModel.kt (ViewModel)
- **Role**: State management and business logic orchestration
- **Responsibilities**:
  - Manages reader state (ReaderState)
  - Handles user events (ReaderEvent)
  - Emits side effects (ReaderEffect)
  - Coordinates use cases
  - Manages scroll state and progress tracking
  - Handles chapter navigation

#### ReaderState.kt
```kotlin
data class ReaderState(
    val book: Book,                        // Current book
    val text: List<ReaderText>,            // Parsed text content
    val listState: LazyListState,          // Scroll state
    val currentChapter: Chapter?,          // Active chapter
    val currentChapterProgress: Float,     // Reading progress
    val errorMessage: UIText?,             // Error state
    val isLoading: Boolean,                // Loading state
    val showMenu: Boolean,                 // UI menu visibility
    val checkpoints: List<Checkpoint>,     // Saved positions
    val lockMenu: Boolean,                 // Prevent menu interactions
    val bottomSheet: BottomSheet?,         // Active bottom sheet
    val drawer: Drawer?                    // Active drawer
)
```

#### ReaderEvent.kt
User actions that trigger state changes:
- `OnLoadText` - Load book content
- `OnRestoreScroll` - Restore scroll position
- `OnMenuVisibility` - Show/hide reader menu
- `OnChangeProgress` - Update reading progress
- `OnUpdateChapter` - Switch chapters
- `OnScrollToChapter` - Navigate to specific chapter
- `OnScroll` - Handle scroll events
- `OnRestoreCheckpoint` - Return to saved position
- `OnLeave` - Exit reader
- `OnOpenTranslator` - Open text translation
- `OnOpenShareApp` - Share text
- `OnOpenWebBrowser` - Search selected text
- `OnOpenDictionary` - Look up definition
- `OnShowSettingsBottomSheet` - Show settings
- `OnDismissBottomSheet` - Close bottom sheet
- `OnShowChaptersDrawer` - Show chapter list
- `OnDismissDrawer` - Close drawer
- `OnNavigateBack` - Back navigation
- `OnNavigateToBookInfo` - Go to book details

#### ReaderEffect.kt
One-time events (side effects):
- Navigation events
- Toast messages
- System interactions
- Share intents
- Web browser launches

#### ReaderScreen.kt
Main composable function that sets up the reader UI.

### 2. UI Layer (`ui/reader/`)

#### Core Components

**ReaderScaffold.kt**
- Main layout scaffold
- Coordinates all reader components
- Manages top bar, bottom bar, content area
- Handles gestures and interactions

**ReaderContent.kt**
- Main content container
- Renders the reading text
- Manages layout modes
- Handles scrolling

**ReaderLayout.kt**
- Layout manager
- Determines reading layout type
- Switches between different reading modes

**ReaderLayoutText.kt**
- Text-based reading layout
- Primary layout for most books
- Handles text rendering and pagination

**ReaderLayoutTextChapter.kt**
- Chapter rendering component
- Displays chapter titles
- Handles chapter transitions

**ReaderLayoutTextParagraph.kt**
- Paragraph rendering
- Text formatting
- Selection handling

**ReaderLayoutTextImage.kt**
- Image rendering within text
- Image sizing and positioning
- Image interactions

**ReaderLayoutTextSeparator.kt**
- Visual separators between sections
- Chapter breaks
- Scene transitions

#### UI Controls

**ReaderTopBar.kt**
- Title display
- Back button
- Menu button
- Status indicators

**ReaderBottomBar.kt**
- Reading controls
- Chapter navigation (previous/next)
- Quick actions
- Settings access

**ReaderProgressBar.kt**
- Reading progress indicator
- Chapter progress
- Page/percentage display
- Can show at top or bottom

#### Drawers & Sheets

**ReaderBottomSheet.kt**
- Bottom sheet container
- Settings panel
- Reader options

**ReaderSettingsBottomSheet.kt**
- Reader settings UI
- Tab-based settings
- Quick adjustments

**ReaderSettingsBottomSheetTabRow.kt**
- Settings tabs
- Categories: General, Reader, Colors

**ReaderChaptersDrawer.kt**
- Chapter list navigation
- Chapter selection
- Progress indicators per chapter

**ReaderDrawer.kt**
- Side drawer component
- Additional navigation

#### Special Features

**ReaderHorizontalGesture.kt**
- Swipe gesture handling
- Page turning gestures
- Gesture configuration

**ReaderHorizontalLimiter.kt**
- Reading speed assistance
- Horizontal ruler overlay
- Focuses attention on specific lines

**ReaderPerceptionExpander.kt**
- Speed reading aid
- Highlights reading focus area
- Expands/contracts view

#### State Management

**ReaderEffects.kt**
- Effect handlers
- Side effect processing
- System interactions

**ReaderBackHandler.kt**
- Back press handling
- State persistence on back
- Navigation confirmation

**ReaderColorPresetChange.kt**
- Color preset switching
- Theme transitions
- Color management

#### Loading & Error States

**ReaderLoadingPlaceholder.kt**
- Loading indicator
- Skeleton UI
- Progress feedback

**ReaderErrorPlaceholder.kt**
- Error display
- Retry actions
- Error messages

### 3. Domain Models

#### ReaderText (domain/model/reader/)
Represents parsed book content:
```kotlin
sealed class ReaderText {
    data class Chapter(
        val title: String,
        val index: Int
    ) : ReaderText()
    
    data class Paragraph(
        val text: String
    ) : ReaderText()
    
    data class Image(
        val uri: Uri
    ) : ReaderText()
    
    // Other text types
}
```

#### Checkpoint (presentation/reader/model/)
Saved reading positions:
```kotlin
data class Checkpoint(
    val index: Int,           // Item index
    val offset: Int,          // Scroll offset
    val chapter: Chapter?     // Associated chapter
)
```

### 4. Data Flow

#### Loading a Book
```
User opens book
    ↓
ReaderEvent.OnLoadText
    ↓
ReaderModel calls use cases
    ↓
BookRepository fetches book
    ↓
Parser extracts content
    ↓
Text converted to ReaderText list
    ↓
State updated with text
    ↓
UI renders content
```

#### Progress Tracking
```
User scrolls
    ↓
LazyList scroll callback
    ↓
ReaderEvent.OnChangeProgress
    ↓
ReaderModel calculates progress
    ↓
Chapter detection
    ↓
State updated
    ↓
Progress saved to database
    ↓
UI shows updated progress
```

#### Chapter Navigation
```
User selects chapter
    ↓
ReaderEvent.OnScrollToChapter
    ↓
ReaderModel finds chapter index
    ↓
LazyListState.animateScrollToItem()
    ↓
Progress updated
    ↓
Current chapter updated in state
```

## Reader Settings

### Settings Categories

#### Text Settings (`ui/settings/reader/text/`)
- Font family
- Font size
- Line height
- Letter spacing
- Text alignment
- Paragraph spacing
- Paragraph indentation

#### Font Settings (`ui/settings/reader/font/`)
- System fonts
- Custom fonts
- Font file import
- Font preview

#### Padding Settings (`ui/settings/reader/padding/`)
- Horizontal padding
- Vertical padding
- Cutout padding (notches)
- Side padding

#### Progress Settings (`ui/settings/reader/progress/`)
- Progress bar visibility
- Progress bar position
- Progress count type (pages, percentage)
- Font size
- Alignment

#### Chapter Settings (`ui/settings/reader/chapters/`)
- Chapter title display
- Chapter separator
- Chapter start behavior

#### Images Settings (`ui/settings/reader/images/`)
- Image display size
- Image position
- Image caching

#### Reading Mode Settings (`ui/settings/reader/reading_mode/`)
- Vertical scroll
- Horizontal paging
- Continuous scroll
- Page turn animations

#### Reading Speed Settings (`ui/settings/reader/reading_speed/`)
- **Highlighted Reading**: Highlights current line
- **Horizontal Limiter**: Shows ruler to focus
- **Perception Expander**: Focuses on specific area
- Customization for each tool

#### Misc Settings (`ui/settings/reader/misc/`)
- Fullscreen mode
- Keep screen on
- Hide bars on fast scroll
- Cutout handling

#### System Settings (`ui/settings/reader/system/`)
- Screen brightness
- Screen orientation
- System UI visibility

#### Translator Settings (`ui/settings/reader/translator/`)
- Translation service
- Source/target languages
- Translation display

## Adding New Reader Features

### Example: Adding Read Aloud Feature

Here's how you would structure a Text-to-Speech read aloud feature:

#### 1. Define Domain Models
```kotlin
// domain/model/reader/ReadAloud.kt
data class ReadAloudState(
    val isPlaying: Boolean = false,
    val currentTextIndex: Int = 0,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val voice: String? = null
)
```

#### 2. Create Use Cases
```kotlin
// domain/use_case/reader/StartReadAloudUseCase.kt
class StartReadAloudUseCase @Inject constructor(
    private val ttsService: TextToSpeechService
) {
    suspend operator fun invoke(text: String) {
        ttsService.speak(text)
    }
}

// domain/use_case/reader/StopReadAloudUseCase.kt
class StopReadAloudUseCase @Inject constructor(
    private val ttsService: TextToSpeechService
) {
    operator fun invoke() {
        ttsService.stop()
    }
}
```

#### 3. Create TTS Service
```kotlin
// domain/service/TextToSpeechService.kt
interface TextToSpeechService {
    suspend fun initialize()
    suspend fun speak(text: String)
    fun pause()
    fun resume()
    fun stop()
    fun setSpeed(speed: Float)
    fun setPitch(pitch: Float)
    fun getAvailableVoices(): List<Voice>
}

// data/service/TextToSpeechServiceImpl.kt
class TextToSpeechServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeechService {
    private var tts: TextToSpeech? = null
    
    override suspend fun initialize() {
        // Initialize Android TTS
    }
    
    override suspend fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }
    
    // Implement other methods
}
```

#### 4. Update Reader State
```kotlin
// presentation/reader/ReaderState.kt
data class ReaderState(
    // ... existing properties
    val readAloudState: ReadAloudState = ReadAloudState()
)
```

#### 5. Add Events
```kotlin
// presentation/reader/ReaderEvent.kt
sealed class ReaderEvent {
    // ... existing events
    
    data object OnStartReadAloud : ReaderEvent()
    data object OnPauseReadAloud : ReaderEvent()
    data object OnResumeReadAloud : ReaderEvent()
    data object OnStopReadAloud : ReaderEvent()
    data class OnChangeReadAloudSpeed(val speed: Float) : ReaderEvent()
    data class OnChangeReadAloudPitch(val pitch: Float) : ReaderEvent()
}
```

#### 6. Update ViewModel
```kotlin
// presentation/reader/ReaderModel.kt
@HiltViewModel
class ReaderModel @Inject constructor(
    private val startReadAloudUseCase: StartReadAloudUseCase,
    private val stopReadAloudUseCase: StopReadAloudUseCase,
    // ... other dependencies
) : ViewModel() {
    
    fun onEvent(event: ReaderEvent) {
        when (event) {
            is ReaderEvent.OnStartReadAloud -> {
                viewModelScope.launch {
                    val textToRead = getCurrentText()
                    startReadAloudUseCase(textToRead)
                    _state.update { 
                        it.copy(
                            readAloudState = it.readAloudState.copy(
                                isPlaying = true
                            )
                        )
                    }
                }
            }
            is ReaderEvent.OnStopReadAloud -> {
                stopReadAloudUseCase()
                _state.update {
                    it.copy(
                        readAloudState = ReadAloudState()
                    )
                }
            }
            // Handle other events
        }
    }
}
```

#### 7. Create UI Component
```kotlin
// ui/reader/ReaderReadAloudControls.kt
@Composable
fun ReaderReadAloudControls(
    state: ReadAloudState,
    onEvent: (ReaderEvent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Play/Pause button
        IconButton(
            onClick = {
                if (state.isPlaying) {
                    onEvent(ReaderEvent.OnPauseReadAloud)
                } else {
                    onEvent(ReaderEvent.OnStartReadAloud)
                }
            }
        ) {
            Icon(
                imageVector = if (state.isPlaying) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                contentDescription = "Play/Pause"
            )
        }
        
        // Stop button
        IconButton(
            onClick = { onEvent(ReaderEvent.OnStopReadAloud) }
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop"
            )
        }
        
        // Speed control
        Text("Speed: ${state.speed}x")
        
        // Add more controls
    }
}
```

#### 8. Integrate in ReaderBottomBar
```kotlin
// ui/reader/ReaderBottomBar.kt
@Composable
fun ReaderBottomBar(
    state: ReaderState,
    onEvent: (ReaderEvent) -> Unit
) {
    Column {
        // Existing controls
        
        // Add read aloud controls
        ReaderReadAloudControls(
            state = state.readAloudState,
            onEvent = onEvent
        )
    }
}
```

#### 9. Add Settings
```kotlin
// ui/settings/reader/read_aloud/ReadAloudSubcategory.kt
@Composable
fun ReadAloudSubcategory() {
    Column {
        // Speed setting
        ReadAloudSpeedOption()
        
        // Pitch setting
        ReadAloudPitchOption()
        
        // Voice selection
        ReadAloudVoiceOption()
        
        // Auto-scroll setting
        ReadAloudAutoScrollOption()
    }
}
```

#### 10. Add String Resources
```xml
<!-- res/values/strings.xml -->
<string name="read_aloud">Read Aloud</string>
<string name="play">Play</string>
<string name="pause">Pause</string>
<string name="stop">Stop</string>
<string name="read_aloud_speed">Reading Speed</string>
<string name="read_aloud_pitch">Voice Pitch</string>
```

#### 11. Add Permissions (if needed)
```xml
<!-- AndroidManifest.xml -->
<!-- Usually not needed for TTS, but add if required -->
```

#### 12. Update Hilt Module
```kotlin
// data/di/ServiceModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    
    @Binds
    @Singleton
    abstract fun bindTextToSpeechService(
        impl: TextToSpeechServiceImpl
    ): TextToSpeechService
}
```

## Testing Considerations

### Unit Tests
- Test use cases in isolation
- Mock TTS service
- Verify state updates

### UI Tests
- Test button interactions
- Verify state changes
- Test error handling

### Integration Tests
- Test with actual TTS engine
- Verify full flow
- Test edge cases

## Performance Considerations

### Memory
- Release TTS resources when not in use
- Avoid loading entire book at once
- Use lazy loading for chapters

### Battery
- TTS uses significant battery
- Consider wake locks carefully
- Offer battery saver mode

### Threading
- TTS callbacks on main thread
- Use coroutines for async operations
- Handle lifecycle properly

---

This documentation provides a complete blueprint for understanding and extending the Book's Story reader system!
