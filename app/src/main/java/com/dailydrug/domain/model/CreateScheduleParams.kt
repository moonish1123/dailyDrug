package com.dailydrug.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Immutable payload used when creating or updating a medication schedule.
 * If [medicineId] is null, the data layer should create a new medicine entry.
 * When [scheduleId] is provided, the repository should update the existing schedule.
 */
data class CreateScheduleParams(
    val scheduleId: Long? = null,
    val medicineId: Long? = null,
    val name: String,
    val dosage: String,
    val color: Int,
    val memo: String = "",
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val timeSlots: List<LocalTime>,
    val takeDays: Int,
    val restDays: Int
) {
    init {
        require(name.isNotBlank()) { "Medicine name must not be blank." }
        require(dosage.isNotBlank()) { "Dosage description must not be blank." }
        require(timeSlots.isNotEmpty()) { "At least one time slot must be provided." }
        require(takeDays > 0) { "takeDays must be greater than 0." }
        require(restDays >= 0) { "restDays must be zero or greater." }
        require(endDate == null || !endDate.isBefore(startDate)) { "endDate cannot be before startDate." }
    }
}
