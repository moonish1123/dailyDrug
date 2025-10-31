package com.dailydrug.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailydrug.data.alarm.MedicationAlarmReceiver
import com.dailydrug.data.notification.NotificationConstants.EXTRA_RECORD_ID
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager fallback that re-schedules reminder alarms when the device is idle or restarted.
 * Ensures we keep sending alerts even if AlarmManager gets throttled.
 */
@HiltWorker
class MedicationReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val recordId = inputData.getLong(EXTRA_RECORD_ID, -1L)
        if (recordId <= 0) {
            Log.w(TAG, "MedicationReminderWorker missing recordId")
            return Result.failure()
        }

        Log.d(TAG, "MedicationReminderWorker scheduling re-alert for recordId=$recordId")
        val fallbackIntent = MedicationAlarmReceiver.createIntent(
            context = applicationContext,
            recordId = recordId,
            medicineName = "",
            dosage = "",
            scheduledTime = ""
        )
        reminderScheduler.scheduleRealert(recordId, fallbackIntent)
        return Result.success()
    }

    companion object {
        private const val TAG = "MedicationReminderWorker"
    }
}
