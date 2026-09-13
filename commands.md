# CyberGuard AI: Terminal Command Reference

## 🛠️ Build & Test
| Action | Command |
| :--- | :--- |
| **Build Debug APK** | `./gradlew assembleDebug` |
| **Run Security & AI Tests** | `./gradlew testDebugUnitTest` |
| **Clean Build** | `./gradlew clean assembleDebug` |
| **Android Lint** | `./gradlew lintDebug` |

## 🛡️ Security & Encryption
| Action | Command |
| :--- | :--- |
| **Encrypt Models** | `python3 encrypt_models.py` |
| **Check Root Status** | `adb shell getprop ro.boot.flash.locked` |
| **Monitor Model Cache** | `adb shell ls -la /data/data/com.example/no-backup/secure_models` |

## 🌐 Go Swarm Server
| Action | Command |
| :--- | :--- |
| **Tidy Dependencies** | `go mod tidy` |
| **Run Unit Tests** | `go test ./...` |
| **Build & Run** | `go build -o server_bin . && ./server_bin` |
| **Health Check** | `curl http://localhost:8080/api/health` |

## 📦 Maintenance
| Action | Command |
| :--- | :--- |
| **Gradle Sync** | `./gradlew --refresh-dependencies` |
| **Clear Local Cache** | `rm -rf .gradle build app/build` |
