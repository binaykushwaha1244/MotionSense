# Changelog

All notable changes to **MotionSense** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2026-11-18

### Completed Milestones
- **Milestone 1 — Sensor Integration (Reviewed, Polished & Verified)**
- **Milestone 2 — Real-Time Telemetry Dashboard (Redesigned & Polished)**
- **Milestone 3A — Local Data Storage & Persistence (100% Complete & Verified)**

### Added
- **Room Database Architecture (Milestone 3A):**
  - Integrated Kotlin Symbol Processing (`KSP 2.3.13`) and Android Room (`2.6.1`).
  - Implemented `SensorSessionEntity` tracking session metadata, device specs, duration, and sample counts.
  - Implemented `SensorReadingEntity` with foreign key CASCADE deletion to sessions, and B-Tree indexes on `sessionId`, `timestampNs`, and composite `(sessionId, sensorType)`.
  - Created `SensorSessionDao` and `SensorReadingDao` with reactive Flow queries and atomic batch operations.
  - Implemented `MotionSenseDatabase` singleton with destructive migration fallback for development agility.
- **Batched Persistence Pipeline:**
  - High-performance in-memory write buffer (`ConcurrentLinkedQueue`) decoupling high-frequency sensor acquisition (~150 Hz) from SQLite disk writes.
  - Two-tier flush triggers: threshold-based (100 items), periodic ticker (500ms), and guaranteed drain upon session completion.
- **Recording Controls & Workflow:**
  - Clear separation between Live Telemetry Streaming and Local Database Recording.
  - "Record Session to Database" button with custom naming dialog (`StartRecordingDialog`).
  - Active recording strip with pulsing Red `REC` indicator, live duration timer, and real-time saved sample counter.
  - "Stop & Save" control ensuring atomic session finalization.
- **Application Navigation & Architecture:**
  - Top-level `MotionSenseNavHost` with Material 3 `NavigationBar` switching between "Live Telemetry" and "Recordings".
- **Session History Screen:**
  - Chronological list of saved sessions with date/time, duration, sample counts, and status pills.
  - Swipe/tap actions to view details, export to CSV, or delete.
  - Material 3 `AlertDialog` for delete confirmation to prevent accidental loss.
  - Clean empty state with illustrated icon and call-to-action.
- **Session Details Screen:**
  - Session overview card with timestamps, device model, and measurement tallies.
  - Statistical Analysis Cards for Accelerometer and Gyroscope computing Min, Max, Average for $X, Y, Z$, and peak/average vector magnitude.
  - Historical Waveform Visualizations plotted directly from stored database readings (downsampled dynamically for smooth Canvas rendering).
  - Prominent "Export Session to CSV" button.
- **CSV Export via Storage Access Framework (SAF):**
  - Integrated `ActivityResultContracts.CreateDocument("text/csv")` for zero-permission file saving.
  - Streams formatted records directly to the destination Uri output stream without memory bloat.
  - Verified on physical hardware producing 6,583 lines matching 6,582 recorded data points.
- **Documentation Suite:**
  - Created `docs/DATABASE_DESIGN.md` detailing Room architecture, entity schemas, relationships, indexing, and batching.
  - Captured and embedded high-resolution screenshots of the redesigned dashboard, recording state, history list, details view, and SAF file picker.

---

## [1.0.0] - 2026-11-18

### Initial Release
- Milestone 1: SensorManager integration with Bosch BMI2xy accelerometer and gyroscope.
- Milestone 2: Real-time telemetry dashboard with Compose Canvas waveforms.

---

## [PENDING] - Milestone 3B

The following external hardware features remain strictly deferred:
- ⏳ ESP32 firmware and serial telemetry
- ⏳ Bluetooth Low Energy (BLE) peripheral streaming and discovery
- ⏳ Cloud synchronization and backend endpoints
