package com.dailydrug.presentation.widget

import com.dailydrug.domain.repository.MedicationRepository
import com.dailydrug.domain.usecase.RecordMedicationUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun medicationRepository(): MedicationRepository
    fun recordMedicationUseCase(): RecordMedicationUseCase
}
