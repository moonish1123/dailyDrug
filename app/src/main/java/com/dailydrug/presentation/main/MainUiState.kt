package com.dailydrug.presentation.main

import com.dailydrug.domain.model.MedicationStatus
import com.dailydrug.domain.model.MedicationTimePeriod
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class MainUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val todayMedications: List<TodayMedication> = emptyList(),
    val medicationGroups: List<MedicationTimeGroupUi> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * 시간대별 약 그룹 UI 모델
 */
data class MedicationTimeGroupUi(
    val period: MedicationTimePeriod,
    val medications: List<TodayMedication>
) {
    val isAllTaken: Boolean
        get() = medications.all { it.status == MedicationStatus.TAKEN }

    val pendingCount: Int
        get() = medications.count { it.status == MedicationStatus.PENDING }
}

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
