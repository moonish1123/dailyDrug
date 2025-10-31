package com.dailydrug.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "medication_records")
data class MedicationRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val scheduledDateTime: LocalDateTime,
    val takenDateTime: LocalDateTime?,
    val isTaken: Boolean,
    val skipped: Boolean,
    val note: String = ""
)
