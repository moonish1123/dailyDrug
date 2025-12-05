package com.dailydrug.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.os.bundleOf
import com.dailydrug.R
import com.dailydrug.domain.usecase.RecordMedicationUseCase
import com.dailydrug.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodayMedicationWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = createRemoteViews(context, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "📱 Widget: onReceive called")
        android.util.Log.d(TAG, "Action: ${intent.action}")
        android.util.Log.d(TAG, "Extras: ${intent.extras?.keySet()?.joinToString()}")

        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_TAKEN -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                android.util.Log.i(TAG, "✅ ACTION_TOGGLE_TAKEN received, recordId=$recordId")
                if (recordId > 0) {
                    handleMarkAsTaken(context, recordId)
                } else {
                    android.util.Log.e(TAG, "❌ Invalid recordId: $recordId")
                }
            }
            ACTION_REFRESH -> {
                android.util.Log.i(TAG, "🔄 ACTION_REFRESH received")
                refreshAll(context)
            }
            ACTION_OPEN_APP -> {
                android.util.Log.i(TAG, "🚀 ACTION_OPEN_APP received")
                openMainScreen(context)
            }
            else -> {
                android.util.Log.w(TAG, "⚠️ Unknown action: ${intent.action}")
            }
        }
        android.util.Log.d(TAG, "========================================")
    }

    private fun openMainScreen(context: Context) {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            ?: Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        context.startActivity(launchIntent)
    }

    private fun handleMarkAsTaken(context: Context, recordId: Long) {
        android.util.Log.i(TAG, "========================================")
        android.util.Log.i(TAG, "💊 Widget: Mark as taken requested")
        android.util.Log.i(TAG, "RecordId: $recordId")

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val recordUseCase = entryPoint.recordMedicationUseCase()

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                recordUseCase(
                    RecordMedicationUseCase.Params(
                        recordId = recordId,
                        markAsTaken = true
                    )
                )
                android.util.Log.i(TAG, "✅ Widget: Medication recorded successfully")

                refreshAll(context)
                android.util.Log.i(TAG, "🔄 Widget: Refresh completed")

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "복용 완료!", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                android.util.Log.e(TAG, "❌ Widget: Failed to record medication", error)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "복용 처리에 실패했어요.", Toast.LENGTH_SHORT).show()
                }
            }
            android.util.Log.i(TAG, "========================================")
        }
    }

    companion object {
        private const val TAG = "WidgetProvider"
        internal const val ACTION_TOGGLE_TAKEN = "com.dailydrug.widget.TOGGLE_TAKEN"
        internal const val ACTION_REFRESH = "com.dailydrug.widget.REFRESH"
        internal const val ACTION_OPEN_APP = "com.dailydrug.widget.OPEN_APP"
        const val EXTRA_RECORD_ID = "extra_record_id"

        fun refreshAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TodayMedicationWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            if (appWidgetIds.isEmpty()) return
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
            appWidgetIds.forEach { id ->
                val views = createRemoteViews(context, id)
                appWidgetManager.updateAppWidget(id, views)
            }
        }

        private fun createRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_today_medication)
            val serviceIntent = Intent(context, TodayMedicationRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("dailydrug://widget/$appWidgetId")
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // PendingIntent template for list items
            // Component를 명시적으로 설정하고 FLAG_MUTABLE 사용 (fillInIntent와 병합 가능하도록)
            val templateIntent = Intent(context, TodayMedicationWidgetProvider::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                templateIntent,
                flags
            )
            views.setPendingIntentTemplate(R.id.widget_list, pendingIntent)
            return views
        }

        fun createToggleFillInIntent(context: Context, recordId: Long): Intent =
            Intent(ACTION_TOGGLE_TAKEN).apply {
                setClass(context, TodayMedicationWidgetProvider::class.java)
                putExtras(bundleOf(EXTRA_RECORD_ID to recordId))
            }

        fun createOpenAppFillInIntent(context: Context): Intent =
            Intent(ACTION_OPEN_APP).apply {
                setClass(context, TodayMedicationWidgetProvider::class.java)
            }
    }
}
