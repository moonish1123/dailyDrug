package com.dailydrug.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dailydrug.data.local.converter.DateTimeConverters
import com.dailydrug.data.local.dao.MedicationRecordDao
import com.dailydrug.data.local.dao.MedicationScheduleDao
import com.dailydrug.data.local.dao.MedicineDao
import com.dailydrug.data.local.entity.MedicationRecordEntity
import com.dailydrug.data.local.entity.MedicationScheduleEntity
import com.dailydrug.data.local.entity.MedicineEntity

@Database(
    entities = [
        MedicineEntity::class,
        MedicationScheduleEntity::class,
        MedicationRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun medicationRecordDao(): MedicationRecordDao

    companion object {
        const val DATABASE_NAME = "dailydrug.db"
    }
}
