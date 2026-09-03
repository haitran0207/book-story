# Book's Story - Build & Testing Guide

## ⚡ Quick Compilation Commands

| Action / Build Type | Command | Output Path |
| :--- | :--- | :--- |
| **Build Debug APK (For Testing)** | `./gradlew assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| **Build Release-Debug APK** | `./gradlew assembleReleaseDebug` | `app/build/outputs/apk/releaseDebug/app-release-debug.apk` |
| **Build Release APK (Unsigned)** | `./gradlew assembleRelease` | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| **Clean & Rebuild Debug APK** | `./gradlew clean assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| **Install APK to Connected Phone** | `adb install app/build/outputs/apk/debug/app-debug.apk` | N/A |

### Quick Start Command (One-liner)
```bash
cd /Users/macos/private/SourceCode/book-story && ./gradlew assembleDebug
```

---

## Prerequisites

### Required Software
1. **Android Studio** (latest stable version recommended)
   - Download from: https://developer.android.com/studio
   
2. **JDK 17** 
   - Required for Kotlin 2.2.0
   - Can be installed via Android Studio

3. **Android SDK**
   - API Level 26 (minimum)
   - API Level 36 (target)
   - Install via Android Studio SDK Manager

### Optional Tools
- **Git** - For version control
- **ADB (Android Debug Bridge)** - For device debugging

## Initial Setup

### 1. Clone the Repository
```bash
cd /Users/macos/private/SourceCode
git clone https://github.com/Acclorite/book-story.git
cd book-story
```

### 2. Open in Android Studio
1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to `/Users/macos/private/SourceCode/book-story`
4. Click "Open"

### 3. Sync Gradle
Android Studio will automatically trigger Gradle sync. If not:
1. Click "File" → "Sync Project with Gradle Files"
2. Wait for dependencies to download

### 4. Configure SDK
If prompted:
1. Install required SDK versions (26, 36)
2. Accept licenses
3. Install missing components

## Building the Project

### Method 1: Using Android Studio (Recommended for Beginners)

#### Debug Build (For Testing)
1. Select "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"
2. Wait for build to complete
3. Click "locate" in the notification
4. APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

#### Release Build (For Distribution)
1. Select "Build" → "Generate Signed Bundle / APK"
2. Choose "APK"
3. Click "Next"
4. Either:
   - **Create new keystore**: 
     - Click "Create new..."
     - Fill in keystore details
     - Remember the passwords!
   - **Use existing keystore**:
     - Browse to keystore file
     - Enter passwords
5. Click "Next"
6. Select "release" build variant
7. Check "V2 (Full APK Signature)"
8. Click "Finish"
9. APK will be at: `app/build/outputs/apk/release/app-release.apk`

### Method 2: Using Command Line (Gradle)

#### Navigate to Project Directory
```bash
cd /Users/macos/private/SourceCode/book-story
```

#### Debug Build
```bash
# Make gradlew executable (first time only)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

#### Release Debug Build (Unsigned)
```bash
# Build release-debug variant (debug signed, release optimizations)
./gradlew assembleReleaseDebug

# Output: app/build/outputs/apk/releaseDebug/app-release-debug.apk
```

#### Release Build (Needs signing)
```bash
# Build unsigned release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

#### Clean Build
```bash
# Clean previous builds
./gradlew clean

# Clean and build
./gradlew clean assembleDebug
```

### Method 3: Using Terminal Commands (All Variants)

#### Build All Variants
```bash
./gradlew build
```

#### Build Specific Variant
```bash
# Debug
./gradlew :app:assembleDebug

# Release-Debug
./gradlew :app:assembleReleaseDebug

# Release
./gradlew :app:assembleRelease
```

## Build Variants Explained

### 1. Debug
- **Package**: `ua.acclorite.book_story.debug`
- **Signing**: Debug keystore (automatic)
- **Minification**: Disabled
- **Optimizations**: Disabled
- **Use for**: Development and testing

### 2. Release-Debug
- **Package**: `ua.acclorite.book_story.release.debug`
- **Signing**: Debug keystore (automatic)
- **Minification**: Enabled (ProGuard)
- **Optimizations**: Enabled
- **Use for**: Testing release optimizations without signing

### 3. Release
- **Package**: `ua.acclorite.book_story`
- **Signing**: Requires release keystore
- **Minification**: Enabled (ProGuard)
- **Optimizations**: Enabled
- **Use for**: Production distribution

## Creating a Keystore (For Release Builds)

### Using Command Line
```bash
# Navigate to project root
cd /Users/macos/private/SourceCode/book-story

# Create keystore
keytool -genkey -v -keystore my-release-key.keystore \
  -alias my-key-alias \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# You'll be prompted for:
# - Keystore password
# - Key password
# - Name, organization, etc.
```

### Important Notes
- **Keep keystore safe!** You need it to update the app
- **Remember passwords!** They can't be recovered
- **Backup keystore!** Store in multiple secure locations

