package com.dailydrug.presentation.main

import com.dailydrug.domain.model.MedicationStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class MainUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val todayMedications: List<TodayMedication> = emptyList(),
    val isLoading: Boolean = false
)

data class TodayMedication(
    val recordId: Long,
    val scheduleId: Long,
    val medicineId: Long,
    val medicineName: String,
    val dosage: String,
    val scheduledTime: LocalTime,
    val status: MedicationStatus,
    val takenTime: LocalDateTime?,
    val color: Int
)
