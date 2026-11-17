# MotionSense Architecture & Engineering Blueprint

This document details the software architecture, package hierarchy, state flow, database architecture, and concurrency models implemented in **MotionSense – Android Sensor Telemetry Dashboard**.

---

## 1. Architectural Overview

MotionSense is engineered according to **Clean Architecture** principles, adopting the **Model-View-ViewModel (MVVM)** pattern combined with **Unidirectional Data Flow (UDF)**, reactive asynchronous streams powered by **Kotlin Coroutines and Flow**, and persistent local storage powered by **Android Room (SQLite)**.

```
┌────────────────────────────────────────────────────────┐
│               Physical Hardware Sensors                │
│    (Bosch Sensortec BMI2xy Accelerometer & Gyroscope)  │
└───────────────────────────┬────────────────────────────┘
                            │ Hardware SensorEvents (~150 Hz)
                            ▼
┌────────────────────────────────────────────────────────┐
│                 SensorManager & Driver                 │
│         Android Framework Hardware Abstraction         │
└───────────────────────────┬────────────────────────────┘
                            │ callbackFlow + awaitClose
                            ▼
┌────────────────────────────────────────────────────────┐
│                   SensorRepository                     │
│         com.motionsense.app.data.repository            │
│   • Lifecycle-safe listener management                 │
│   • Hardware metadata interrogation                    │
│   • Zero memory leaks guarantee                        │
└──────────────┬────────────────────────────┬────────────┘
               │ Flow<SensorDataPoint>      │ Live Stream
               ▼                            ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│     TelemetryViewModel       │ │     RecordingRepository      │
│ • 30 Hz UI Decoupled Loop    │ │ • In-Memory Buffer (Concurrent│
│ • Rolling Chart Buffer (50)  │ │ • Batched Insert (100 items) │
│ • Active Duration Timer      │ │ • MotionSenseDatabase (Room) │
└──────────────┬───────────────┘ └──────────────┬───────────────┘
               │ StateFlow                      │ Flow<List<Session>>
               ▼                                ▼
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│         Jetpack Compose + Material 3 + Canvas          │
│   • Bottom Navigation (Live Telemetry & Recordings)    │
│   • Telemetry Dashboard Screen                         │
│   • Session History Screen & Confirmation Dialog       │
│   • Session Details Screen & Historical Waveforms      │
│   • Android Storage Access Framework (SAF) CSV Export  │
└────────────────────────────────────────────────────────┘
```

---

## 2. Package Organization

```
com.motionsense.app/
│
├── data/
│   ├── local/
│   │   ├── MotionSenseDatabase.kt   // Room database definition
│   │   ├── dao/
│   │   │   ├── SensorSessionDao.kt  // Session CRUD and reactive Flow queries
│   │   │   └── SensorReadingDao.kt  // Batched measurement insertions and analytics
│   │   └── entity/
│   │       ├── SensorSessionEntity.kt // Recording session schema with stats
│   │       └── SensorReadingEntity.kt // Measurement schema with CASCADE delete & indexes
│   ├── model/
│   │   ├── SensorType.kt            // Enum defining ACCELEROMETER and GYROSCOPE
│   │   ├── SensorAccuracy.kt        // Enum mapping SensorManager.SENSOR_STATUS_*
│   │   ├── SensorMetadata.kt        // Hardware specs (vendor, power, range, resolution)
│   │   └── SensorDataPoint.kt       // Immutable 3-axis telemetry point (x, y, z, timestamp, magnitude)
│   └── repository/
│       ├── SensorRepository.kt      // Hardware sensor abstraction
│       ├── SensorRepositoryImpl.kt  // SensorManager wrapper using callbackFlow
│       ├── RecordingRepository.kt   // Local database persistence & CSV export contract
│       └── RecordingRepositoryImpl.kt // Batched buffer manager and CSV writer
│
├── domain/
│   └── model/
│       ├── MonitoringState.kt       // High-level lifecycle state (IDLE, MONITORING, PAUSED)
│       ├── SensorStatistics.kt      // Computed statistical metrics (min, max, avg, peak)
│       └── SessionDetails.kt        // Aggregated session information with analytics
│
├── presentation/
│   ├── dashboard/
│   │   ├── DashboardScreen.kt       // Primary Compose telemetry dashboard
│   │   ├── DashboardUiState.kt      // StateFlow state model for telemetry & recording
│   │   └── components/
│   │       ├── TelemetryHeader.kt   // App branding, device platform badge, session stats
│   │       ├── MonitoringControls.kt// Dual controls: Live Streaming & Database Recording
│   │       ├── StartRecordingDialog.kt // Dialog to customize recording session name
│   │       ├── SensorCard.kt        // Live axis readings, magnitude, metadata, and chart
│   │       ├── TelemetryChart.kt    // Custom Compose Canvas time-series waveform visualizer
│   │       └── AxisMetricChip.kt    // Monospace numeric chip styled by axis (Red, Green, Blue)
│   ├── history/
│   │   ├── SessionHistoryScreen.kt  // List of recorded sessions with export & delete
│   │   └── SessionDetailsScreen.kt  // Detailed metrics, historical charts, and CSV export
│   ├── navigation/
│   │   └── MotionSenseNavHost.kt    // Top-level navigation host with Material 3 NavigationBar
│   └── theme/
│       ├── Color.kt                 // Telemetry dark-first palette and axis colors
│       ├── Theme.kt                 // Material 3 DynamicColorScheme and dark/light themes
│       └── Type.kt                  // Monospace typography for numeric telemetry display
│
├── viewmodel/
│   ├── TelemetryViewModel.kt        // Telemetry state management, rate-limiting, and recording
│   └── SessionHistoryViewModel.kt   // History state, session details, and CSV export triggers
│
└── MainActivity.kt                  // Single Activity entrypoint with edge-to-edge support
```

---

## 3. Component Responsibilities

### Data Layer (`data/`)
- **`SensorRepository`**: Hardware abstraction layer querying `SensorManager`. Converts hardware callbacks to cold Kotlin `Flow<SensorDataPoint>` via `callbackFlow` with leak-free `awaitClose`.
- **`RecordingRepository`**: Buffers incoming sensor events in memory and performs atomic batched insertions into Room on `Dispatchers.IO`. Streams CSV exports directly to SAF `OutputStream`.
- **`MotionSenseDatabase`**: Room database housing `sensor_sessions` and `sensor_readings` with foreign key CASCADE deletion.

### ViewModel Layer (`viewmodel/`)
- **`TelemetryViewModel`**:
  - Exposes `StateFlow<DashboardUiState>` for live telemetry.
  - **Decoupled 30 Hz Rate-Limiter**: Collects high-frequency hardware events into concurrent queues and flushes UI state every 33ms, isolating Compose from 150 Hz hardware interrupts.
  - Dispatches readings to `RecordingRepository` when actively recording.
  - Coordinates Activity lifecycle events (`onPause`, `onResume`).
- **`SessionHistoryViewModel`**:
  - Exposes `StateFlow<List<SensorSessionEntity>>` backed directly by Room Flow.
  - Manages session details loading, analytical calculations, and deletion.

### Presentation Layer (`presentation/`)
- **`MotionSenseNavHost`**: Top-level scaffold with Material 3 `NavigationBar` switching between Live Telemetry and Recordings, handling SAF document creation.
- **`DashboardScreen`**: Industrial telemetry dashboard with distinct stream and record controls.
- **`SessionHistoryScreen`**: Displays saved sessions with swipe/tap actions and confirmation dialogs.
- **`SessionDetailsScreen`**: Renders analytical metrics and historical waveforms plotted from stored database rows.
