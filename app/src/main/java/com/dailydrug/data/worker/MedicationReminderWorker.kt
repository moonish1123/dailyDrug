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
        val recordId = inputData.getLong(EXTRA_RECORD_ID, -1L)
        if (recordId <= 0) {
            Log.w(TAG, "MedicationReminderWorker missing recordId")
            return Result.failure()
        }

        Log.d(TAG, "MedicationReminderWorker scheduling re-alert for recordId=$recordId")
        val dose = medicationRepository.getScheduledDose(recordId)
            ?: return Result.retry()
        val fallbackIntent = MedicationAlarmReceiver.createIntent(
            context = applicationContext,
            recordId = recordId,
            medicineName = dose.medicine.name,
            dosage = dose.medicine.dosage,
            scheduledTime = dose.scheduledDateTime.toLocalTime().format(timeFormatter),
            medicineId = dose.medicine.id
        )
        reminderScheduler.scheduleRealert(recordId, fallbackIntent)
        return Result.success()
    }

    companion object {
        private const val TAG = "MedicationReminderWorker"
    }
}
