# MotionSense – Android Sensor Telemetry Dashboard

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Room](https://img.shields.io/badge/Room-2.6.1-4285F4?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-FF6F00)](./docs/ARCHITECTURE.md)
[![Hardware](https://img.shields.io/badge/Verified%20Device-OnePlus%20Nord%20CE5%20(Android%2016)-F50057)](./docs/TESTING.md)

**MotionSense** is a professional native Android motion sensor telemetry application built from scratch using **Kotlin**, the **Android Sensor Framework**, **Jetpack Compose (Material 3)**, and **Room Database**. It captures real-time high-frequency smartphone sensor data, visualizes waveforms through custom Canvas rendering, and provides persistent local recording with statistical analysis and CSV export via the Storage Access Framework (SAF).

Developed as an engineering demonstration of native Android software architecture, hardware abstraction, asynchronous programming, reactive state management, and reliable local data persistence.

---

## Application Screenshots (Verified on Physical OnePlus Nord CE5)

| Live Telemetry Dashboard | Active Database Recording |
| :---: | :---: |
| ![Live Telemetry Dashboard](./docs/screenshots/dashboard_v2_idle.png) | ![Active Database Recording](./docs/screenshots/dashboard_v2_recording.png) |

| Session Recording History | Session Details & Statistics |
| :---: | :---: |
| ![Session History](./docs/screenshots/session_history.png) | ![Session Details Overview](./docs/screenshots/session_details_overview.png) |

| Historical Waveform Charts | Storage Access Framework (CSV Export) |
| :---: | :---: |
| ![Historical Waveforms](./docs/screenshots/session_details_charts.png) | ![CSV Export via SAF](./docs/screenshots/csv_export_saf.png) |

| Delete Confirmation Dialog | Empty History State |
| :---: | :---: |
| ![Delete Confirmation Dialog](./docs/screenshots/session_delete_dialog.png) | ![Empty History State](./docs/screenshots/session_history_empty.png) |

---

## Implemented Milestones

### Milestone 1 — Hardware Sensor Integration (Polished & Verified)
- **SensorManager Abstraction:** Clean reactive repository layer wrapping Android's HAL and kernel sensor drivers.
- **Bosch Sensortec BMI2xy IMU:** Physical integration of both the primary 3-axis Accelerometer (`TYPE_ACCELEROMETER`) and 3-axis Gyroscope (`TYPE_GYROSCOPE`).
- **Calibrated Units & Baselines:**
  - Accelerometer: Readings in meters per second squared ($\text{m/s}^2$). Correctly measures standard gravity vector ($\approx 9.81\ \text{m/s}^2$ on $Z$-axis when at rest).
  - Gyroscope: Readings in radians per second ($\text{rad/s}$). Zero baseline ($\approx 0.00\ \text{rad/s}$) when stationary.
  - Euclidean Vector Magnitude: Live $\sqrt{X^2 + Y^2 + Z^2}$ computation for total motion intensity.
- **Hardware Accuracy & Timestamps:** Monitors OS reporting accuracy (`High`, `Medium`, `Low`, `Unreliable`) and preserves nanosecond hardware event timestamps (`event.timestamp`).
- **Hardware Metadata Interrogation:** Live readout of driver names, vendor, power consumption (mA), maximum range, resolution, and sampling delay bounds.
- **Deterministic Leak-Free Lifecycle:** Backed by Kotlin Coroutines `callbackFlow` and `awaitClose`. Listeners are unregistered automatically when monitoring stops or when the activity is backgrounded.

### Milestone 2 — Real-Time Telemetry Dashboard (Redesigned & Polished)
- **Modern Industrial Telemetry Aesthetic:** Dark-first interface built with Material 3, high-contrast axis color coding ($X$: Red `#EF5350`, $Y$: Green `#4CAF50`, $Z$: Blue `#42A5F5`), and monospace numeric typography.
- **Decoupled 30 Hz Rate-Limiter:** Separates the high-frequency hardware event stream (~150 Hz) from the UI recomposition loop (~30 Hz) via a concurrent queue and periodic flush, guaranteeing 60fps rendering without jank.
- **Custom Canvas Waveform Visualizer:** Lightweight, zero-dependency time-series charts implemented directly with Jetpack Compose `Canvas`. Draws $X, Y, Z$ waveforms with zero-reference line, guideline grids, dynamic peak auto-scaling, and axis legends.
- **Bounded Rolling History:** Chart buffer is strictly capped at 50 data points (~2 to 3 seconds of continuous signal), ensuring constant memory usage without risk of `OutOfMemoryError`.
- **Clear Separation of Controls:**
  - **Monitoring Controls:** Start Stream, Stop Stream, and Reset live waveforms.
  - **Recording Controls:** Record Session to Database, live `REC` indicator, session duration timer, and atomic Stop & Save.

### Milestone 3A — Local Data Storage & Persistence (100% Complete)
- **Room Database Architecture:**
  - `SensorSessionEntity`: Session metadata, duration, device model, and measurement tallies.
  - `SensorReadingEntity`: Individual $X, Y, Z$, magnitude, timestamp, and accuracy rows. Configured with foreign key `CASCADE` deletion.
  - Targeted B-Tree indexes on `sessionId`, `timestampNs`, and composite `(sessionId, sensorType)` for sub-millisecond query performance.
- **High-Performance Batched Ingestion:** In-memory lock-free write buffer (`ConcurrentLinkedQueue`) buffering incoming sensor readings at 150 Hz and flushing atomic batches of 100 to SQLite on `Dispatchers.IO`. Zero frame drops during active recording.
- **Application Navigation:** Material 3 `NavigationBar` seamlessly switching between **Live Telemetry** and **Recordings** tabs.
- **Session History Screen:** Chronological listing of saved sessions with dates, durations, and sample counts. Includes delete confirmation dialogs and empty states.
- **Session Details Screen:** Full overview of recorded sessions with statistical cards (Min, Max, Average for $X, Y, Z$, and peak/average vector magnitude) and historical Canvas waveforms.
- **CSV Export via Storage Access Framework (SAF):** Exports session data to standard CSV files through the system document creation picker (`ActivityResultContracts.CreateDocument("text/csv")`) with zero dangerous storage permissions required. Verified on physical hardware producing 6,583 lines matching 6,582 recorded data points.

---

## Technology Stack

- **Language:** Kotlin 2.3.20
- **UI Framework:** Jetpack Compose (BOM 2026.03.01) + Material 3
- **Local Database:** Android Room 2.6.1 (SQLite) with KSP 2.3.13
- **Architecture:** Clean Architecture + MVVM + Unidirectional Data Flow (UDF)
- **Reactive Streams:** Kotlin Coroutines (`callbackFlow`, `StateFlow`) + Android Lifecycle Runtime Compose
- **Sensor APIs:** Android `SensorManager` + `SensorEventListener`
- **File Export:** Android Storage Access Framework (SAF)
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
TelemetryViewModel (Rate-Limiter & Rolling Buffer)
          │                                  │
          ▼                                  ▼
Jetpack Compose Dashboard        RecordingRepository (Batched Buffer)
(Canvas Waveforms & Controls)                │
                                             ▼
                                 Room Database (SQLite)
                                             │
                                             ▼
                                 Session History & CSV Export (SAF)
```

For complete technical specifications:
- [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)
- [docs/DATABASE_DESIGN.md](./docs/DATABASE_DESIGN.md)
- [docs/SENSOR_INTEGRATION.md](./docs/SENSOR_INTEGRATION.md)

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

For complete setup commands and database inspection tips, see [docs/DEVELOPMENT.md](./docs/DEVELOPMENT.md).

---

## Testing & Hardware Verification

Verified on physical **OnePlus Nord CE5** (CPH2717IN, Android 16 API 36):
- **Gravity Baseline Test:** Stationary reading confirmed at $Z \approx 9.81\ \text{m/s}^2$ ($1\text{g}$).
- **Rotation Response Test:** Angular velocity response confirmed across yaw, pitch, and roll axes.
- **Continuous Stream Stress Test:** Monitored over **39,400+ consecutive hardware events** at **150.4 Hz** with zero memory leaks, crashes, or dropped frames.
- **Database Persistence Benchmark:** Streamed and persisted **6,582 records** in batches of 100 on `Dispatchers.IO` with zero lost records.
- **Storage Access Framework Test:** Exported complete session to CSV (`541 KB`), verified row count and timestamps.
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
  - [x] Material 3 Dark-first industrial telemetry dashboard
  - [x] Decoupled 30 Hz Compose rate-limiting
  - [x] Distinct Monitoring vs Recording controls
  - [x] Custom Canvas rolling waveform visualizer
  - [x] Physical device hardware testing & verification
- [x] **Milestone 3A — Local Data Storage & Persistence**
  - [x] Room Database with `SensorSessionEntity` and `SensorReadingEntity`
  - [x] High-performance batched write pipeline (100 items / 500ms)
  - [x] Bottom Navigation (Live Telemetry & Recordings)
  - [x] Session History screen with confirmation delete dialogs
  - [x] Session Details screen with statistical analysis (Min, Max, Avg, Peak)
  - [x] Historical waveform visualization plotted from stored database readings
  - [x] CSV export via Android Storage Access Framework (SAF)
- [ ] **Milestone 3B — External Hardware & Cloud (PENDING)**
  - [ ] *Pending: ESP32 wireless telemetry sync*
  - [ ] *Pending: Bluetooth Low Energy (BLE) peripheral streaming & discovery*
  - [ ] *Pending: Cloud synchronization and backend endpoints*

---

## License

This project is licensed under the Apache License 2.0.
