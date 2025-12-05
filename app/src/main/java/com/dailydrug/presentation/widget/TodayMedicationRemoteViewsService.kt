package com.dailydrug.presentation.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.dailydrug.R
import com.dailydrug.domain.model.MedicationStatus
import com.dailydrug.domain.model.ScheduledDose
import com.dailydrug.domain.repository.MedicationRepository
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class TodayMedicationRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayMedicationRemoteViewsFactory(applicationContext)
    }
}

private class TodayMedicationRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private val widgetEntryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        WidgetEntryPoint::class.java
    )

    private val medicationRepository: MedicationRepository =
        widgetEntryPoint.medicationRepository()

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private var items: List<ScheduledDose> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val today = LocalDate.now()
        items = runBlocking {
            medicationRepository.observeScheduledDoses(today)
                .firstOrNull()
                ?.filter { it.status == MedicationStatus.PENDING }
                ?.sortedBy { it.scheduledDateTime }
                ?: emptyList()
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        android.util.Log.d(TAG, "📋 Widget: Creating view for position=$position, recordId=${item.recordId}, medicine=${item.medicine.name}")

        val views = RemoteViews(context.packageName, R.layout.widget_today_medication_item)
        views.setTextViewText(R.id.widget_item_medicine, item.medicine.name)
        val scheduleText = "${item.medicine.dosage} • ${item.scheduledDateTime.toLocalTime().format(timeFormatter)}"
        views.setTextViewText(R.id.widget_item_schedule, scheduleText)
        views.setInt(R.id.widget_item_border, "setColorFilter", item.medicine.color)

        val takeIntent = TodayMedicationWidgetProvider.createToggleFillInIntent(context, item.recordId)
        android.util.Log.d(TAG, "🔘 Widget: Setting take button fillInIntent - action=${takeIntent.action}, recordId=${item.recordId}")
        views.setOnClickFillInIntent(R.id.widget_item_take_button, takeIntent)

        val openIntent = TodayMedicationWidgetProvider.createOpenAppFillInIntent(context)
        android.util.Log.d(TAG, "🔘 Widget: Setting root fillInIntent - action=${openIntent.action}")
        views.setOnClickFillInIntent(R.id.widget_item_root, openIntent)

        return views
    }

    companion object {
        private const val TAG = "WidgetRemoteViews"
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.recordId ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
