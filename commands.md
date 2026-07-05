# CyberGuard AI - Gradle Commands Cheat Sheet

This document contains a comprehensive list of all the essential Gradle commands used to build, test, manage caches, and debug the CyberGuard AI application.

## Building & Compiling

| Command | Description |
| :--- | :--- |
| `./gradlew assembleDebug` | Builds the debug APK (located in `app/build/outputs/apk/debug/`). |
| `./gradlew assembleRelease` | Builds the release APK. |
| `./gradlew bundleRelease` | Builds the Android App Bundle (.aab) required for Google Play Store upload. |
| `./gradlew clean` | Deletes the `build/` directory, wiping all compiled binaries and temporary build files. |

## Testing

| Command | Description |
| :--- | :--- |
| `./gradlew testDebugUnitTest` | Runs all local Unit Tests (Robolectric, MockK, Coroutines) using your computer's JVM. |
| `./gradlew connectedAndroidTest` | Runs all Instrumented UI Tests (Jetpack Compose) on a connected physical device or emulator. |
| `./gradlew testDebugUnitTest --tests "*SosBypassChaosTest"` | Runs a specific test class instead of the entire suite (wildcards accepted). |
| `./gradlew connectedAndroidTest --rerun-tasks` | Forces all instrumented tests to run from scratch, bypassing previously cached test results. |

## Cache Management & Memory Rescue
Use these commands if Gradle hangs, your RAM is dangerously full (to prevent Kernel Panics), or you suspect a corrupted build cache.

| Command | Description |
| :--- | :--- |
| `./gradlew --stop` | Immediately kills all running Gradle Daemon background processes to free up system RAM. |
| `rm -rf ~/.gradle/caches/` | Manually deletes the entire global Gradle cache (run this in bash if things get totally corrupted). |
| `./gradlew cleanBuildCache` | Safely clears the local Gradle build cache. |
| `./gradlew build --no-build-cache` | Runs a build while completely ignoring the build cache (useful for debugging cache-miss errors). |
| `./gradlew build --no-daemon` | Runs the build without spawning a persistent background daemon (slower, but uses strictly less memory). |

## Running & Installing on Device

| Command | Description |
| :--- | :--- |
| `./gradlew installDebug` | Compiles and installs the debug APK directly onto your connected USB device. |
| `adb devices` | Lists all connected Android devices to ensure your phone is properly hooked up via USB Debugging. |
| `adb logcat -s "CyberGuard"` | Streams the live device logs specifically filtered for our app's `Log.i(tag, ...)` outputs. |
| `adb logcat -c` | Clears the massive log buffer on your Android device (useful before starting a fresh test). |

## Advanced Flags & Diagnostics
Append these flags to **any** of the Gradle commands above to get more insight into what Gradle is doing under the hood.

* `--info`: Prints verbose output (excellent for seeing exactly which step is failing or hanging).
* `--stacktrace`: Prints a full Java/Kotlin stack trace if an exception is thrown during the build.
* `--scan`: Generates a deeply detailed web-based HTML report of the entire build process (requires accepting terms of service).
* `--offline`: Forces Gradle to build using only locally cached dependencies (perfect for working without internet).
