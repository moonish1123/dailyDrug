package com.dailydrug.domain.model

import java.time.LocalDateTime

/**
 * Domain-level medication record capturing an individual intake event.
 */
data class MedicationRecord(
    val id: Long = 0,
    val scheduleId: Long,
    val scheduledDateTime: LocalDateTime,
    val takenDateTime: LocalDateTime?,
    val status: MedicationStatus,
    val note: String = ""
)

enum class MedicationStatus {
    PENDING,
    TAKEN,
    SKIPPED
}
