# MotionSense Hardware & Automated Testing Report

This document records the testing procedure, physical test bench, baseline verifications, stress tests, and lifecycle validation conducted on physical Android hardware for **Milestone 1**, **Milestone 2**, and **Milestone 3A**.

---

## 1. Physical Test Bench Environment

| Parameter | Specification |
| :--- | :--- |
| **Physical Device** | OnePlus Nord CE5 / CPH2717 (Product: `CPH2717IN`) |
| **Android Version** | Android 16 (API Level 36 / 37) |
| **Processor Architecture** | ARM64-v8a |
| **Accelerometer Hardware** | Bosch Sensortec `bmi2xy acc` (Driver v1) |
| **Gyroscope Hardware** | Bosch Sensortec `bmi2xy gyro` (Driver v1) |
| **Database Engine** | SQLite 3 via Android Room 2.6.1 |
| **Connectivity** | High-speed USB Debugging (Authorized) |
| **Testing Date** | November 18, 2026 |

---

## 2. Sensor Verification Checklist (Milestone 1)

| Test Case | Description | Verification Method | Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| **M1-01** | Accelerometer Detection | `SensorRepositoryImpl.isSensorAvailable(ACCELEROMETER)` | Detected Bosch BMI2xy | **PASSED** |
| **M1-02** | Gyroscope Detection | `SensorRepositoryImpl.isSensorAvailable(GYROSCOPE)` | Detected Bosch BMI2xy | **PASSED** |
| **M1-03** | Stationary Gravity Baseline | Phone resting flat face-up on table | $Z = +9.81\ \text{m/s}^2$, $X \approx 0.0$, $Y \approx 0.0$ | **PASSED** |
| **M1-04** | Stationary Gyroscope Baseline | Phone stationary on table | $X, Y, Z = 0.00\ \text{rad/s}$ ($\pm 0.02$) | **PASSED** |
| **M1-05** | Accelerometer Dynamic Response | Shaking and tilting device | Dynamic $X, Y, Z$ waveforms between $-20$ and $+25\ \text{m/s}^2$ | **PASSED** |
| **M1-06** | Gyroscope Rotation Response | Rapid yaw/pitch/roll rotations | Instantaneous angular velocity peaks up to $15.4\ \text{rad/s}$ | **PASSED** |
| **M1-07** | Sensor Metadata Extraction | Reading vendor, power, range, and resolution | Acc: $78.45\ \text{m/s}^2$, Gyro: $34.91\ \text{rad/s}$ | **PASSED** |
| **M1-08** | Accuracy Status Reporting | Mapping `SensorManager.SENSOR_STATUS_*` | Reports `Accuracy: High` | **PASSED** |
| **M1-09** | Hardware Timestamps | Verify nanoseconds since boot | Monotonically increasing hardware timestamps | **PASSED** |
| **M1-10** | Listener Registration & Teardown | Inspect OS registrations via `dumpsys` | Clean `+` on start, clean `-` on stop | **PASSED** |

---

## 3. Dashboard & Real-Time Visualization Checklist (Milestone 2)

| Test Case | Description | Verification Method | Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| **M2-01** | UI Launch State | App launch prior to monitoring | Displays `IDLE`, Standby badges, `Awaiting telemetry stream...` | **PASSED** |
| **M2-02** | Start Monitoring Toggle | Tap "Start Stream" button | Changes to `Stop Stream`, pill turns green `ACTIVE` | **PASSED** |
| **M2-03** | Stop Monitoring Toggle | Tap "Stop Stream" button | Changes to `Start Stream`, pill turns gray `IDLE` | **PASSED** |
| **M2-04** | Session Reset | Tap "Reset" button | Counters reset to 0, history buffers cleared | **PASSED** |
| **M2-05** | Real-Time Numeric Updates | High-frequency axis readings | Monospace numbers update smoothly at ~30 Hz without jitter | **PASSED** |
| **M2-06** | Real-Time Waveform Rendering | Jetpack Compose Canvas time-series | 3 continuous lines ($X$: Red, $Y$: Green, $Z$: Blue) with grid | **PASSED** |
| **M2-07** | Rolling History Bounding | Continuous session stress test | History strictly bounded to 50 items; zero OOM | **PASSED** |
| **M2-08** | Hardware Specs Drawer | Tap Info icon on sensor cards | Expands hardware specs (Driver, Vendor, Range, Power) | **PASSED** |
| **M2-09** | Session Telemetry Stats | Live sample rate & total counter | Measured rate: **150.4 Hz**, processed **39,400+** samples | **PASSED** |

---

## 4. Local Data Persistence & CSV Export Checklist (Milestone 3A)

| Test Case | Description | Verification Method | Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| **M3-01** | Session Creation | Tap "Record Session to Database" | Dialog opens; initializes active session record | **PASSED** |
| **M3-02** | Batched Ingestion Pipeline | Ingest readings at 150 Hz | In-memory buffer flushes batches of 100 to SQLite on IO thread | **PASSED** |
| **M3-03** | Session Finalization | Tap "Stop & Save" | Buffer drained completely; tallies written; status `COMPLETED` | **PASSED** |
| **M3-04** | Session History Listing | Navigate to Recordings tab | Saved card displays session name, date, duration, sample count | **PASSED** |
| **M3-05** | Session Details View | Tap saved session card | Renders full analytical metrics and historical waveforms | **PASSED** |
| **M3-06** | Statistical Computation | Min, Max, Avg, Peak magnitude | Correctly calculates statistics across stored readings | **PASSED** |
| **M3-07** | Cascade Session Deletion | Tap delete icon on session card | Confirmation dialog shown; session and readings deleted | **PASSED** |
| **M3-08** | Empty History State | History list with 0 sessions | Displays informative empty state with call-to-action | **PASSED** |
| **M3-09** | Storage Access Framework (SAF) | Tap "Export Session to CSV" | Launches DocumentsUI file picker with suggested filename | **PASSED** |
| **M3-10** | CSV Output Validation | Inspect exported CSV file on disk | `6,583` lines (header + 6,582 records); valid comma-separated format | **PASSED** |

---

## 5. Automated Unit Tests

Automated unit tests were executed with JUnit:
```powershell
.\gradlew.bat testDebugUnitTest
```
- `SensorDataPointTest`:
  - Verified 3D vector magnitude calculation ($\sqrt{X^2 + Y^2 + Z^2}$).
  - Verified 1g gravity stationary baseline ($9.81\ \text{m/s}^2$).
  - Verified `SensorAccuracy` mapping across all Android framework states.
- `DashboardUiStateTest`:
  - Verified time duration formatting (`00:42`, `02:05`, `01:01:05`).
  - Verified state predicates (`isMonitoring`, `isRecording`).
- `CsvExportTest`:
  - Validates CSV header structure and data column mapping.

Result: **100% Passed**.

---

## 6. Known Limitations & Deferred Scope

- **Milestone 3B (ESP32, BLE, Cloud Sync):** Strictly marked as **PENDING** as specified in the project charter.
- **Orientation Lock:** The app adapts to orientation changes, but is optimized for portrait telemetry viewing.
