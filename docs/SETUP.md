# Development Environment Setup

## Prerequisites

### Required
- **macOS** (Apple Silicon or Intel)
- **Android Studio** Ladybug (2024.2+) or newer
- **JDK 25** (Temurin 25.0.2+ recommended; JDK 21 also works)
- **Kotlin 2.1+** (managed by Gradle)
- **Android SDK** API 35 — install via Android Studio SDK Manager
- **Min SDK**: API 26 (Android 8.0)

### Optional
- **Git** (for version control)
- **GitHub CLI** (`brew install gh`) — for CI/CD and PR management

## Initial Setup

### 1. Install Android Studio
```bash
# Download from https://developer.android.com/studio
# Or via Homebrew:
brew install --cask android-studio
```

### 2. Configure Android SDK
Open Android Studio → Settings → SDK Manager:
- **SDK Platforms**: Android 15 (API 35)
- **SDK Tools**:
  - Android SDK Build-Tools 35
  - Android SDK Command-line Tools
  - Android Emulator
  - Android SDK Platform-Tools

### 3. Create Android Emulator
Android Studio → Device Manager → Create Device:
- **Device**: Pixel 7 (recommended)
- **System Image**: API 35 (x86_64 for Intel Mac, arm64 for Apple Silicon)
- **RAM**: 2048 MB minimum
- **Storage**: 2 GB internal

### 4. Clone and Build
```bash
cd "/Users/anup/AI Workspace/expense-analyst"
./gradlew assembleDebug
```

### 5. Run on Emulator
```bash
# Start emulator (from command line)
emulator -avd Pixel_7_API_34

# Or use Android Studio: Run → Select device → Run 'app'

# CLI install:
./gradlew installDebug
```

## Project Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Install on connected device/emulator
./gradlew installDebug

# Run all unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (emulator must be running)
./gradlew connectedDebugAndroidTest

# Code quality
./gradlew ktlintCheck     # Formatting
./gradlew detekt           # Static analysis

# Clean build
./gradlew clean build

# Generate coverage report
./gradlew jacocoTestReport
```

## APK Signing (Release Builds)

### Generate Keystore (one-time)
```bash
keytool -genkey -v \
  -keystore expense-analyst-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias expense-analyst
```

### Configure in `app/build.gradle.kts`
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../expense-analyst-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "expense-analyst"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### Build Signed APK
```bash
KEYSTORE_PASSWORD=yourpass KEY_PASSWORD=yourpass ./gradlew assembleRelease
# APK at: app/build/outputs/apk/release/app-release.apk
```

## Testing Notification Parsing on Emulator

The emulator can simulate SMS for testing bank parsers:

### Method 1: ADB SMS
```bash
# Send a test SMS simulating HDFC bank
adb emu sms send "HDFCBK" "Rs.450.00 debited from A/C XXXX1234 on 24-Mar-26 at SWIGGY. Avl Bal: Rs.25000.00"
```

### Method 2: Emulator Extended Controls
- Open emulator → `...` (Extended Controls) → Phone → SMS
- Enter sender and message, click Send

### Method 3: Notification via ADB
```bash
# Trigger a test notification
adb shell am broadcast -a com.expenseanalyst.TEST_NOTIFICATION --es message "Rs.500 debited"
```

## Troubleshooting

### Emulator Won't Start
- Check Virtualization is enabled in BIOS (Intel) or use arm64 image (Apple Silicon)
- Increase emulator RAM in AVD settings
- Try cold boot: Device Manager → right-click → Cold Boot Now

### Gradle Build Fails
```bash
# Clean the project first
./gradlew clean
./gradlew assembleDebug
```

If Android Studio shows only a generic KSP error like `KSP failed with exit code: PROCESSING_ERROR`, verify the real cause from the first compiler error line or run:

```bash
./gradlew assembleDebug --stacktrace
```

### gradle-wrapper.jar Missing
If you see `Error: Unable to access jarfile gradle/wrapper/gradle-wrapper.jar`, the jar was not committed. Download it once:
```bash
curl -sL "https://raw.githubusercontent.com/gradle/gradle/v9.3.1/gradle/wrapper/gradle-wrapper.jar" \
  -o gradle/wrapper/gradle-wrapper.jar
```

### gradlew "Could not find or load main class"
If `./gradlew` fails with `Error: Could not find or load main class '-Xmx64m'`, the exec line in `gradlew` is broken (embedded quotes not handled). The current repo has the fix already applied (line 136):
```sh
eval exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS '"$@"'
```
If it ever regresses, apply that fix and re-run.

### Alternative: Direct Gradle Invocation
If `./gradlew` is broken for any reason, use the cached Gradle binary directly:
```bash
~/.gradle/wrapper/dists/gradle-9.3.1-bin/23ovyewtku6u96viwx3xl3oks/gradle-9.3.1/bin/gradle assembleDebug
```

### Re-running After Code Changes
- Sync Gradle files if build scripts changed
- Rebuild the project in Android Studio
- Stop the currently running app session
- Run the app again so the new APK is installed cleanly

### Notification Listener Not Working
- Ensure notification access is granted: Settings → Apps → Special Access → Notification Access
- On emulator: some system settings may need manual navigation
- Check logcat: `adb logcat | grep TransactionNotification`

## IDE Settings (Android Studio)

### Recommended Plugins
- Kotlin (bundled)
- Compose Multiplatform IDE Support
- Detekt (real-time static analysis)
- ktlint (formatting)

### Code Style
Import the project's `.editorconfig` (will be created with project):
```
[*.kt]
indent_size = 4
max_line_length = 120
```
