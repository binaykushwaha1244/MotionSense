# Changelog

All notable changes to **MotionSense** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-11-18

### Completed Milestones
- **Milestone 1 — Sensor Integration (100% Complete & Verified)**
- **Milestone 2 — Real-Time Telemetry Dashboard (100% Complete & Verified)**

### Added
- **Hardware Sensor Integration:**
  - `SensorManager` integration via Kotlin Coroutines `callbackFlow`.
  - Accelerometer integration (`TYPE_ACCELEROMETER`, Bosch BMI2xy) reading $X, Y, Z$ in $\text{m/s}^2$ with vector magnitude.
  - Gyroscope integration (`TYPE_GYROSCOPE`, Bosch BMI2xy) reading $X, Y, Z$ in $\text{rad/s}$ with angular velocity magnitude.
  - Hardware accuracy tracking (`SensorAccuracy`) mapping `SensorManager.SENSOR_STATUS_*`.
  - Hardware metadata interrogation (driver name, vendor, power in mA, max range, resolution, sampling delay range, reporting mode).
  - Nanosecond hardware timestamps (`event.timestamp`) captured and preserved.
  - Lifecycle-aware listener registration and deterministic unregistration via `awaitClose`.
- **Telemetry Dashboard Presentation:**
  - Top header displaying MotionSense branding, system status pill (`ACTIVE` / `IDLE` / `PAUSED`), and OnePlus device/OS platform badge.
  - Live session metrics strip: formatted session duration (`mm:ss`), live sample rate ($\text{Hz}$), and total sample counter.
  - Monitoring action controls: "Start Monitoring" / "Stop Monitoring" button with play/stop icons, and "Reset Session" button.
  - Accelerometer telemetry card with 3-axis chips ($X$: Red, $Y$: Green, $Z$: Blue), vector magnitude, accuracy badge, and expandable hardware specifications drawer.
  - Gyroscope telemetry card with 3-axis chips, angular velocity magnitude, accuracy badge, and expandable hardware specifications drawer.
  - Custom hardware-accelerated time-series waveform charts implemented via Jetpack Compose `Canvas` with bounded rolling buffers (50 points), zero reference line, dynamic scaling, and axis legends.
  - High-performance decoupled rate-limiter in `TelemetryViewModel` separating raw hardware event frequency (~150 Hz) from UI recomposition (~30 Hz) to eliminate frame drops.
  - Dark-first engineering aesthetic with Material 3 styling, monospace numeric typography, and high contrast accents.
- **Testing & Verification:**
  - Physical testing on OnePlus Nord CE5 (Android 16 API 36).
  - Automated unit test suite (`SensorDataPointTest`, `DashboardUiStateTest`).
  - OS-level listener verification via `adb shell dumpsys sensorservice`.
  - Comprehensive documentation suite: `README.md`, `ARCHITECTURE.md`, `SENSOR_INTEGRATION.md`, `DEVELOPMENT.md`, `TESTING.md`.
  - High-resolution physical hardware screenshots embedded in documentation.

### Deferred (Pending)
- **Milestone 3 — Data Persistence & External Hardware (PENDING):**
  - Room Database integration (Deferred).
  - CSV telemetry export (Deferred).
  - ESP32 communication (Deferred).
  - Bluetooth Low Energy (BLE) peripheral streaming (Deferred).
  - Backend/Cloud services (Deferred).
