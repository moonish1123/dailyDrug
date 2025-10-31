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
        val targetDate = LocalDate.now(clock)
        if (targetDate.dayOfMonth == 1) {
            ensureScheduleOccurrencesUseCase(endOfNextMonth(targetDate))
        }
        val doses = getTodayMedicationsUseCase(targetDate).first()
        scheduleNotificationUseCase.scheduleAll(doses)
        reminderScheduler.scheduleDailyRefresh()
        return Result.success()
    }

    private fun endOfNextMonth(referenceDate: LocalDate): LocalDate {
        val nextMonth = referenceDate.plusMonths(1)
        return nextMonth.withDayOfMonth(nextMonth.lengthOfMonth())
    }
}
