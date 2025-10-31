package com.dailydrug.data.notification

object NotificationConstants {
    const val CHANNEL_ID = "medication_reminder"
    const val CHANNEL_NAME = "복용 알림"
    const val CHANNEL_DESCRIPTION = "약 복용 알림과 재알림을 제공합니다."

    const val ALERT_NOTIFICATION_ID_BASE = 2000

    const val ACTION_REMIND = "com.dailydrug.action.REMIND"
    const val ACTION_SNOOZE = "com.dailydrug.action.SNOOZE"
    const val ACTION_TAKE = "com.dailydrug.action.TAKE"
    const val ACTION_TAKE_TODAY = "com.dailydrug.action.TAKE_TODAY"

    const val EXTRA_RECORD_ID = "extra_record_id"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_DOSAGE = "extra_dosage"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_MEDICINE_ID = "extra_medicine_id"

    const val REMINDER_INTERVAL_MILLIS = 60 * 60 * 1000L // 1 hour

    const val DAILY_SCHEDULE_WORK = "daily_schedule_work"
    const val REMINDER_WORK_PREFIX = "reminder_work_"
}
