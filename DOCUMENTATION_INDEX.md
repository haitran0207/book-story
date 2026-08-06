# Book's Story - Documentation Index

## 📚 Overview
This directory contains comprehensive documentation for the Book's Story Android eBook reader project.

## 📖 Documentation Files

### 1. [PROJECT_INDEX.md](PROJECT_INDEX.md)
**Complete codebase index and structure**
- Project overview and statistics
- Directory structure breakdown
- All 558 Kotlin files organized by module
- Feature list
- Technology stack
- Resource organization
- Build configuration

**Use this when**: You need to understand the overall project structure or find where specific code is located.

### 2. [TECHNICAL_GUIDE.md](TECHNICAL_GUIDE.md)
**Developer navigation and implementation patterns**
- Key entry points (Application, MainActivity, Reader)
- Architecture flow diagrams
- Common task workflows
- Adding new features guide
- State management patterns
- Dependency injection with Hilt
- Code style and conventions

**Use this when**: You want to add new features, understand code patterns, or navigate the codebase for development.

### 3. [READER_SYSTEM.md](READER_SYSTEM.md)
**Deep dive into the Reader implementation**
- Reader architecture breakdown
- Component documentation
- State, Events, and Effects system
- Data flow explanations
- Complete example: Adding Read Aloud feature
- Settings system
- Testing considerations
- Performance guidelines

**Use this when**: Working specifically on reader features, understanding how the reading system works, or implementing the Read Aloud feature.

### 4. [BUILD_GUIDE.md](BUILD_GUIDE.md)
**Building and testing instructions**
- Prerequisites and setup
- Building APKs (debug, release)
- Android Studio guide
- Command-line instructions
- Keystore creation for signing
- Installing on devices
- Testing procedures
- Troubleshooting
- CI/CD setup

**Use this when**: You need to build the app, test on devices, or prepare for release.

## 🚀 Quick Start Guide

### For First-Time Setup
1. Read [BUILD_GUIDE.md](BUILD_GUIDE.md) - Prerequisites section
2. Follow Initial Setup steps
3. Build your first APK

### For Understanding the Codebase
1. Start with [PROJECT_INDEX.md](PROJECT_INDEX.md) - Get the big picture
2. Read [TECHNICAL_GUIDE.md](TECHNICAL_GUIDE.md) - Learn the patterns
3. Reference [READER_SYSTEM.md](READER_SYSTEM.md) - Deep dive into reader

### For Adding Read Aloud Feature
1. Read [READER_SYSTEM.md](READER_SYSTEM.md) - "Adding New Reader Features" section
2. Follow the complete Read Aloud implementation example
3. Use [TECHNICAL_GUIDE.md](TECHNICAL_GUIDE.md) for general patterns
4. Test with [BUILD_GUIDE.md](BUILD_GUIDE.md) instructions

### For Building and Testing
1. Follow [BUILD_GUIDE.md](BUILD_GUIDE.md) step by step
2. Start with debug builds for testing
3. Create release builds when ready

## 📋 Common Tasks

### I want to... 
| Task | Documentation | Section |
|------|---------------|---------|
| Understand project structure | [PROJECT_INDEX.md](PROJECT_INDEX.md) | All sections |
| Find where a feature is implemented | [PROJECT_INDEX.md](PROJECT_INDEX.md) | Project Structure |
| Learn architecture patterns | [TECHNICAL_GUIDE.md](TECHNICAL_GUIDE.md) | Architecture Flow |
| Add a new reader feature | [READER_SYSTEM.md](READER_SYSTEM.md) | Adding New Reader Features |
| Implement Read Aloud (TTS) | [READER_SYSTEM.md](READER_SYSTEM.md) | Example: Adding Read Aloud Feature |
| Add a new settings option | [TECHNICAL_GUIDE.md](TECHNICAL_GUIDE.md) | Task: Adding Settings |
| Build an APK for testing | [BUILD_GUIDE.md](BUILD_GUIDE.md) | Debug Build |
| Build for release | [BUILD_GUIDE.md](BUILD_GUIDE.md) | Release Build |
| Install on my device | [BUILD_GUIDE.md](BUILD_GUIDE.md) | Installing on Device |
| Fix build errors | [BUILD_GUIDE.md](BUILD_GUIDE.md) | Troubleshooting |
| Run tests | [BUILD_GUIDE.md](BUILD_GUIDE.md) | Testing |
| Understand reader architecture | [READER_SYSTEM.md](READER_SYSTEM.md) | Reader Architecture |
| Find all reader components | [READER_SYSTEM.md](READER_SYSTEM.md) | UI Layer |

## 🎯 Implementation Roadmap for Read Aloud Feature

Based on [READER_SYSTEM.md](READER_SYSTEM.md), here's your implementation checklist:

### Phase 1: Domain Layer ✓
- [ ] Create `ReadAloudState` model
- [ ] Create `TextToSpeechService` interface
- [ ] Create `StartReadAloudUseCase`
- [ ] Create `StopReadAloudUseCase`
- [ ] Create `PauseReadAloudUseCase`
- [ ] Create `ResumeReadAloudUseCase`

