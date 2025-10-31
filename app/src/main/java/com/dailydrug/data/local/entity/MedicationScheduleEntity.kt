package com.dailydrug.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "medication_schedules")
data class MedicationScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicineId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val timeSlots: List<LocalTime>,
    val takeDays: Int,
    val restDays: Int,
    val isActive: Boolean = true
)
