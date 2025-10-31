package com.dailydrug.data.repository

import androidx.room.withTransaction
import com.dailydrug.data.local.dao.MedicationRecordDao
import com.dailydrug.data.local.dao.MedicationScheduleDao
import com.dailydrug.data.local.dao.MedicineDao
import com.dailydrug.data.local.database.AppDatabase
import com.dailydrug.data.local.entity.MedicationRecordEntity
import com.dailydrug.data.local.entity.MedicationScheduleEntity
import com.dailydrug.data.local.entity.MedicineEntity
import com.dailydrug.data.local.result.ScheduledDoseTuple
import com.dailydrug.data.worker.ReminderScheduler
import com.dailydrug.domain.model.CreateScheduleParams
import com.dailydrug.domain.model.Medicine
import com.dailydrug.domain.model.MedicineDetail
import com.dailydrug.domain.model.MedicationRecord
import com.dailydrug.domain.model.MedicationStatus
import com.dailydrug.domain.model.ScheduledDose
import com.dailydrug.domain.model.MedicationSchedule
import com.dailydrug.domain.repository.MedicationRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.temporal.ChronoUnit

class MedicationRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val medicineDao: MedicineDao,
    private val scheduleDao: MedicationScheduleDao,
    private val recordDao: MedicationRecordDao,
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock
) : MedicationRepository {

    private val ioDispatcher = Dispatchers.IO
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun observeScheduledDoses(date: LocalDate): Flow<List<ScheduledDose>> {
        val start = date.atStartOfDay()
        val end = start.plusDays(1)
        return recordDao.observeScheduledDoses(start, end)
            .map { tuples -> tuples.map { it.toDomain() } }
    }

    override suspend fun recordDose(recordId: Long, takenAt: LocalDateTime?, note: String?) {
        withContext(ioDispatcher) {
            recordDao.updateRecordState(
                recordId = recordId,
                takenDateTime = takenAt,
                isTaken = takenAt != null,
                skipped = false,
                note = note
            )
            if (takenAt != null) {
                reminderScheduler.cancelReminder(recordId)
            }
        }
    }

    override suspend fun skipDose(recordId: Long, note: String?) {
        withContext(ioDispatcher) {
            recordDao.updateRecordState(
                recordId = recordId,
                takenDateTime = null,
                isTaken = false,
                skipped = true,
                note = note
            )
            reminderScheduler.cancelReminder(recordId)
        }
    }

    override suspend fun createSchedule(params: CreateScheduleParams): Long = withContext(ioDispatcher) {
        database.withTransaction {
            val medicineId = upsertMedicine(params)
            val scheduleEntity = MedicationScheduleEntity(
                medicineId = medicineId,
                startDate = params.startDate,
                endDate = params.endDate,
                timeSlots = params.timeSlots,
                takeDays = params.takeDays,
                restDays = params.restDays,
                isActive = true
            )
            val scheduleId = scheduleDao.insert(scheduleEntity)
            val insertedSchedule = scheduleEntity.copy(id = scheduleId)
            val horizon = endOfNextMonth(LocalDate.now(clock))
            val newlyInserted = ensureOccurrencesForSchedule(insertedSchedule, horizon)
            if (newlyInserted.isNotEmpty()) {
                val recordIds = newlyInserted.map { it.first }
                val occurrences = newlyInserted.map { it.second }
                scheduleInitialReminder(
                    recordIds = recordIds,
                    occurrences = occurrences,
                    medicine = medicineDao.getById(medicineId) ?: return@withTransaction scheduleId
                )
            }
            reminderScheduler.scheduleDailyRefresh()
            scheduleId
        }
    }

    override suspend fun ensureOccurrencesUpTo(date: LocalDate) = withContext(ioDispatcher) {
        database.withTransaction {
            val schedules = scheduleDao.getActiveSchedules()
            schedules.forEach { schedule ->
                ensureOccurrencesForSchedule(schedule, date)
            }
        }
    }

    override suspend fun getScheduledDose(recordId: Long): ScheduledDose? = withContext(ioDispatcher) {
        recordDao.getScheduledDose(recordId)?.toDomain()
    }

    override suspend fun scheduleReminder(recordId: Long, triggerAt: LocalDateTime) {
        withContext(ioDispatcher) {
            val tuple = recordDao.getScheduledDose(recordId) ?: return@withContext
            reminderScheduler.scheduleDoseReminder(
                recordId = recordId,
                medicineId = tuple.medicineId,
                medicineName = tuple.medicineName,
                dosage = tuple.medicineDosage,
                scheduledTime = triggerAt.toLocalTime().format(timeFormatter),
                triggerAt = triggerAt
            )
        }
    }

    override fun observeMedicineDetail(medicineId: Long): Flow<MedicineDetail?> {
        return combine(
            medicineDao.observeById(medicineId),
            scheduleDao.observeByMedicineId(medicineId),
            recordDao.observeRecordsByMedicine(medicineId)
        ) { medicineEntity, schedules, records ->
            medicineEntity?.let {
                MedicineDetail(
                    medicine = it.toDomain(),
                    schedules = schedules.map { schedule -> schedule.toDomain() },
                    records = records.map { record -> record.toDomain() }
                )
            }
        }
    }

    override fun observeMedicationRecords(medicineId: Long): Flow<List<MedicationRecord>> {
        return recordDao.observeRecordsByMedicine(medicineId)
            .map { records -> records.map { it.toDomain() } }
    }

    private suspend fun upsertMedicine(params: CreateScheduleParams): Long {
        val now = Instant.now(clock).toEpochMilli()
        val medicineId = params.medicineId
        val entity = MedicineEntity(
            id = medicineId ?: 0,
            name = params.name,
            dosage = params.dosage,
            color = params.color,
            memo = params.memo,
            createdAt = now
        )
        return if (medicineId == null) {
            medicineDao.insert(entity)
        } else {
            val existing = medicineDao.getById(medicineId)
            if (existing != null) {
                medicineDao.update(
                    existing.copy(
                        name = params.name,
                        dosage = params.dosage,
                        color = params.color,
                        memo = params.memo
                    )
                )
                medicineId
            } else {
                medicineDao.insert(entity.copy(id = 0))
            }
        }
    }

    private suspend fun ensureOccurrencesForSchedule(
        schedule: MedicationScheduleEntity,
        horizon: LocalDate
    ): List<Pair<Long, LocalDateTime>> {
        val effectiveEnd = schedule.endDate?.let { minOf(it, horizon) } ?: horizon
        if (effectiveEnd.isBefore(schedule.startDate)) return emptyList()

        val lastScheduledDate = if (schedule.id != 0L) {
            recordDao.getLatestScheduledDateTime(schedule.id)?.toLocalDate()
        } else {
            null
        }
        val startDate = when {
            lastScheduledDate != null -> lastScheduledDate.plusDays(1)
            else -> schedule.startDate
        }
        if (startDate.isAfter(effectiveEnd)) return emptyList()

        val occurrences = generateOccurrences(
            schedule = schedule,
            startDate = startDate,
            endDate = effectiveEnd
        )
        if (occurrences.isEmpty()) return emptyList()

        val records = occurrences.map { occurrence ->
            MedicationRecordEntity(
                scheduleId = schedule.id,
                scheduledDateTime = occurrence,
                takenDateTime = null,
                isTaken = false,
                skipped = false,
                note = ""
            )
        }
        val recordIds = recordDao.insertAll(records)
        return recordIds.zip(occurrences)
    }

    private fun generateOccurrences(
        schedule: MedicationScheduleEntity,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDateTime> {
        val effectiveEnd = schedule.endDate?.let { minOf(it, endDate) } ?: endDate
        if (effectiveEnd.isBefore(startDate)) return emptyList()
        val result = mutableListOf<LocalDateTime>()
        val slots = schedule.timeSlots.sorted()
        var current = startDate
        while (!current.isAfter(effectiveEnd)) {
            if (shouldTakeMedicationOn(schedule, current)) {
                slots.forEach { slot ->
                    result += LocalDateTime.of(current, slot)
                }
            }
            current = current.plusDays(1)
        }
        return result
    }

    private fun shouldTakeMedicationOn(
        schedule: MedicationScheduleEntity,
        targetDate: LocalDate
    ): Boolean {
        if (targetDate.isBefore(schedule.startDate)) return false
        if (schedule.restDays == 0) return true
        val daysSinceStart = schedule.startDate.until(targetDate, ChronoUnit.DAYS)
        val cycleLength = schedule.takeDays + schedule.restDays
        val dayInCycle = (daysSinceStart % cycleLength).toInt()
        return dayInCycle < schedule.takeDays
    }

    private suspend fun scheduleInitialReminder(
        recordIds: List<Long>,
        occurrences: List<LocalDateTime>,
        medicine: MedicineEntity
    ) {
        if (recordIds.isEmpty()) return
        val now = LocalDateTime.now(clock)
        val paired = occurrences.zip(recordIds)
        val next = paired.firstOrNull { it.first.isAfter(now) } ?: paired.first()
        reminderScheduler.scheduleDoseReminder(
            recordId = next.second,
            medicineId = medicine.id,
            medicineName = medicine.name,
            dosage = medicine.dosage,
            scheduledTime = next.first.toLocalTime().format(timeFormatter),
            triggerAt = next.first
        )
    }

    private fun ScheduledDoseTuple.toDomain(): ScheduledDose {
        val medicine = Medicine(
            id = medicineId,
            name = medicineName,
            dosage = medicineDosage,
            color = medicineColor,
            memo = medicineMemo,
            createdAt = Instant.ofEpochMilli(medicineCreatedAt)
        )
        return ScheduledDose(
            recordId = recordId,
            scheduleId = scheduleId,
            medicine = medicine,
            scheduledDateTime = recordScheduledDateTime,
            takenDateTime = recordTakenDateTime,
            status = when {
                recordIsTaken -> MedicationStatus.TAKEN
                recordSkipped -> MedicationStatus.SKIPPED
                else -> MedicationStatus.PENDING
            }
        )
    }

    private fun MedicineEntity.toDomain(): Medicine = Medicine(
        id = id,
        name = name,
        dosage = dosage,
        color = color,
        memo = memo,
        createdAt = Instant.ofEpochMilli(createdAt)
    )

    private fun MedicationScheduleEntity.toDomain(): MedicationSchedule = MedicationSchedule(
        id = id,
        medicineId = medicineId,
        startDate = startDate,
        endDate = endDate,
        timeSlots = timeSlots,
        takeDays = takeDays,
        restDays = restDays,
        isActive = isActive
    )

    private fun MedicationRecordEntity.toDomain(): MedicationRecord = MedicationRecord(
        id = id,
        scheduleId = scheduleId,
        scheduledDateTime = scheduledDateTime,
        takenDateTime = takenDateTime,
        status = when {
            isTaken -> MedicationStatus.TAKEN
            skipped -> MedicationStatus.SKIPPED
            else -> MedicationStatus.PENDING
        },
        note = note
    )

    private fun endOfNextMonth(referenceDate: LocalDate): LocalDate {
        val nextMonth = referenceDate.plusMonths(1)
        return nextMonth.withDayOfMonth(nextMonth.lengthOfMonth())
    }
}
