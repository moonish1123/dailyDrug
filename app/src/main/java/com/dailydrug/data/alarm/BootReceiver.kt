package com.dailydrug.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

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

        // Use WorkManager to handle rescheduling (more reliable than BroadcastReceiver)
        BootRescheduleWorker.enqueue(context)

        Log.i(TAG, "✅ Boot rescheduled via WorkManager")
        Log.i(TAG, "========================================")
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
