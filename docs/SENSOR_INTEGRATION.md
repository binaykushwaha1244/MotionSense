# Android Sensor Framework Integration Guide

This guide documents the technical details of hardware sensor acquisition in **MotionSense**, covering the Android Sensor Framework, sensor physics, units, lifecycle safety, and hardware verification on physical devices.

---

## 1. Android Sensor Framework Fundamentals

Android provides sensor access through `android.hardware.SensorManager`, which interfaces with the hardware abstraction layer (HAL) and kernel drivers:

```kotlin
val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
```

Hardware events are reported via `SensorEventListener`:

```kotlin
interface SensorEventListener {
    fun onSensorChanged(event: SensorEvent?)
    fun onAccuracyChanged(sensor: Sensor?, accuracy: Int)
}
```

---

## 2. Supported Sensor Types & Physical Units

### A. Accelerometer (`Sensor.TYPE_ACCELEROMETER`)
- **Physical Property:** Acceleration applied to the device, including the force of gravity.
- **Unit of Measurement:** Meters per second squared ($\text{m/s}^2$).
- **Coordinate Reference Frame:**
  - **X-axis:** Horizontal, pointing right relative to the screen.
  - **Y-axis:** Vertical, pointing upward relative to the screen.
  - **Z-axis:** Orthogonal, pointing out of the front screen face.
- **Gravity Baseline at Rest:**
  When a smartphone lies flat on a table face-up:
  $$X \approx 0.0\ \text{m/s}^2,\quad Y \approx 0.0\ \text{m/s}^2,\quad Z \approx +9.81\ \text{m/s}^2$$
  The measured magnitude is:
  $$\|A\| = \sqrt{X^2 + Y^2 + Z^2} \approx 9.81\ \text{m/s}^2 \quad (1\text{g})$$

### B. Gyroscope (`Sensor.TYPE_GYROSCOPE`)
- **Physical Property:** Device rate of rotation around each of the three physical axes.
- **Unit of Measurement:** Radians per second ($\text{rad/s}$).
- **Sign Convention (Right-Hand Rule):**
  - Positive rotation counter-clockwise when looking along an axis towards the origin.
- **Baseline at Rest:**
  When a smartphone is stationary on a desk:
  $$X \approx 0.00\ \text{rad/s},\quad Y \approx 0.00\ \text{rad/s},\quad Z \approx 0.00\ \text{rad/s}$$
  The angular velocity magnitude:
  $$\|\Omega\| = \sqrt{\omega_x^2 + \omega_y^2 + \omega_z^2} \approx 0.00\ \text{rad/s}$$

---

## 3. Sampling Rates and Delays

Android offers standard delay hints:
- `SensorManager.SENSOR_DELAY_NORMAL`: ~200,000 µs (5 Hz)
- `SensorManager.SENSOR_DELAY_UI`: ~60,000 µs (~16.7 Hz)
- `SensorManager.SENSOR_DELAY_GAME`: ~20,000 µs (~50 Hz)
- `SensorManager.SENSOR_DELAY_FASTEST`: 0 µs (Driver maximum, up to 400 Hz)

**MotionSense Configuration:**
- Requests `SENSOR_DELAY_GAME` (20,000 µs delay hint).
- On the physical OnePlus Nord CE5, the Bosch BMI2xy hardware driver reports live events at approximately **~150 Hz** combined.
- The ViewModel decouples this stream by maintaining an event queue and updating Compose state at a steady **30 Hz**, giving smooth visualization without draining the battery.

---

## 4. Timestamps & Time Synchronization

Each `SensorEvent` includes:
- `event.timestamp`: Nanoseconds since device boot (`SystemClock.elapsedRealtimeNanos()`).
- High-precision time unaffected by network time drift or manual clock adjustments.
- `SensorDataPoint` captures this hardware timestamp for telemetry fidelity alongside `receivedAtMs` (epoch milliseconds) for session duration calculations.

---

## 5. Reactive Listener Management with `callbackFlow`

To prevent memory leaks and dangling listeners, MotionSense encapsulates the Android listener inside a Kotlin Coroutine `callbackFlow`:

```kotlin
override fun getSensorStream(
    sensorType: SensorType,
    samplingPeriodUs: Int
): Flow<SensorDataPoint> = callbackFlow {
    var currentAccuracy = SensorAccuracy.UNKNOWN

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.values.size < 3) return
            trySend(
                SensorDataPoint(
                    sensorType = sensorType,
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    timestampNs = event.timestamp,
                    accuracy = currentAccuracy
                )
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            currentAccuracy = SensorAccuracy.fromInt(accuracy)
        }
    }

    sensorManager.registerListener(listener, sensor, samplingPeriodUs)

    awaitClose {
        sensorManager.unregisterListener(listener, sensor)
    }
}
```

### Safety Guarantees:
1. **Zero Duplicate Listeners:** Listeners are registered only when the collector launches.
2. **Deterministic Cleanup:** When the collector cancels (e.g. user taps "Stop Monitoring" or Activity backgrounded), `awaitClose` immediately unregisters the listener.
3. **Verified via ADB:** Confirmed via `adb shell dumpsys sensorservice` that 0 listeners remain registered after monitoring is stopped.

---

## 6. Physical Device Hardware Specifications (OnePlus Nord CE5)

| Metric | Accelerometer | Gyroscope |
| :--- | :--- | :--- |
| **Hardware Driver** | `bmi2xy acc` | `bmi2xy gyro` |
| **Vendor** | Bosch Sensortec (`bosch`) | Bosch Sensortec (`bosch`) |
| **Android Type** | `TYPE_ACCELEROMETER` (1) | `TYPE_GYROSCOPE` (4) |
| **Max Range** | $78.45\ \text{m/s}^2$ ($\approx 8\text{g}$) | $34.91\ \text{rad/s}$ ($\approx 2000^\circ/\text{s}$) |
| **Resolution** | $0.001200\ \text{m/s}^2$ | $0.001068\ \text{rad/s}$ |
| **Power Consumption** | $0.001\ \text{mA}$ | $0.500\ \text{mA}$ |
| **Delay Range** | $5,000\ \mu\text{s} - 200,000\ \mu\text{s}$ | $5,000\ \mu\text{s} - 200,000\ \mu\text{s}$ |
| **Reporting Mode** | Continuous | Continuous |
