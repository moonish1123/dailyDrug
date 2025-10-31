package com.dailydrug.di

import com.dailydrug.domain.repository.MedicationRepository
import com.dailydrug.domain.usecase.CalculateSchedulePatternsUseCase
import com.dailydrug.domain.usecase.CreateScheduleUseCase
import com.dailydrug.domain.usecase.EnsureScheduleOccurrencesUseCase
import com.dailydrug.domain.usecase.GetTodayMedicationsUseCase
import com.dailydrug.domain.usecase.RecordMedicationUseCase
import com.dailydrug.domain.usecase.ScheduleNotificationUseCase
import com.dailydrug.domain.usecase.ObserveMedicineDetailUseCase
import com.dailydrug.domain.usecase.ObserveMedicationRecordsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideGetTodayMedicationsUseCase(repository: MedicationRepository): GetTodayMedicationsUseCase =
        GetTodayMedicationsUseCase(repository)

    @Provides
    fun provideRecordMedicationUseCase(
        repository: MedicationRepository,
        clock: Clock
    ): RecordMedicationUseCase = RecordMedicationUseCase(repository = repository, clock = clock)

    @Provides
    fun provideCreateScheduleUseCase(repository: MedicationRepository): CreateScheduleUseCase =
        CreateScheduleUseCase(repository)

    @Provides
    fun provideScheduleNotificationUseCase(repository: MedicationRepository): ScheduleNotificationUseCase =
        ScheduleNotificationUseCase(repository)

    @Provides
    fun provideEnsureScheduleOccurrencesUseCase(repository: MedicationRepository): EnsureScheduleOccurrencesUseCase =
        EnsureScheduleOccurrencesUseCase(repository)

    @Provides
    fun provideCalculateSchedulePatternsUseCase(): CalculateSchedulePatternsUseCase =
        CalculateSchedulePatternsUseCase()

    @Provides
    fun provideObserveMedicineDetailUseCase(repository: MedicationRepository): ObserveMedicineDetailUseCase =
        ObserveMedicineDetailUseCase(repository)

    @Provides
    fun provideObserveMedicationRecordsUseCase(repository: MedicationRepository): ObserveMedicationRecordsUseCase =
        ObserveMedicationRecordsUseCase(repository)
}
