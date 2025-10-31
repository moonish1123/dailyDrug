package com.dailydrug.domain.usecase

import com.dailydrug.domain.repository.MedicationRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EnsureScheduleOccurrencesUseCase(
    private val repository: MedicationRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend operator fun invoke(horizon: LocalDate) = withContext(dispatcher) {
        repository.ensureOccurrencesUpTo(horizon)
    }
}
