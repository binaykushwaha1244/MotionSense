# MotionSense Development & Toolchain Guide

This document describes the development environment, SDK setup, build tools, database debugging, and terminal commands required to compile, test, and deploy **MotionSense**.

---

## 1. Prerequisites & Toolchain

| Tool / Environment | Version / Path |
| :--- | :--- |
| **Operating System** | Windows 10 / Windows 11 |
| **IDE** | Android Studio 2026.2.2+ |
| **Java / JDK** | OpenJDK 25.0.4 (`C:\Program Files\Android\Android Studio\jbr`) |
| **Android SDK** | `C:\Users\binay\AppData\Local\Android\Sdk` |
| **Compile SDK / Target SDK** | `compileSdk = 37`, `targetSdk = 37` (Android 16 API 36/37) |
| **Min SDK** | `minSdk = 26` (Android 8.0 Oreo) |
| **Build System** | Gradle 9.1.0 (`gradlew.bat`) |
| **Android Gradle Plugin (AGP)** | 9.0.1 |
| **Kotlin Version** | 2.3.20 (with Compose Compiler & KSP 2.3.13) |
| **Local Database Engine** | Android Room 2.6.1 (SQLite) |

---

## 2. Project Setup & Local Configuration

Clone the repository and set your local SDK location in `local.properties`:

```properties
sdk.dir=C\:\\Users\\binay\\AppData\\Local\\Android\\Sdk
```

Ensure `gradle.properties` includes the required flags:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
android.suppressUnsupportedCompileSdk=37.0
```

---

## 3. Build & Test Commands

### Run Unit Tests
```powershell
.\gradlew.bat testDebugUnitTest
```

### Build Debug APK
```powershell
.\gradlew.bat assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Clean Build
```powershell
.\gradlew.bat clean assembleDebug
```

---

## 4. Physical Device Deployment (via ADB)

### 1. Verify Device Connection
```powershell
adb devices -l
```

### 2. Install Debug APK
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 3. Launch MotionSense
```powershell
adb shell am start -n com.motionsense.app/.MainActivity
```

### 4. Inspect Live Telemetry Logcat
```powershell
adb logcat -s AndroidRuntime:E MotionSense:* TelemetryViewModel:*
```

### 5. Inspect Room SQLite Database On-Device
```powershell
# Open interactive SQLite shell
adb shell "run-as com.motionsense.app sqlite3 databases/motionsense.db 'SELECT * FROM sensor_sessions;'"

# Count total persisted readings
adb shell "run-as com.motionsense.app sqlite3 databases/motionsense.db 'SELECT COUNT(*) FROM sensor_readings;'"
```

### 6. Verify Exported CSV Files
```powershell
adb shell ls -la /sdcard/Download/MotionSense_*.csv
adb shell head -n 5 /sdcard/Download/MotionSense_*.csv
```
