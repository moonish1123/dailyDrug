package com.dailydrug.domain.usecase

import com.dailydrug.domain.repository.MedicationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteScheduleUseCase(
    private val repository: MedicationRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend operator fun invoke(scheduleId: Long) = withContext(dispatcher) {
        repository.deleteSchedule(scheduleId)
    }
}
