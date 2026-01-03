package com.dailydrug.domain.model

import java.time.LocalDateTime

/**
 * Aggregated view used by presentation layer: a specific scheduled intake paired with medicine metadata.
 */
data class ScheduledDose(
    val recordId: Long,
    val scheduleId: Long,
    val medicine: Medicine,
    val scheduledDateTime: LocalDateTime,
    val takenDateTime: LocalDateTime?,
    val status: MedicationStatus
) {
    val isMissed: Boolean get() = status == MedicationStatus.PENDING

    /**
     * 예약된 시간에 해당하는 시간대 반환
     */
    val timePeriod: MedicationTimePeriod
        get() = MedicationTimePeriod.fromTime(scheduledDateTime.toLocalTime())
}
