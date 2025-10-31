package com.dailydrug.domain.usecase

import com.dailydrug.domain.model.ScheduledDose
import com.dailydrug.domain.model.MedicationStatus
import com.dailydrug.domain.repository.MedicationRepository
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleNotificationUseCase(
    private val repository: MedicationRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend operator fun invoke(
        recordId: Long,
        triggerAt: LocalDateTime? = null
    ) = withContext(dispatcher) {
        val dose = repository.getScheduledDose(recordId) ?: return@withContext
        if (dose.status != MedicationStatus.PENDING) return@withContext

        val trigger = triggerAt ?: dose.scheduledDateTime
        repository.scheduleReminder(recordId, trigger)
    }

    suspend fun scheduleAll(doses: List<ScheduledDose>) = withContext(dispatcher) {
        doses
            .filter { it.status == MedicationStatus.PENDING }
            .forEach { dose ->
                repository.scheduleReminder(dose.recordId, dose.scheduledDateTime)
            }
    }
}