### Configure Gradle for Signing
Create or edit `keystore.properties` in project root:
```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=my-key-alias
storeFile=my-release-key.keystore
```

Add to `app/build.gradle.kts`:
```kotlin
// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    // ... existing config
    
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }
    
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            // ... existing config
        }
    }
}
```

**Important**: Add `keystore.properties` to `.gitignore`:
```bash
echo "keystore.properties" >> .gitignore
```

## Installing on Device

### Method 1: Direct Install via Android Studio
1. Connect device via USB
2. Enable USB Debugging on device:
   - Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back → Developer Options
   - Enable "USB Debugging"
3. In Android Studio:
   - Select device from dropdown
   - Click "Run" button (▶️)

### Method 2: Install via Command Line
```bash
# Install debug APK
./gradlew installDebug

# Install release-debug APK
./gradlew installReleaseDebug

# Or use adb directly
adb install app/build/outputs/apk/debug/app-debug.apk

# Install and grant storage permission
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
```

### Method 3: Manual Installation
1. Build APK (see above)
2. Transfer APK to device:
   ```bash
   # Via USB
   adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
   
   # Or transfer via file manager, email, cloud storage, etc.
   ```
3. On device:
   - Open file manager
   - Navigate to APK location
   - Tap APK file
   - Allow "Install from Unknown Sources" if prompted
   - Click "Install"

## Testing

### Run Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run debug unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew test --tests "ua.acclorite.book_story.SomeTest"
```

### Run Instrumented Tests (on device/emulator)
```bash
# Run all instrumented tests
./gradlew connectedAndroidTest

# Run on specific device
adb devices  # List devices
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.deviceId=DEVICE_ID
```

### Run Tests in Android Studio
1. Navigate to test file
2. Click green arrow next to test class/method
3. Or right-click → "Run 'TestName'"

## Troubleshooting

### Build Errors

#### "SDK location not found"
**Solution**: Create `local.properties`:
```properties
sdk.dir=/Users/macos/Library/Android/sdk
```

#### "Gradle sync failed"
**Solution**:
1. Check internet connection
2. Invalidate caches: File → Invalidate Caches / Restart
3. Delete `.gradle` folder and sync again

#### "Unsupported Kotlin version"
**Solution**: Update Android Studio to latest version

#### "Insufficient memory"
**Solution**: Increase Gradle memory in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
```

### Installation Errors

#### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
**Solution**: Uninstall existing app first:
```bash
adb uninstall ua.acclorite.book_story.debug
```

#### "INSTALL_FAILED_INSUFFICIENT_STORAGE"
**Solution**: Free up device storage

#### "App not installed"
**Solution**: 
- Check if APK is corrupted
- Rebuild APK
- Try different signing

## APK Locations

After building, APKs are located at:

```
app/build/outputs/apk/
├── debug/
│   └── app-debug.apk                    # Debug build
├── releaseDebug/
│   └── app-release-debug.apk            # Release-debug build
└── release/
    └── app-release.apk                  # Release build (signed)
    └── app-release-unsigned.apk         # Release build (unsigned)
```

## Optimizing Build Performance

### Enable Gradle Daemon
Add to `gradle.properties`:
```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

### Use Build Cache
```bash
./gradlew --build-cache assembleDebug
```

### Offline Mode (if dependencies cached)
```bash
./gradlew --offline assembleDebug
```

## Continuous Integration (CI)

### GitHub Actions Example
Create `.github/workflows/build.yml`:
```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build with Gradle
      run: ./gradlew assembleDebug
      
    - name: Run tests
      run: ./gradlew test
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

## Quick Reference Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install debug
./gradlew installDebug

# Clean and build
./gradlew clean assembleDebug

# Run tests
./gradlew test

# List all tasks
./gradlew tasks

# Check for updates
./gradlew dependencyUpdates

# Generate AboutLibraries JSON
./gradlew exportLibraryDefinitions

# Uninstall from device
adb uninstall ua.acclorite.book_story.debug

# View device logs
adb logcat | grep "BookStory"

# Clear app data
adb shell pm clear ua.acclorite.book_story.debug
```

## Pro Tips

1. **Use Build Variants**: Switch variants in Android Studio's "Build Variants" panel

2. **Instant Run**: Enable in Android Studio for faster development

3. **Module-level Build**: Speed up by building only changed modules

4. **Gradle Properties**: Configure parallel builds and caching

5. **Emulator Snapshots**: Save emulator state for quick startup

6. **USB Debugging**: Keep enabled for quick testing

7. **Wireless Debugging**: Use ADB over WiFi (Android 11+)

8. **Build Analyzer**: Use Android Studio's Build Analyzer for optimization

9. **Profiler**: Use Android Profiler to test performance

10. **Layout Inspector**: Debug UI issues with Layout Inspector

---

## Next Steps

After building successfully:
1. Test basic functionality
2. Test on multiple devices
3. Test different Android versions
4. Performance testing
5. Memory leak testing
6. Beta testing with users
7. Prepare for release (F-Droid, GitHub, etc.)

Good luck with your build!
