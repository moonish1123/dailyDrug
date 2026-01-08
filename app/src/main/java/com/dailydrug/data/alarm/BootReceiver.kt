package com.dailydrug.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailydrug.util.Log
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.EntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * BroadcastReceiver that handles device boot events.
 * Reschedules all pending medication alarms after device restart.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        Log.i(TAG, "========================================")
        Log.i(TAG, "🔄 Boot received - Rescheduling alarms")
        Log.i(TAG, "========================================")

        // Initialize WorkManager with Hilt configuration if not already initialized
        // This handles the case where Application.onCreate() hasn't run yet
        try {
            initializeWorkManagerIfNeeded(context)
            Log.i(TAG, "✅ WorkManager ready")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to initialize WorkManager", e)
        }

        // Use WorkManager to handle rescheduling (more reliable than BroadcastReceiver)
        BootRescheduleWorker.enqueue(context)

        Log.i(TAG, "✅ Boot rescheduled via WorkManager")
        Log.i(TAG, "========================================")
    }

    /**
     * Initialize WorkManager with HiltWorkerFactory only if not already initialized.
     * This handles both scenarios:
     * 1. BootReceiver runs before Application (needs manual init)
     * 2. Application already initialized WorkManager (skip re-init)
     */
    private fun initializeWorkManagerIfNeeded(context: Context) {
        try {
            // Try to get WorkManager instance - this will succeed if already initialized
            androidx.work.WorkManager.getInstance(context)
            Log.d(TAG, "ℹ️ WorkManager already initialized")
        } catch (e: Exception) {
            // WorkManager not initialized yet, initialize with Hilt factory
            Log.d(TAG, "🔧 Initializing WorkManager with Hilt factory...")
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java
            )

            val config = Configuration.Builder()
                .setWorkerFactory(entryPoint.hiltWorkerFactory())
                .build()

            androidx.work.WorkManager.initialize(context, config)
        }
    }

    @EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun hiltWorkerFactory(): HiltWorkerFactory
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
