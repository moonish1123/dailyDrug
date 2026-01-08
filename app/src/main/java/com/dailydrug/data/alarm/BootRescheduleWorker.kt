package com.dailydrug.data.alarm

import android.content.Context
import android.content.Intent
import com.dailydrug.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailydrug.data.worker.ReminderScheduler
import com.dailydrug.domain.model.MedicationStatus
import com.dailydrug.domain.model.ScheduledDose
import com.dailydrug.domain.repository.MedicationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that reschedules medication alarms after device boot.
 * Uses manual dependency injection via EntryPoint to avoid HiltWorkerFactory initialization issues.
 */
class BootRescheduleWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootRescheduleWorkerEntryPoint {
        fun medicationRepository(): MedicationRepository
        fun reminderScheduler(): ReminderScheduler
    }

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            BootRescheduleWorkerEntryPoint::class.java
        )
        val medicationRepository = entryPoint.medicationRepository()
        val reminderScheduler = entryPoint.reminderScheduler()

        val startTime = System.currentTimeMillis()

        Log.i(TAG, "========================================")
        Log.i(TAG, "🔄 BootRescheduleWorker started")
        Log.i(TAG, "Current Time: ${LocalDateTime.now()}")

        // Small delay to ensure database is fully initialized
        kotlinx.coroutines.delay(2000)

        val today = LocalDate.now()
        Log.i(TAG, "📅 Target Date: $today")

        try {
            // Get all pending doses for today and future
            val doses: List<ScheduledDose> = medicationRepository.observeScheduledDoses(today)
                .first()
                .filter { it.status == MedicationStatus.PENDING }

            Log.i(TAG, "📋 Found ${doses.size} pending doses to reschedule")

            if (doses.isEmpty()) {
                Log.i(TAG, "⚠️ No pending doses found")
                Log.i(TAG, "========================================")
                return Result.success()
            }

            // Reschedule each pending dose
            var rescheduledCount = 0
            var skippedCount = 0

            for (dose: ScheduledDose in doses) {
                val scheduledTime = dose.scheduledDateTime
                val now = LocalDateTime.now()

                if (scheduledTime.isBefore(now.minusMinutes(5))) {
                    // Skip doses that are more than 5 minutes in the past
                    Log.d(TAG, "⏭️ Skipping past dose: ${dose.medicine.name} at ${scheduledTime.toLocalTime()}")
                    skippedCount++
                } else {
                    // Schedule the dose reminder
                    reminderScheduler.scheduleDoseReminder(
                        recordId = dose.recordId,
                        medicineId = dose.medicine.id,
                        medicineName = dose.medicine.name,
                        dosage = dose.medicine.dosage,
                        scheduledTime = scheduledTime.toLocalTime().format(timeFormatter),
                        triggerAt = scheduledTime
                    )
                    rescheduledCount++
                    Log.d(TAG, "✅ Rescheduled: ${dose.medicine.name} at ${scheduledTime.toLocalTime()}")
                }
            }

            // Ensure daily refresh worker is scheduled
            reminderScheduler.scheduleDailyRefresh()
            Log.i(TAG, "⏰ Daily refresh worker scheduled")

            // Refresh widgets
            reminderScheduler.notifyWidgets()
            Log.i(TAG, "📱 Widgets refreshed")

            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "📊 Reschedule Summary:")
            Log.i(TAG, "  - Total pending: ${doses.size}")
            Log.i(TAG, "  - Rescheduled: $rescheduledCount")
            Log.i(TAG, "  - Skipped (past): $skippedCount")
            Log.i(TAG, "  - Duration: ${duration}ms")
            Log.i(TAG, "✅ BootRescheduleWorker completed successfully")
            Log.i(TAG, "========================================")

            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ BootRescheduleWorker failed", e)
            Log.i(TAG, "========================================")
            // Return retry to try again later
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "BootRescheduleWorker"
        private const val WORK_NAME = "boot_reschedule_work"

        fun enqueue(context: Context) {
            val workManager = androidx.work.WorkManager.getInstance(context)
            val request = androidx.work.OneTimeWorkRequestBuilder<BootRescheduleWorker>()
                .setInitialDelay(5, java.util.concurrent.TimeUnit.SECONDS) // Wait 5 seconds for system to stabilize
                .build()

            workManager.enqueueUniqueWork(
                WORK_NAME,
                androidx.work.ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun createIntent(context: Context): Intent {
            return Intent(context, BootRescheduleWorker::class.java)
        }
    }
}
