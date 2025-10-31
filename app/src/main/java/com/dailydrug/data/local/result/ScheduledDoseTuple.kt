package com.dailydrug.data.local.result

import androidx.room.ColumnInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ScheduledDoseTuple(
    @ColumnInfo(name = "record_id")
    val recordId: Long,
    @ColumnInfo(name = "record_schedule_id")
    val recordScheduleId: Long,
    @ColumnInfo(name = "record_scheduledDateTime")
    val recordScheduledDateTime: LocalDateTime,
    @ColumnInfo(name = "record_takenDateTime")
    val recordTakenDateTime: LocalDateTime?,
    @ColumnInfo(name = "record_isTaken")
    val recordIsTaken: Boolean,
    @ColumnInfo(name = "record_skipped")
    val recordSkipped: Boolean,
    @ColumnInfo(name = "record_note")
    val recordNote: String,

    @ColumnInfo(name = "schedule_id")
    val scheduleId: Long,
    @ColumnInfo(name = "schedule_medicineId")
    val scheduleMedicineId: Long,
    @ColumnInfo(name = "schedule_startDate")
    val scheduleStartDate: LocalDate,
    @ColumnInfo(name = "schedule_endDate")
    val scheduleEndDate: LocalDate?,
    @ColumnInfo(name = "schedule_timeSlots")
    val scheduleTimeSlots: List<LocalTime>,
    @ColumnInfo(name = "schedule_takeDays")
    val scheduleTakeDays: Int,
    @ColumnInfo(name = "schedule_restDays")
    val scheduleRestDays: Int,
    @ColumnInfo(name = "schedule_isActive")
    val scheduleIsActive: Boolean,

    @ColumnInfo(name = "medicine_id")
    val medicineId: Long,
    @ColumnInfo(name = "medicine_name")
    val medicineName: String,
    @ColumnInfo(name = "medicine_dosage")
    val medicineDosage: String,
    @ColumnInfo(name = "medicine_color")
    val medicineColor: Int,
    @ColumnInfo(name = "medicine_memo")
    val medicineMemo: String,
    @ColumnInfo(name = "medicine_createdAt")
    val medicineCreatedAt: Long
)
