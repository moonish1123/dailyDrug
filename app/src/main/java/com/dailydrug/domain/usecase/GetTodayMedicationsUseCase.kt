package com.dailydrug.domain.usecase

import com.dailydrug.domain.model.ScheduledDose
import com.dailydrug.domain.repository.MedicationRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn

class GetTodayMedicationsUseCase(
    private val repository: MedicationRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    operator fun invoke(date: LocalDate): Flow<List<ScheduledDose>> {
        return repository.observeScheduledDoses(date)
            .map { doses -> doses.sortedBy { it.scheduledDateTime } }
            .flowOn(dispatcher)
    }
}
