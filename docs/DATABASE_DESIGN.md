# MotionSense Local Database Design & Room Architecture

This document specifies the technical design, schema, relationships, indexes, batching strategy, and concurrency models for local data persistence in **MotionSense** using Android Room (SQLite).

---

## 1. Architectural Role & Objective

The local storage subsystem fulfills **Milestone 3A** by providing robust, on-device data persistence without external network, backend, or cloud dependencies.

Key design goals:
1. **Zero Data Loss:** Buffer sensor readings during high-frequency acquisition (~150 Hz) and safely persist them without blocking the UI thread or hardware sensor interrupts.
2. **Relational Integrity:** Maintain parent-child relationships between recording sessions and individual data points with automatic cascade deletion.
3. **Query Performance:** Enable high-speed retrieval of historical sessions and time-series readings for analytical rendering and CSV streaming.

---

## 2. Entity-Relationship Model

```
┌──────────────────────────────────────────────┐
│             sensor_sessions                  │
├──────────────────────────────────────────────┤
│ PK  sessionId            : INTEGER (auto)    │
│     sessionName          : TEXT              │
│     startedAt            : INTEGER           │
│     endedAt              : INTEGER (nullable)│
│     durationMs           : INTEGER           │
│     deviceModel          : TEXT              │
│     status               : TEXT              │
│     accelerometerSamples : INTEGER           │
│     gyroscopeSamples     : INTEGER           │
└──────────────────────┬───────────────────────┘
                       │ 1
                       │
                       │ has many (CASCADE DELETE)
                       │
                       │ N
┌──────────────────────▼───────────────────────┐
│             sensor_readings                  │
├──────────────────────────────────────────────┤
│ PK  readingId            : INTEGER (auto)    │
│ FK  sessionId            : INTEGER           │
│     sensorType           : TEXT              │
│     x                    : REAL              │
│     y                    : REAL              │
│     z                    : REAL              │
│     magnitude            : REAL              │
│     timestampNs          : INTEGER           │
│     accuracy             : INTEGER           │
│     receivedAtMs         : INTEGER           │
└──────────────────────────────────────────────┘
```

---

## 3. Schema Definitions

### A. `SensorSessionEntity` (`sensor_sessions`)
Represents a discrete recording session initiated by the user:

```kotlin
@Entity(tableName = "sensor_sessions")
data class SensorSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0L,
    val sessionName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationMs: Long = 0L,
    val deviceModel: String,
    val status: String = STATUS_ACTIVE,
    val accelerometerSamples: Int = 0,
    val gyroscopeSamples: Int = 0
)
```

### B. `SensorReadingEntity` (`sensor_readings`)
Represents an individual 3-axis motion measurement:

```kotlin
@Entity(
    tableName = "sensor_readings",
    foreignKeys = [
        ForeignKey(
            entity = SensorSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestampNs"]),
        Index(value = ["sessionId", "sensorType"])
    ]
)
data class SensorReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val readingId: Long = 0L,
    val sessionId: Long,
    val sensorType: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float,
    val timestampNs: Long,
    val accuracy: Int,
    val receivedAtMs: Long
)
```

---

## 4. Database Indexing Strategy

High-frequency sensor acquisition produces thousands of rows per session. The following B-Tree indexes are enforced:
1. `index_sensor_readings_sessionId`: Accelerates foreign key cascade deletions and session-level joins.
2. `index_sensor_readings_timestampNs`: Ensures $O(\log N)$ chronological sorting for historical waveform plotting.
3. `index_sensor_readings_sessionId_sensorType`: Enables instant separation of Accelerometer vs Gyroscope streams for statistical analysis.

---

## 5. Batched Write Buffer & Concurrency Strategy

Writing every sensor reading immediately to SQLite in its own transaction at 150 Hz causes disk I/O thrashing and frame drops. MotionSense implements a **two-tier batched write pipeline**:

```
SensorEvent Interrupt (~150 Hz)
            │
            ▼
ConcurrentLinkedQueue<SensorReadingEntity> (In-Memory Buffer)
            │
     ┌──────┴─────────────────────────────────┐
     │ Condition 1: Buffer Size >= 100        │
     │ Condition 2: Periodic Ticker (500ms)   │
     │ Condition 3: User Taps "Stop & Save"   │
     └──────┬─────────────────────────────────┘
            │
            ▼
Dispatchers.IO Coroutine Worker
            │
            ▼
readingDao.insertBatch(batch) [Single Atomic SQLite Transaction]
```

### Guarantees:
- **Zero UI Stutter:** All database operations run strictly on `Dispatchers.IO`.
- **Drain on Stop:** When the user taps "Stop & Save", `flushPendingReadings()` drains every remaining element before computing totals, guaranteeing zero lost records.
- **Thread Safety:** Implemented with `ConcurrentLinkedQueue` and `AtomicInteger` for lock-free ingestion.

---

## 6. CSV Export Architecture

CSV export uses the Android **Storage Access Framework (SAF)** via `ActivityResultContracts.CreateDocument("text/csv")`:
- **No Dangerous Permissions:** Does not require legacy `WRITE_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE`.
- **Streaming Execution:** Directly opens `context.contentResolver.openOutputStream(uri)` and buffers output via `bufferedWriter(Charsets.UTF_8)`. Memory consumption remains constant ($O(1)$) regardless of file size.
- **Standard Headers:**
  ```csv
  Session_ID,Timestamp_Ns,Received_At_Epoch_Ms,Sensor_Type,Axis_X,Axis_Y,Axis_Z,Magnitude,Accuracy
  1,12091514116012,1794956312332,ACCELEROMETER,0.051000,1.609050,9.681001,9.813940,0
  ```
