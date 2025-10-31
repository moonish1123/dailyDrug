package com.dailydrug.di

import android.content.Context
import androidx.room.Room
import com.dailydrug.data.local.dao.MedicationRecordDao
import com.dailydrug.data.local.dao.MedicationScheduleDao
import com.dailydrug.data.local.dao.MedicineDao
import com.dailydrug.data.local.database.AppDatabase
import com.dailydrug.data.worker.ReminderScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMedicineDao(db: AppDatabase): MedicineDao = db.medicineDao()

    @Provides
    fun provideMedicationScheduleDao(db: AppDatabase): MedicationScheduleDao = db.medicationScheduleDao()

    @Provides
    fun provideMedicationRecordDao(db: AppDatabase): MedicationRecordDao = db.medicationRecordDao()

    @Provides
    @Singleton
    fun provideReminderScheduler(@ApplicationContext context: Context): ReminderScheduler =
        ReminderScheduler(context)

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