### Phase 2: Data Layer ✓
- [ ] Implement `TextToSpeechServiceImpl`
- [ ] Initialize Android TTS engine
- [ ] Handle TTS lifecycle
- [ ] Implement voice selection
- [ ] Implement speed/pitch controls

### Phase 3: Presentation Layer ✓
- [ ] Update `ReaderState` with `readAloudState`
- [ ] Add events to `ReaderEvent`
- [ ] Add effects to `ReaderEffect` (if needed)
- [ ] Update `ReaderModel` to handle events
- [ ] Implement TTS callbacks
- [ ] Handle text chunking for playback

### Phase 4: UI Layer ✓
- [ ] Create `ReaderReadAloudControls` component
- [ ] Add play/pause button
- [ ] Add stop button
- [ ] Add speed controls
- [ ] Add pitch controls
- [ ] Integrate in `ReaderBottomBar`
- [ ] Add visual feedback (playing indicator)

### Phase 5: Settings ✓
- [ ] Create `read_aloud/` settings directory
- [ ] Create `ReadAloudSubcategory`
- [ ] Add speed setting option
- [ ] Add pitch setting option
- [ ] Add voice selection option
- [ ] Add auto-scroll option
- [ ] Add to reader settings screen

### Phase 6: Resources & DI ✓
- [ ] Add string resources (all languages)
- [ ] Update Hilt modules
- [ ] Add service binding
- [ ] Add permissions (if needed)

### Phase 7: Testing ✓
- [ ] Unit tests for use cases
- [ ] Unit tests for ViewModel
- [ ] UI tests for controls
- [ ] Integration tests with TTS
- [ ] Test on multiple devices

### Phase 8: Polish ✓
- [ ] Add animations
- [ ] Handle interruptions (calls, etc.)
- [ ] Battery optimization
- [ ] Background playback (if desired)
- [ ] Notification controls (optional)
- [ ] Accessibility improvements

## 🛠 Technology Stack

- **Language**: Kotlin 2.2.0
- **UI**: Jetpack Compose
- **Architecture**: Clean Architecture (Domain-Data-Presentation)
- **DI**: Dagger Hilt
- **Database**: Room
- **Build**: Gradle with Kotlin DSL
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36

## 📁 Project Statistics

- **Total Kotlin Files**: 558
- **Main Package**: `ua.acclorite.book_story`
- **Architecture Layers**: 3 (Domain, Data, Presentation/UI)
- **Supported Languages**: 25+
- **Supported Book Formats**: EPUB, PDF, FB2, TXT, ZIP, HTML, HTM

## 🔗 Key Directories

```
app/src/main/java/ua/acclorite/book_story/
├── core/              # Core utilities and helpers
├── data/              # Data layer (repositories, parsers, database)
├── domain/            # Business logic (use cases, models, interfaces)
├── presentation/      # ViewModels and state management
└── ui/                # Jetpack Compose UI components
```

## 📝 Notes

- All documentation is current as of 2026-08-06
- Code examples use Kotlin and Jetpack Compose
- Build instructions are for macOS but work on all platforms
- The project follows Material 3 design guidelines
- License: GPL-3.0-only

## 🤝 Contributing

When adding features:
1. Follow the architecture patterns in [TECHNICAL_GUIDE.md](TECHNICAL_GUIDE.md)
2. Maintain clean architecture separation
3. Use Hilt for dependency injection
4. Write unit tests for use cases
5. Add string resources for all text
6. Update documentation if needed

## 📞 Getting Help

- **GitHub**: Check existing issues and discussions
- **Matrix Chat**: Join the community chat
- **Documentation**: Search these files first
- **Code Comments**: Many files have helpful comments

## 🎓 Learning Path

**Beginner Level**:
1. Read [PROJECT_INDEX.md](PROJECT_INDEX.md)
2. Build a debug APK following [BUILD_GUIDE.md](BUILD_GUIDE.md)
3. Run the app and explore features

**Intermediate Level**:
1. Study [TECHNICAL_GUIDE.md](TECHNICAL_GUIDE.md)
2. Trace code flow for an existing feature
3. Make small UI tweaks
4. Add a simple setting

**Advanced Level**:
1. Deep dive into [READER_SYSTEM.md](READER_SYSTEM.md)
2. Implement Read Aloud feature
3. Add new reader functionality
4. Optimize performance

---

## 🎉 Ready to Start?

1. **Setup**: Follow [BUILD_GUIDE.md](BUILD_GUIDE.md) to set up your environment
2. **Explore**: Use [PROJECT_INDEX.md](PROJECT_INDEX.md) to understand the structure
3. **Learn**: Read [TECHNICAL_GUIDE.md](TECHNICAL_GUIDE.md) for development patterns
4. **Build**: Follow [READER_SYSTEM.md](READER_SYSTEM.md) to implement Read Aloud
5. **Test**: Build and test your APK!

Good luck! 🚀
