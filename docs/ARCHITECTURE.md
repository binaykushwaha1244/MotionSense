# MotionSense Architecture & Engineering Blueprint

This document details the software architecture, package hierarchy, state flow, and concurrency models implemented in **MotionSense – Android Sensor Telemetry Dashboard**.

---

## 1. Architectural Overview

MotionSense is engineered according to **Clean Architecture** principles, adopting the **Model-View-ViewModel (MVVM)** pattern combined with **Unidirectional Data Flow (UDF)** and reactive asynchronous streams powered by **Kotlin Coroutines and Flow**.

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
└───────────────────────────┬────────────────────────────┘
                            │ Flow<SensorDataPoint>
                            ▼
┌────────────────────────────────────────────────────────┐
│                  TelemetryViewModel                    │
│           com.motionsense.app.viewmodel                │
│   • High-frequency event buffer ingestion              │
│   • Decoupled 30 Hz UI refresh rate-limiter            │
│   • 50-point rolling history management                │
│   • Session duration timer & sample rate estimation    │
└───────────────────────────┬────────────────────────────┘
                            │ StateFlow<DashboardUiState>
                            ▼
┌────────────────────────────────────────────────────────┐
│                Presentation Layer                      │
│         Jetpack Compose + Material 3 + Canvas          │
│   • collectAsStateWithLifecycle()                      │
│   • Hardware-accelerated Canvas waveforms              │
│   • Start / Stop / Reset telemetry controls            │
└────────────────────────────────────────────────────────┘
```

---

## 2. Package Organization

```
com.motionsense.app/
│
├── data/
│   ├── model/
│   │   ├── SensorType.kt            // Enum defining ACCELEROMETER and GYROSCOPE
│   │   ├── SensorAccuracy.kt        // Enum mapping SensorManager.SENSOR_STATUS_*
│   │   ├── SensorMetadata.kt        // Hardware specifications (vendor, power, range, resolution)
│   │   └── SensorDataPoint.kt       // Immutable 3-axis telemetry point (x, y, z, timestamp, magnitude)
│   └── repository/
│       ├── SensorRepository.kt      // Interface contract for hardware access and reactive streams
│       └── SensorRepositoryImpl.kt  // Concrete Android SensorManager implementation using callbackFlow
│
├── domain/
│   └── model/
│       └── MonitoringState.kt       // High-level lifecycle state (IDLE, MONITORING, PAUSED)
│
├── presentation/
│   ├── dashboard/
│   │   ├── DashboardScreen.kt       // Top-level scaffold orchestrating header, cards, and controls
│   │   ├── DashboardUiState.kt      // Immutable UI state representation consumed by Compose
│   │   └── components/
│   │       ├── TelemetryHeader.kt   // App branding, device platform badge, session statistics
│   │       ├── MonitoringControls.kt// Start/Stop monitoring and Reset action buttons
│   │       ├── SensorCard.kt        // Detailed card displaying live axes, magnitude, metadata, and chart
│   │       ├── TelemetryChart.kt    // Custom Compose Canvas time-series waveform visualizer
│   │       └── AxisMetricChip.kt    // Monospace numeric chip styled by axis (Red, Green, Blue)
│   └── theme/
│       ├── Color.kt                 // Telemetry dark-first palette and axis colors
│       ├── Theme.kt                 // Material 3 DynamicColorScheme and dark/light themes
│       └── Type.kt                  // Monospace typography for numeric telemetry display
│
├── viewmodel/
│   └── TelemetryViewModel.kt        // Reactive state management, rate-limiting, and lifecycle coordination
│
└── MainActivity.kt                  // Single Activity entrypoint with edge-to-edge support
```

---

## 3. Component Responsibilities

### Data Layer (`data/`)
- **`SensorRepository`**: Defines the abstraction layer over Android hardware sensors. Decouples Android framework dependencies from higher-level business logic.
- **`SensorRepositoryImpl`**: Direct consumer of `android.hardware.SensorManager`. Transforms callback-based hardware interrupts (`SensorEventListener`) into cold Kotlin `Flow<SensorDataPoint>` via `callbackFlow`. Crucially, calls `awaitClose` to guarantee that hardware listeners are unregistered whenever collection is cancelled.
- **`SensorDataPoint`**: Immutable data class holding $(X, Y, Z)$ float readings, nanosecond timestamp (`event.timestamp`), and sensor accuracy. Calculates vector magnitude $\sqrt{X^2 + Y^2 + Z^2}$.

### ViewModel Layer (`viewmodel/`)
- **`TelemetryViewModel`**:
  - Exposes an immutable `StateFlow<DashboardUiState>` to the presentation layer.
  - Manages background coroutine jobs (`accelerometerJob`, `gyroscopeJob`, `timerJob`, `uiUpdateJob`).
  - **Decoupling Rate-Limiter**: Collects raw hardware events into concurrent queues and periodically flushes state to the UI at ~30 Hz (33ms interval). This isolates the Jetpack Compose rendering pipeline from high-frequency sensor updates (~150 Hz), eliminating frame drops and UI jank.
  - Maintains a bounded rolling buffer of the last 50 data points for continuous waveform rendering.
  - Computes real-time sample rate ($\text{Hz} = \frac{\Delta\text{samples}}{\Delta t}$) and session duration.

### Presentation Layer (`presentation/`)
- **`DashboardScreen`**: Pure declarative Jetpack Compose UI. Uses `collectAsStateWithLifecycle()` to safely consume the ViewModel's `StateFlow`.
- **`TelemetryChart`**: Custom `Canvas` component that plots continuous waveforms for $X$ (Red), $Y$ (Green), and $Z$ (Blue) without external third-party dependencies.
- **`SensorCard`**: Displays current axis values, vector magnitude, accuracy, hardware metadata, and embedded waveforms.

---

## 4. Concurrency & Performance Strategy

1. **Cold Reactive Streams**: Sensors are only polled when an active subscriber collects the flow. When monitoring is stopped, all coroutine jobs are cancelled, unregistering listeners at the OS level.
2. **Backpressure Prevention**: Uses `trySend()` inside `callbackFlow` to drop saturated buffer events rather than blocking Android's main or sensor dispatch threads.
3. **Bounded Memory Footprint**: Rolling chart history is capped at `MAX_CHART_HISTORY_POINTS = 50`. Old items are discarded in $O(1)$ time, guaranteeing that memory consumption remains constant regardless of monitoring session duration.
4. **Lifecycle Coordination**: The Activity triggers `onLifecyclePause()` on backgrounding, placing telemetry in `PAUSED` state and releasing hardware listeners. Upon foregrounding (`onLifecycleResume()`), telemetry resumes seamlessly.
