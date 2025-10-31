package com.dailydrug.domain.usecase

import com.dailydrug.domain.repository.MedicationRepository
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordMedicationUseCase(
    private val repository: MedicationRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    data class Params(
        val recordId: Long,
        val markAsTaken: Boolean,
        val skip: Boolean = false,
        val note: String? = null,
        val takenAt: LocalDateTime? = null
    )

    suspend operator fun invoke(params: Params) = withContext(dispatcher) {
        when {
            params.skip -> repository.skipDose(params.recordId, params.note)
            params.markAsTaken -> {
                val takenAt = params.takenAt ?: LocalDateTime.now(clock)
            repository.recordDose(params.recordId, takenAt, params.note)
        }
        else -> repository.recordDose(params.recordId, null, params.note)
    }
}
}
