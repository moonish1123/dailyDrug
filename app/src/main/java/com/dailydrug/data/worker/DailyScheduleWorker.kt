package com.dailydrug.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailydrug.domain.usecase.EnsureScheduleOccurrencesUseCase
import com.dailydrug.domain.usecase.GetTodayMedicationsUseCase
import com.dailydrug.domain.usecase.ScheduleNotificationUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * Daily job triggered at midnight to generate the next day's reminders.
 * Generates notifications for the upcoming day leveraging the domain use cases.
 */
@HiltWorker
class DailyScheduleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getTodayMedicationsUseCase: GetTodayMedicationsUseCase,
    private val scheduleNotificationUseCase: ScheduleNotificationUseCase,
    private val ensureScheduleOccurrencesUseCase: EnsureScheduleOccurrencesUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        val targetDate = LocalDate.now(clock)

        android.util.Log.i(TAG, "========================================")
        android.util.Log.i(TAG, "🌙 DailyScheduleWorker started")
        android.util.Log.i(TAG, "Target Date: $targetDate")
        android.util.Log.i(TAG, "Day of Month: ${targetDate.dayOfMonth}")

        if (targetDate.dayOfMonth == 1) {
            val horizon = endOfNextMonth(targetDate)
            android.util.Log.i(TAG, "📅 First day of month - Ensuring schedules up to: $horizon")
            ensureScheduleOccurrencesUseCase(horizon)
            android.util.Log.i(TAG, "✅ Schedule occurrences ensured")
        }

        val doses = getTodayMedicationsUseCase(targetDate).first()
        android.util.Log.i(TAG, "📋 Found ${doses.size} medication doses for today")

        doses.forEachIndexed { index, dose ->
            android.util.Log.d(TAG, "  [$index] ${dose.medicine.name} at ${dose.scheduledDateTime.toLocalTime()}")
        }

        scheduleNotificationUseCase.scheduleAll(doses)
        android.util.Log.i(TAG, "🔔 All notifications scheduled")

        reminderScheduler.scheduleDailyRefresh()
        android.util.Log.i(TAG, "⏰ Next daily refresh scheduled")

        reminderScheduler.notifyWidgets()
        android.util.Log.i(TAG, "📱 Widget refreshed for new day")

        val duration = System.currentTimeMillis() - startTime
        android.util.Log.i(TAG, "✅ DailyScheduleWorker completed in ${duration}ms")
        android.util.Log.i(TAG, "========================================")

        return Result.success()
    }

    companion object {
        private const val TAG = "DailyScheduleWorker"
    }

    private fun endOfNextMonth(referenceDate: LocalDate): LocalDate {
        val nextMonth = referenceDate.plusMonths(1)
        return nextMonth.withDayOfMonth(nextMonth.lengthOfMonth())
    }
}
