package com.dailydrug.domain.usecase

import com.dailydrug.domain.model.CreateScheduleParams
import com.dailydrug.domain.repository.MedicationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreateScheduleUseCase(
    private val repository: MedicationRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Validates and normalizes the incoming payload before delegating to the repository.
     */
    suspend operator fun invoke(params: CreateScheduleParams): Long = withContext(dispatcher) {
        val normalized = params.copy(
            timeSlots = params.timeSlots
                .distinct()
                .sorted()
        )
        repository.createSchedule(normalized)
    }
}
