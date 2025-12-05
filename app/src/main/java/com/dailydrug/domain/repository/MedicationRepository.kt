package com.dailydrug.domain.repository

import com.dailydrug.domain.model.CreateScheduleParams
import com.dailydrug.domain.model.ScheduledDose
import com.dailydrug.domain.model.MedicineDetail
import com.dailydrug.domain.model.MedicationRecord
import com.dailydrug.domain.model.ScheduleDetail
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Contract between domain layer and data layer.
 * The data module will provide the concrete implementation via Hilt bindings in later phases.
 */
interface MedicationRepository {

    /**
     * Observe all doses scheduled for the given date, including current taken/skip state.
     */
    fun observeScheduledDoses(date: LocalDate): Flow<List<ScheduledDose>>

    /**
     * Persist intake result for a given record. Passing null for [takenAt] clears the taken state.
     */
    suspend fun recordDose(recordId: Long, takenAt: LocalDateTime?, note: String? = null)

    /**
     * Marks the record as skipped with an optional note.
     */
    suspend fun skipDose(recordId: Long, note: String?)

    /**
     * Creates or updates a medicine schedule and returns the new schedule id.
     */
    suspend fun createSchedule(params: CreateScheduleParams): Long

    /**
     * Remove a schedule and any future reminders/records associated with it.
     */
    suspend fun deleteSchedule(scheduleId: Long)

    /**
     * Fetch a schedule with its associated medicine for edit forms.
     */
    suspend fun getScheduleDetail(scheduleId: Long): ScheduleDetail?

    /**
     * Fetch a single scheduled dose (used for notifications).
     */
    suspend fun getScheduledDose(recordId: Long): ScheduledDose?

    /**
     * Queue any alarms or workers needed to remind the user about an upcoming dose.
     * Later phases will have the repository delegate this to WorkManager / AlarmManager.
     */
    suspend fun scheduleReminder(recordId: Long, triggerAt: LocalDateTime)

    /**
     * Ensure medication records are generated up to the provided [date] horizon.
     * Implementations should avoid duplicating existing occurrences.
     */
    suspend fun ensureOccurrencesUpTo(date: LocalDate)

    /**
     * Observe a medicine along with its schedules and records for detail view.
     */
    fun observeMedicineDetail(medicineId: Long): Flow<MedicineDetail?>

    /**
     * Observe raw medication records for visualizing history charts/calendars.
     */
    fun observeMedicationRecords(medicineId: Long): Flow<List<MedicationRecord>>
}
