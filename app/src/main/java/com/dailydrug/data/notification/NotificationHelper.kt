package com.dailydrug.data.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import com.dailydrug.R
import com.dailydrug.data.notification.NotificationConstants.ACTION_TAKE
import com.dailydrug.data.notification.NotificationConstants.ACTION_SNOOZE
import com.dailydrug.data.notification.NotificationConstants.ACTION_TAKE_TODAY
import com.dailydrug.data.notification.NotificationConstants.ACTION_REMIND
import com.dailydrug.data.notification.NotificationConstants.ALERT_NOTIFICATION_ID_BASE
import com.dailydrug.data.notification.NotificationConstants.CHANNEL_DESCRIPTION
import com.dailydrug.data.notification.NotificationConstants.CHANNEL_ID
import com.dailydrug.data.notification.NotificationConstants.CHANNEL_NAME
import com.dailydrug.data.notification.NotificationConstants.EXTRA_DOSAGE
import com.dailydrug.data.notification.NotificationConstants.EXTRA_MEDICINE_NAME
import com.dailydrug.data.notification.NotificationConstants.EXTRA_RECORD_ID
import com.dailydrug.data.notification.NotificationConstants.EXTRA_SCHEDULED_TIME
import com.dailydrug.data.notification.NotificationConstants.ACTION_DISMISS_ALARM_UI
import com.dailydrug.data.alarm.MedicationAlarmReceiver
import com.dailydrug.presentation.alarm.MedicationAlarmActivity

class NotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManagerCompat
        get() = NotificationManagerCompat.from(context)

    companion object {
        private const val TAG = "NotificationHelper"

        fun Context.ensureNotificationChannel() = NotificationHelper(this).ensureChannel()
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val vibrationPattern = longArrayOf(0L, 400L, 200L, 400L)

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                this.vibrationPattern = vibrationPattern
                setSound(
                    defaultSound,
                    audioAttributes
                )
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            val existingChannel = manager?.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                manager?.createNotificationChannel(channel)
            } else {
                val needsUpdate =
                    existingChannel.importance < NotificationManager.IMPORTANCE_HIGH ||
                        existingChannel.vibrationPattern?.contentEquals(vibrationPattern) == false ||
                        existingChannel.sound != defaultSound
                if (needsUpdate) {
                    manager?.deleteNotificationChannel(CHANNEL_ID)
                    manager?.createNotificationChannel(channel)
                }
            }
        }
    }

    fun showReminder(
        recordId: Long,
        medicineId: Long,
        medicineName: String,
        dosage: String,
        scheduledTime: String
    ) {
        val currentTime = java.time.LocalTime.now()
        android.util.Log.i(TAG, "========================================")
        android.util.Log.i(TAG, "🔔 Showing notification")
        android.util.Log.i(TAG, "RecordId: $recordId")
        android.util.Log.i(TAG, "Medicine: $medicineName ($dosage)")
        android.util.Log.i(TAG, "Scheduled Time: $scheduledTime")
        android.util.Log.i(TAG, "Display Time: $currentTime")
        android.util.Log.i(TAG, "NotificationId: ${notificationId(recordId)}")

        ensureChannel()

        // Check notification permission
        if (!notificationManager.areNotificationsEnabled()) {
            android.util.Log.w(TAG, "⚠️ Notifications are disabled by user")
            return
        }

        val notification = buildReminderNotification(
            recordId = recordId,
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = scheduledTime
        )
        @SuppressLint("MissingPermission") // Permission checked via areNotificationsEnabled()
        notificationManager.notify(notificationId(recordId), notification)

        android.util.Log.i(TAG, "✅ Notification displayed successfully")
        android.util.Log.i(TAG, "========================================")
    }

    fun dismissReminder(recordId: Long) {
        notificationManager.cancel(notificationId(recordId))
        context.sendBroadcast(
            Intent(ACTION_DISMISS_ALARM_UI).putExtra(EXTRA_RECORD_ID, recordId)
        )
    }

    private fun buildReminderNotification(
        recordId: Long,
        medicineId: Long,
        medicineName: String,
        dosage: String,
        scheduledTime: String
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            recordId.toInt(),
            MedicationAlarmActivity.createIntent(
                context = context,
                recordId = recordId,
                medicineId = medicineId,
                medicineName = medicineName,
                dosage = dosage,
                scheduledTime = scheduledTime
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            "snooze_$recordId".hashCode(),
            MedicationAlarmReceiver.createIntent(
                context = context,
                action = ACTION_SNOOZE,
                recordId = recordId,
                medicineName = medicineName,
                dosage = dosage,
                scheduledTime = scheduledTime,
                medicineId = medicineId
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takeIntent = PendingIntent.getBroadcast(
            context,
            "take_$recordId".hashCode(),
            MedicationAlarmReceiver.createIntent(
                context = context,
                action = ACTION_TAKE,
                recordId = recordId,
                medicineName = medicineName,
                dosage = dosage,
                scheduledTime = scheduledTime,
                medicineId = medicineId
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takeTodayIntent = PendingIntent.getBroadcast(
            context,
            "take_today_$recordId".hashCode(),
            MedicationAlarmReceiver.createIntent(
                context = context,
                action = ACTION_TAKE_TODAY,
                recordId = recordId,
                medicineName = medicineName,
                dosage = dosage,
                scheduledTime = scheduledTime,
                medicineId = medicineId
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val vibrationPattern = longArrayOf(0L, 400L, 200L, 400L)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$medicineName 복용 시간입니다")
            .setContentText("$dosage • 예정 시간: $scheduledTime")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$dosage 복용 예정 ($scheduledTime)\n복용 완료 시 바로 표시해주세요.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(defaultSound)
            .setVibrate(vibrationPattern)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    "1시간 후 알림",
                    snoozeIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    "오늘 약 먹음",
                    takeTodayIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    "복용 완료",
                    takeIntent
                ).build()
            )
            .setFullScreenIntent(
                PendingIntent.getActivity(
                    context,
                    ("full_screen_$recordId").hashCode(),
                    MedicationAlarmActivity.createIntent(
                        context = context,
                        recordId = recordId,
                        medicineId = medicineId,
                        medicineName = medicineName,
                        dosage = dosage,
                        scheduledTime = scheduledTime
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ),
                true
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addExtras(
                androidx.core.os.bundleOf(
                    EXTRA_RECORD_ID to recordId,
                    EXTRA_MEDICINE_NAME to medicineName,
                    EXTRA_DOSAGE to dosage,
                    EXTRA_SCHEDULED_TIME to scheduledTime
                )
            )
            .build()
    }

    private fun notificationId(recordId: Long): Int =
        ALERT_NOTIFICATION_ID_BASE + (recordId % Int.MAX_VALUE).toInt()
}
