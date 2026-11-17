package com.motionsense.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.motionsense.app.data.local.dao.SensorReadingDao
import com.motionsense.app.data.local.dao.SensorSessionDao
import com.motionsense.app.data.local.entity.SensorReadingEntity
import com.motionsense.app.data.local.entity.SensorSessionEntity

@Database(
    entities = [
        SensorSessionEntity::class,
        SensorReadingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MotionSenseDatabase : RoomDatabase() {

    abstract fun sensorSessionDao(): SensorSessionDao
    abstract fun sensorReadingDao(): SensorReadingDao

    companion object {
        private const val DATABASE_NAME = "motionsense.db"

        @Volatile
        private var INSTANCE: MotionSenseDatabase? = null

        fun getInstance(context: Context): MotionSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MotionSenseDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
