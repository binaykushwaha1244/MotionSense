# MotionSense – Android Sensor Telemetry Dashboard

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-FF6F00)](./docs/ARCHITECTURE.md)
[![Hardware](https://img.shields.io/badge/Verified%20Device-OnePlus%20Nord%20CE5%20(Android%2016)-F50057)](./docs/TESTING.md)

**MotionSense** is a native Android smartphone motion telemetry application built from scratch with **Kotlin**, the **Android Sensor Framework**, and **Jetpack Compose**. It captures real-time high-frequency hardware sensor data and visualizes it through an engineering telemetry dashboard featuring hardware-accelerated time-series waveforms.

Developed as a demonstration of native Android software architecture, hardware abstraction, asynchronous programming, reactive streams, and lifecycle safety.

---

## Application Screenshots (Physical Device: OnePlus Nord CE5)

| Live Dashboard & Accelerometer | Real-Time Gyroscope Waveforms |
| :---: | :---: |
| ![Live Telemetry Dashboard](./docs/screenshots/dashboard_live.png) | ![Gyroscope Telemetry](./docs/screenshots/gyroscope_waveform.png) |

| Hardware Specifications Drawer | Standby / Stopped State |
| :---: | :---: |
| ![Hardware Specifications Drawer](./docs/screenshots/hardware_specs.png) | ![Dashboard Standby State](./docs/screenshots/dashboard_stopped.png) |

---

## Key Features

### Milestone 1 — Hardware Sensor Integration (Completed & Verified)
- **SensorManager Abstraction:** Clean reactive repository layer wrapping Android's HAL and kernel sensor drivers.
- **Bosch Sensortec BMI2xy IMU:** Physical integration of both the primary 3-axis Accelerometer (`TYPE_ACCELEROMETER`) and 3-axis Gyroscope (`TYPE_GYROSCOPE`).
- **Precision Units & Baselines:**
  - Accelerometer: Readings in meters per second squared ($\text{m/s}^2$). Correctly measures standard gravity vector ($\approx 9.81\ \text{m/s}^2$ on $Z$-axis when at rest).
  - Gyroscope: Readings in radians per second ($\text{rad/s}$). Zero baseline ($\approx 0.00\ \text{rad/s}$) when stationary.
  - Euclidean Vector Magnitude: Live $\sqrt{X^2 + Y^2 + Z^2}$ computation for total motion intensity.
- **Hardware Accuracy & Timestamps:** Monitors OS reporting accuracy (`High`, `Medium`, `Low`, `Unreliable`) and preserves nanosecond hardware event timestamps.
- **Hardware Metadata Interrogation:** Live readout of driver names, vendor, power consumption (mA), maximum range, resolution, and sampling delay bounds.
- **Deterministic Leak-Free Lifecycle:** Backed by Kotlin Coroutines `callbackFlow` and `awaitClose`. Listeners are unregistered automatically when monitoring stops or when the activity is backgrounded.

### Milestone 2 — Real-Time Telemetry Dashboard (Completed & Verified)
- **Modern Telemetry Aesthetic:** Dark-first interface built with Material 3, high-contrast axis color coding ($X$: Red `#EF5350`, $Y$: Green `#4CAF50`, $Z$: Blue `#42A5F5`), and monospace numeric typography.
- **Decoupled 30 Hz Rate-Limiter:** Separates the high-frequency hardware event stream (~150 Hz) from the UI recomposition loop (~30 Hz) via a concurrent queue and periodic flush, guaranteeing 60fps rendering without jank.
- **Custom Canvas Waveform Visualizer:** Lightweight, zero-dependency time-series charts implemented directly with Jetpack Compose `Canvas`. Draws $X, Y, Z$ waveforms with zero-reference line, guideline grids, dynamic peak auto-scaling, and axis legends.
- **Bounded Rolling History:** Chart buffer is strictly capped at 50 data points (~2 to 3 seconds of continuous signal), ensuring constant memory usage without risk of `OutOfMemoryError`.
- **Monitoring Session Controls:** Start, Stop, and Reset action controls with active duration timer and live sampling rate calculation ($\text{Hz}$).

---

## Technology Stack

- **Language:** Kotlin 2.3.20
- **UI Framework:** Jetpack Compose (BOM 2026.03.01) + Material 3
- **Architecture:** Clean Architecture + MVVM + Unidirectional Data Flow (UDF)
- **Reactive Streams:** Kotlin Coroutines (`callbackFlow`, `StateFlow`) + Android Lifecycle Runtime Compose
- **Sensor APIs:** Android `SensorManager` + `SensorEventListener`
- **Build System:** Gradle 9.1.0 with Android Gradle Plugin (AGP) 9.0.1
- **Testing:** JUnit 4 + Coroutines Test + AndroidX Test Runner

---

## Architecture Overview

```
Physical Hardware Sensors (Bosch BMI2xy)
          │  Hardware SensorEvents (~150 Hz)
          ▼
SensorRepository (callbackFlow + awaitClose)
          │  Flow<SensorDataPoint>
          ▼
TelemetryViewModel (Rate-Limiter & 50-point Rolling Buffer)
          │  StateFlow<DashboardUiState> (~30 Hz)
          ▼
Jetpack Compose Dashboard (Canvas Waveforms & Material 3 UI)
```

For complete technical specifications, see [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md).

---

## Supported Physical Sensors (OnePlus Nord CE5)

| Sensor | Driver Name | Vendor | Type | Units | Max Range | Resolution |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Accelerometer** | `bmi2xy acc` | Bosch Sensortec | `TYPE_ACCELEROMETER` | $\text{m/s}^2$ | $78.45\ \text{m/s}^2$ | $0.0012\ \text{m/s}^2$ |
| **Gyroscope** | `bmi2xy gyro` | Bosch Sensortec | `TYPE_GYROSCOPE` | $\text{rad/s}$ | $34.91\ \text{rad/s}$ | $0.0010\ \text{rad/s}$ |

Detailed sensor documentation is available in [docs/SENSOR_INTEGRATION.md](./docs/SENSOR_INTEGRATION.md).

---

## Setup & Build Instructions

### Prerequisites
- Windows 10/11, macOS, or Linux
- Android Studio 2026.2.2 or higher
- Android SDK with API 36/37 (`compileSdk = 37`, `minSdk = 26`)
- Java JDK 17+ (or bundled Android Studio JBR 25)

### Build Commands
```powershell
# Run unit tests
.\gradlew.bat testDebugUnitTest

# Build debug APK
.\gradlew.bat assembleDebug
```

### Installation via ADB
```powershell
# Verify connected device
adb devices -l

# Install to physical device
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Launch MotionSense
adb shell am start -n com.motionsense.app/.MainActivity
```

For complete setup commands and debugging tips, see [docs/DEVELOPMENT.md](./docs/DEVELOPMENT.md).

---

## Testing & Hardware Verification

Verified on physical **OnePlus Nord CE5** (CPH2717IN, Android 16 API 36):
- **Gravity Baseline Test:** Stationary reading confirmed at $Z \approx 9.81\ \text{m/s}^2$ ($1\text{g}$).
- **Rotation Response Test:** Angular velocity response confirmed across yaw, pitch, and roll axes.
- **Continuous Stream Stress Test:** Monitored over **39,400+ consecutive hardware events** at **150.4 Hz** with zero memory leaks, crashes, or dropped frames.
- **Lifecycle & Listener Teardown Test:** Verified through `adb shell dumpsys sensorservice` that all sensor listeners unregister immediately when backgrounded or stopped.

Comprehensive test protocols and logs are recorded in [docs/TESTING.md](./docs/TESTING.md).

---

## Project Roadmap

- [x] **Milestone 1 — Sensor Integration**
  - [x] `SensorManager` & `SensorEventListener` implementation
  - [x] Accelerometer integration ($X, Y, Z$, magnitude, timestamps)
  - [x] Gyroscope integration ($X, Y, Z$, magnitude, timestamps)
  - [x] Hardware metadata & accuracy state
  - [x] Leak-free lifecycle management via `callbackFlow`
- [x] **Milestone 2 — Real-Time Telemetry Dashboard**
  - [x] Material 3 Dark-first engineering telemetry dashboard
  - [x] Decoupled 30 Hz Compose rate-limiting
  - [x] Start / Stop / Reset monitoring controls
  - [x] Custom Canvas rolling waveform visualizer
  - [x] Physical device hardware testing & verification
- [ ] **Milestone 3 — Data Persistence & External Hardware (PENDING)**
  - [ ] *Pending: Room Database telemetry session storage*
  - [ ] *Pending: CSV / JSON data export*
  - [ ] *Pending: ESP32 wireless telemetry sync*
  - [ ] *Pending: Bluetooth Low Energy (BLE) peripheral streaming*
  - [ ] *Pending: Cloud synchronization and backend endpoints*

---

## License

This project is licensed under the Apache License 2.0.
