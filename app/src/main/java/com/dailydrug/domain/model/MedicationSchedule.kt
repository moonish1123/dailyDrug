package com.dailydrug.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Domain-level medication schedule.
 * Uses LocalDate/LocalTime to remain time-zone aware when mapping to persistence layer.
 */
data class MedicationSchedule(
    val id: Long = 0,
    val medicineId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val timeSlots: List<LocalTime>,
    val takeDays: Int,
    val restDays: Int,
    val isActive: Boolean = true
)
