# MotionSense Development & Toolchain Guide

This document describes the development environment, SDK setup, build tools, and terminal commands required to compile, test, and deploy **MotionSense**.

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
| **Kotlin Version** | 2.3.20 (with Compose Compiler plugin) |

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
Expected output:
```text
List of devices attached
8XG6USAIKNKZXCU4       device product:CPH2717IN model:CPH2717 device:OP613BL1 transport_id:3
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

### 5. Verify Sensor Registrations in Android OS
```powershell
adb shell dumpsys sensorservice | Select-String -Pattern "com.motionsense.app"
```

### 6. Capture Telemetry Screenshot from Device
```powershell
adb exec-out screencap -p > docs/screenshots/dashboard_live.png
```
