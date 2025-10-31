package com.dailydrug.data.worker

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dailydrug.data.notification.NotificationConstants
import com.dailydrug.data.notification.NotificationConstants.ACTION_REMIND
import com.dailydrug.data.notification.NotificationConstants.EXTRA_DOSAGE
import com.dailydrug.data.notification.NotificationConstants.EXTRA_MEDICINE_NAME
import com.dailydrug.data.notification.NotificationConstants.EXTRA_RECORD_ID
import com.dailydrug.data.notification.NotificationConstants.EXTRA_SCHEDULED_TIME
import com.dailydrug.data.notification.NotificationConstants.REMINDER_INTERVAL_MILLIS
import com.dailydrug.data.notification.NotificationHelper
import com.dailydrug.data.alarm.MedicationAlarmReceiver
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ReminderScheduler(
    private val context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context),
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)
) {

    fun scheduleDailyRefresh() {
        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val delayMillis = Duration.between(now, nextMidnight).toMillis()

        val request = PeriodicWorkRequestBuilder<DailyScheduleWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            NotificationConstants.DAILY_SCHEDULE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleDoseReminder(
        recordId: Long,
        medicineName: String,
        dosage: String,
        scheduledTime: String,
        triggerAt: LocalDateTime
    ) {
        NotificationHelper(context).dismissReminder(recordId)
        MedicationAlarmReceiver.cancel(context, recordId)
        NotificationHelper(context).ensureChannel()
        scheduleAlarm(
            recordId = recordId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = scheduledTime,
            triggerAt = triggerAt
        )
        scheduleReminderWorker(recordId, triggerAt)
    }

    fun scheduleRealert(recordId: Long, originalIntent: Intent) {
        val triggerAtMillis = System.currentTimeMillis() + REMINDER_INTERVAL_MILLIS
        val triggerAt = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(triggerAtMillis),
            ZoneId.systemDefault()
        )
        val medicineName = originalIntent.getStringExtra(EXTRA_MEDICINE_NAME).orEmpty()
        val dosage = originalIntent.getStringExtra(EXTRA_DOSAGE).orEmpty()
        val scheduledTime = originalIntent.getStringExtra(EXTRA_SCHEDULED_TIME).orEmpty()
        scheduleDoseReminder(
            recordId = recordId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = scheduledTime,
            triggerAt = triggerAt
        )
    }

    fun cancelReminder(recordId: Long) {
        MedicationAlarmReceiver.cancel(context, recordId)
        workManager.cancelUniqueWork(NotificationConstants.REMINDER_WORK_PREFIX + recordId)
    }

    private fun scheduleAlarm(
        recordId: Long,
        medicineName: String,
        dosage: String,
        scheduledTime: String,
        triggerAt: LocalDateTime
    ) {
        val triggerAtMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pendingIntent = MedicationAlarmReceiver.createPendingIntent(
            context = context,
            action = ACTION_REMIND,
            recordId = recordId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = scheduledTime
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager?.canScheduleExactAlarms() != true) {
                // fall back to WorkManager re-alert
                scheduleReminderWorker(recordId, triggerAt)
                return
            }
        }

        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private fun scheduleReminderWorker(
        recordId: Long,
        triggerAt: LocalDateTime
    ) {
        val delay = Duration.between(
            LocalDateTime.now(),
            triggerAt
        ).let { if (it.isNegative) Duration.ZERO else it }

        val request = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(EXTRA_RECORD_ID, recordId)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            NotificationConstants.REMINDER_WORK_PREFIX + recordId,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
