package com.dailydrug.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dailydrug.data.notification.NotificationConstants
import com.dailydrug.data.notification.NotificationConstants.ACTION_REMIND
import com.dailydrug.data.notification.NotificationConstants.ACTION_SNOOZE
import com.dailydrug.data.notification.NotificationConstants.ACTION_TAKE
import com.dailydrug.data.notification.NotificationConstants.ACTION_TAKE_TODAY
import com.dailydrug.data.notification.NotificationConstants.EXTRA_DOSAGE
import com.dailydrug.data.notification.NotificationConstants.EXTRA_MEDICINE_NAME
import com.dailydrug.data.notification.NotificationConstants.EXTRA_RECORD_ID
import com.dailydrug.data.notification.NotificationConstants.EXTRA_SCHEDULED_TIME
import com.dailydrug.data.notification.NotificationHelper
import com.dailydrug.data.worker.ReminderScheduler
import com.dailydrug.domain.usecase.RecordMedicationUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MedicationAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var recordMedicationUseCase: RecordMedicationUseCase

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        if (recordId <= 0) {
            Log.w(TAG, "Invalid recordId received in alarm intent")
            return
        }

        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME).orEmpty()
        val dosage = intent.getStringExtra(EXTRA_DOSAGE).orEmpty()
        val scheduledTime = intent.getStringExtra(EXTRA_SCHEDULED_TIME).orEmpty()

        when (intent.action) {
            ACTION_REMIND -> {
                NotificationHelper(context).showReminder(
                    recordId = recordId,
                    medicineName = medicineName,
                    dosage = dosage,
                    scheduledTime = scheduledTime
                )
                reminderScheduler.scheduleRealert(recordId, intent)
            }
            ACTION_SNOOZE -> {
                reminderScheduler.scheduleRealert(recordId, intent)
                Log.d(TAG, "Snooze requested for recordId=$recordId")
            }
            ACTION_TAKE -> {
                Log.d(TAG, "Marked as taken via notification action for recordId=$recordId")
                handleTakeAction(recordId, context, dismissNotification = true)
            }
            ACTION_TAKE_TODAY -> {
                Log.d(TAG, "Marked as taken (오늘 약 먹음) for recordId=$recordId")
                handleTakeAction(recordId, context, dismissNotification = true)
            }
            else -> {
                Log.w(TAG, "Unknown action ${intent.action}")
            }
        }
    }

    private fun handleTakeAction(recordId: Long, context: Context, dismissNotification: Boolean) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                recordMedicationUseCase(
                    RecordMedicationUseCase.Params(
                        recordId = recordId,
                        markAsTaken = true
                    )
                )
                reminderScheduler.cancelReminder(recordId)
                if (dismissNotification) {
                    NotificationHelper(context).dismissReminder(recordId)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to mark medication as taken", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "MedicationAlarmReceiver"

        fun createIntent(
            context: Context,
            action: String = ACTION_REMIND,
            recordId: Long,
            medicineName: String,
            dosage: String,
            scheduledTime: String
        ): Intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_RECORD_ID, recordId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
        }

        fun createPendingIntent(
            context: Context,
            action: String,
            recordId: Long,
            medicineName: String,
            dosage: String,
            scheduledTime: String,
            flags: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ): PendingIntent {
            val intent = createIntent(
                context = context,
                action = action,
                recordId = recordId,
                medicineName = medicineName,
                dosage = dosage,
                scheduledTime = scheduledTime
            )
            return PendingIntent.getBroadcast(
                context,
                (action + recordId).hashCode(),
                intent,
                flags
            )
        }

        fun cancel(context: Context, recordId: Long) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val intent = createIntent(
                context = context,
                recordId = recordId,
                medicineName = "",
                dosage = "",
                scheduledTime = "",
                action = ACTION_REMIND
            )
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (ACTION_REMIND + recordId).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.cancel(pendingIntent)
        }
    }
}
