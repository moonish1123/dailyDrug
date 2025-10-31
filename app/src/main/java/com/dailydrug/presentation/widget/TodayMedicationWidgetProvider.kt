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
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_TAKEN -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                if (recordId > 0) {
                    handleMarkAsTaken(context, recordId)
                }
            }
            ACTION_REFRESH -> refreshAll(context)
        }
    }

    private fun handleMarkAsTaken(context: Context, recordId: Long) {
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
                refreshAll(context)
            }.onFailure {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "복용 처리에 실패했어요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val ACTION_TOGGLE_TAKEN = "com.dailydrug.widget.TOGGLE_TAKEN"
        private const val ACTION_REFRESH = "com.dailydrug.widget.REFRESH"
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

            val toggleIntent = Intent(context, TodayMedicationWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TAKEN
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                flags
            )
            views.setPendingIntentTemplate(R.id.widget_list, pendingIntent)
            return views
        }

        fun createFillInIntent(recordId: Long): Intent {
            return Intent().apply {
                putExtras(bundleOf(EXTRA_RECORD_ID to recordId))
            }
        }
    }
}
