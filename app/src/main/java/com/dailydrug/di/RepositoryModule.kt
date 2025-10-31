package com.dailydrug.di

import com.dailydrug.data.local.dao.MedicationRecordDao
import com.dailydrug.data.local.dao.MedicationScheduleDao
import com.dailydrug.data.local.dao.MedicineDao
import com.dailydrug.data.local.database.AppDatabase
import com.dailydrug.data.repository.MedicationRepositoryImpl
import com.dailydrug.data.worker.ReminderScheduler
import com.dailydrug.domain.repository.MedicationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMedicationRepository(
        database: AppDatabase,
        medicineDao: MedicineDao,
        scheduleDao: MedicationScheduleDao,
        recordDao: MedicationRecordDao,
        reminderScheduler: ReminderScheduler,
        clock: Clock
    ): MedicationRepository = MedicationRepositoryImpl(
        database = database,
        medicineDao = medicineDao,
        scheduleDao = scheduleDao,
        recordDao = recordDao,
        reminderScheduler = reminderScheduler,
        clock = clock
    )
}
