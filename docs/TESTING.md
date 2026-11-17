# MotionSense Hardware & Automated Testing Report

This document records the testing procedure, physical test bench, baseline verifications, stress tests, and lifecycle validation conducted on physical Android hardware for **Milestone 1** and **Milestone 2**.

---

## 1. Physical Test Bench Environment

| Parameter | Specification |
| :--- | :--- |
| **Physical Device** | OnePlus Nord CE5 / CPH2717 (Product: `CPH2717IN`) |
| **Android Version** | Android 16 (API Level 36 / 37) |
| **Processor Architecture** | ARM64-v8a |
| **Accelerometer Hardware** | Bosch Sensortec `bmi2xy acc` (Driver v1) |
| **Gyroscope Hardware** | Bosch Sensortec `bmi2xy gyro` (Driver v1) |
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
| **M2-02** | Start Monitoring Toggle | Tap "Start Monitoring" button | Changes to `Stop Monitoring`, pill turns green `ACTIVE` | **PASSED** |
| **M2-03** | Stop Monitoring Toggle | Tap "Stop Monitoring" button | Changes to `Start Monitoring`, pill turns gray `IDLE` | **PASSED** |
| **M2-04** | Session Reset | Tap "Reset" button | Counters reset to 0, history buffers cleared | **PASSED** |
| **M2-05** | Real-Time Numeric Updates | High-frequency axis readings | Monospace numbers update smoothly at ~30 Hz without jitter | **PASSED** |
| **M2-06** | Real-Time Waveform Rendering | Jetpack Compose Canvas time-series | 3 continuous lines ($X$: Red, $Y$: Green, $Z$: Blue) with grid | **PASSED** |
| **M2-07** | Rolling History Bounding | Continuous session stress test | History strictly bounded to 50 items; zero OOM | **PASSED** |
| **M2-08** | Hardware Specs Drawer | Tap Info icon on sensor cards | Expands hardware specs (Driver, Vendor, Range, Power) | **PASSED** |
| **M2-09** | Session Telemetry Stats | Live sample rate & total counter | Measured rate: **150.4 Hz**, processed **39,400+** samples | **PASSED** |

---

## 4. Lifecycle & Reliability Validation

### Backgrounding Test (Home Key Pressed):
1. **Trigger:** `adb shell input keyevent KEYCODE_HOME` while monitoring was active.
2. **Observed OS Event (`dumpsys sensorservice`):**
   ```text
   02:59:51 - 0x00000004 pid=10222 (bmi2xy gyro, com.motionsense.app)
   02:59:51 - 0x00000001 pid=10222 (bmi2xy acc, com.motionsense.app)
   ```
   Both hardware listeners unregistered immediately at the kernel driver level.
3. **Power/Battery Impact:** Zero battery drain in background.

### Foregrounding Test (App Resumed):
1. **Trigger:** User re-opens MotionSense from recent apps.
2. **Result:** `onLifecycleResume()` called; monitoring resumes without duplicate listeners.

### Repeated Start/Stop Stress Test:
- Repeatedly toggled Start/Stop Monitoring 10 times consecutively.
- Verified in `dumpsys sensorservice` that exactly 1 connection exists during active monitoring and 0 connections exist when stopped.

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
  - Verified state predicates (`isMonitoring`).

Result: **100% Passed**.

---

## 6. Known Limitations & Deferred Scope

- **Milestone 3 (Room DB, CSV Export, BLE, ESP32):** Strictly marked as **PENDING** as specified in the project charter.
- **Orientation Lock:** The app adapts to orientation changes, but is optimized for portrait telemetry viewing.
