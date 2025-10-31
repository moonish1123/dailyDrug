package com.dailydrug.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailydrug.data.alarm.MedicationAlarmReceiver
import com.dailydrug.data.notification.NotificationConstants.EXTRA_RECORD_ID
import com.dailydrug.domain.repository.MedicationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.format.DateTimeFormatter

/**
 * WorkManager fallback that re-schedules reminder alarms when the device is idle or restarted.
 * Ensures we keep sending alerts even if AlarmManager gets throttled.
 */
@HiltWorker
class MedicationReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderScheduler: ReminderScheduler,
    private val medicationRepository: MedicationRepository
) : CoroutineWorker(appContext, workerParams) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        val currentTime = java.time.LocalDateTime.now()
        val recordId = inputData.getLong(EXTRA_RECORD_ID, -1L)

        Log.i(TAG, "========================================")
        Log.i(TAG, "⏰ MedicationReminderWorker started")
        Log.i(TAG, "RecordId: $recordId")
        Log.i(TAG, "Current Time: $currentTime")

        if (recordId <= 0) {
            Log.e(TAG, "❌ Invalid recordId - Worker failed")
            Log.i(TAG, "========================================")
            return Result.failure()
        }

        val dose = medicationRepository.getScheduledDose(recordId)
        if (dose == null) {
            Log.w(TAG, "⚠️ Dose not found in DB - Retrying later")
            Log.i(TAG, "========================================")
            return Result.retry()
        }

        Log.i(TAG, "📋 Dose Information:")
        Log.i(TAG, "  Medicine: ${dose.medicine.name}")
        Log.i(TAG, "  Dosage: ${dose.medicine.dosage}")
        Log.i(TAG, "  Scheduled Time: ${dose.scheduledDateTime.toLocalTime().format(timeFormatter)}")
        Log.i(TAG, "  Medicine ID: ${dose.medicine.id}")

        val fallbackIntent = MedicationAlarmReceiver.createIntent(
            context = applicationContext,
            recordId = recordId,
            medicineName = dose.medicine.name,
            dosage = dose.medicine.dosage,
            scheduledTime = dose.scheduledDateTime.toLocalTime().format(timeFormatter),
            medicineId = dose.medicine.id
        )

        Log.i(TAG, "🔄 Scheduling re-alert via ReminderScheduler")
        reminderScheduler.scheduleRealert(recordId, fallbackIntent)

        val duration = System.currentTimeMillis() - startTime
        Log.i(TAG, "✅ MedicationReminderWorker completed in ${duration}ms")
        Log.i(TAG, "========================================")

        return Result.success()
    }

    companion object {
        private const val TAG = "MedicationReminderWorker"
    }
}
