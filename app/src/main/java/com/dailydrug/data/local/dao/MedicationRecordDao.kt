package com.dailydrug.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dailydrug.data.local.entity.MedicationRecordEntity
import com.dailydrug.data.local.result.ScheduledDoseTuple
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<MedicationRecordEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MedicationRecordEntity): Long

    @Query("SELECT * FROM medication_records WHERE id = :id")
    suspend fun getById(id: Long): MedicationRecordEntity?

    @Query(
        """
        SELECT * FROM medication_records
        WHERE scheduleId = :scheduleId AND scheduledDateTime = :scheduledDateTime
        LIMIT 1
        """
    )
    suspend fun getByScheduleAndTime(scheduleId: Long, scheduledDateTime: LocalDateTime): MedicationRecordEntity?

    @Query(
        """
        UPDATE medication_records
        SET
            isTaken = :isTaken,
            takenDateTime = :takenDateTime,
            skipped = :skipped,
            note = CASE WHEN :note IS NULL THEN note ELSE :note END
        WHERE id = :recordId
        """
    )
    suspend fun updateRecordState(
        recordId: Long,
        takenDateTime: LocalDateTime?,
        isTaken: Boolean,
        skipped: Boolean,
        note: String?
    )

    @Query(
        """
        SELECT
            records.id AS record_id,
            records.scheduleId AS record_schedule_id,
            records.scheduledDateTime AS record_scheduledDateTime,
            records.takenDateTime AS record_takenDateTime,
            records.isTaken AS record_isTaken,
            records.skipped AS record_skipped,
            records.note AS record_note,
            schedules.id AS schedule_id,
            schedules.medicineId AS schedule_medicineId,
            schedules.startDate AS schedule_startDate,
            schedules.endDate AS schedule_endDate,
            schedules.timeSlots AS schedule_timeSlots,
            schedules.takeDays AS schedule_takeDays,
            schedules.restDays AS schedule_restDays,
            schedules.isActive AS schedule_isActive,
            medicines.id AS medicine_id,
            medicines.name AS medicine_name,
            medicines.dosage AS medicine_dosage,
            medicines.color AS medicine_color,
            medicines.memo AS medicine_memo,
            medicines.createdAt AS medicine_createdAt
        FROM medication_records AS records
        INNER JOIN medication_schedules AS schedules ON schedules.id = records.scheduleId
        INNER JOIN medicines ON medicines.id = schedules.medicineId
        WHERE records.scheduledDateTime >= :startOfDay AND records.scheduledDateTime < :endOfDay
        ORDER BY records.scheduledDateTime ASC
        """
    )
    fun observeScheduledDoses(
        startOfDay: LocalDateTime,
        endOfDay: LocalDateTime
    ): Flow<List<ScheduledDoseTuple>>

    @Query(
        """
        SELECT
            records.id AS record_id,
            records.scheduleId AS record_schedule_id,
            records.scheduledDateTime AS record_scheduledDateTime,
            records.takenDateTime AS record_takenDateTime,
            records.isTaken AS record_isTaken,
            records.skipped AS record_skipped,
            records.note AS record_note,
            schedules.id AS schedule_id,
            schedules.medicineId AS schedule_medicineId,
            schedules.startDate AS schedule_startDate,
            schedules.endDate AS schedule_endDate,
            schedules.timeSlots AS schedule_timeSlots,
            schedules.takeDays AS schedule_takeDays,
            schedules.restDays AS schedule_restDays,
            schedules.isActive AS schedule_isActive,
            medicines.id AS medicine_id,
            medicines.name AS medicine_name,
            medicines.dosage AS medicine_dosage,
            medicines.color AS medicine_color,
            medicines.memo AS medicine_memo,
            medicines.createdAt AS medicine_createdAt
        FROM medication_records AS records
        INNER JOIN medication_schedules AS schedules ON schedules.id = records.scheduleId
        INNER JOIN medicines ON medicines.id = schedules.medicineId
        WHERE records.id = :recordId
        LIMIT 1
        """
    )
    suspend fun getScheduledDose(recordId: Long): ScheduledDoseTuple?

    @Query(
        """
        SELECT records.*
        FROM medication_records AS records
        INNER JOIN medication_schedules AS schedules ON schedules.id = records.scheduleId
        WHERE schedules.medicineId = :medicineId
        ORDER BY records.scheduledDateTime DESC
        """
    )
    fun observeRecordsByMedicine(medicineId: Long): Flow<List<MedicationRecordEntity>>

    @Query(
        """
        SELECT MAX(scheduledDateTime)
        FROM medication_records
        WHERE scheduleId = :scheduleId
        """
    )
    suspend fun getLatestScheduledDateTime(scheduleId: Long): LocalDateTime?

    @Query(
        """
        SELECT id FROM medication_records
        WHERE scheduleId = :scheduleId AND scheduledDateTime >= :fromDateTime
        """
    )
    suspend fun getRecordIdsFrom(scheduleId: Long, fromDateTime: LocalDateTime): List<Long>

    @Query(
        """
        DELETE FROM medication_records
        WHERE scheduleId = :scheduleId AND scheduledDateTime >= :fromDateTime
        """
    )
    suspend fun deleteRecordsFrom(scheduleId: Long, fromDateTime: LocalDateTime)

    @Query(
        """
        SELECT id FROM medication_records
        WHERE scheduleId = :scheduleId
        """
    )
    suspend fun getRecordIds(scheduleId: Long): List<Long>

    @Query(
        """
        DELETE FROM medication_records
        WHERE scheduleId = :scheduleId
        """
    )
    suspend fun deleteBySchedule(scheduleId: Long)
}
