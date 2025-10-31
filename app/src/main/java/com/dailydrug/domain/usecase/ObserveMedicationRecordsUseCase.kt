package com.dailydrug.domain.usecase

import com.dailydrug.domain.model.MedicationRecord
import com.dailydrug.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMedicationRecordsUseCase @Inject constructor(
    private val repository: MedicationRepository
) {
    operator fun invoke(medicineId: Long): Flow<List<MedicationRecord>> = repository.observeMedicationRecords(medicineId)
}
