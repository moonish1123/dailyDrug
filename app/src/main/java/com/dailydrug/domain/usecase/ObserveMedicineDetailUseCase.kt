package com.dailydrug.domain.usecase

import com.dailydrug.domain.model.MedicineDetail
import com.dailydrug.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMedicineDetailUseCase @Inject constructor(
    private val repository: MedicationRepository
) {
    operator fun invoke(medicineId: Long): Flow<MedicineDetail?> = repository.observeMedicineDetail(medicineId)
}
