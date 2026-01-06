package com.dailydrug.presentation.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.dailydrug.R
import com.dailydrug.domain.model.MedicationStatus
import com.dailydrug.domain.model.MedicationTimePeriod
import com.dailydrug.domain.model.ScheduledDose
import com.dailydrug.domain.repository.MedicationRepository
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * 위젯 리스트 아이템 타입
 */
sealed class WidgetItem {
    data class HeaderItem(
        val period: MedicationTimePeriod,
        val count: Int
    ) : WidgetItem()

    data class MedicationItem(
        val dose: ScheduledDose
    ) : WidgetItem()
}

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
    private var items: List<WidgetItem> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "🔄 Widget: onDataSetChanged called")

        try {
            val today = LocalDate.now()
            android.util.Log.d(TAG, "📅 Today: $today")

            val doses = runBlocking {
                try {
                    withTimeout(5.seconds) {
                        android.util.Log.d(TAG, "📡 Fetching doses from repository...")
                        val result = medicationRepository.observeScheduledDoses(today)
                            .firstOrNull()
                        android.util.Log.d(TAG, "📊 Repository returned: ${result?.size ?: 0} doses")
                        result
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "❌ Error fetching doses", e)
                    emptyList()
                }
            } ?: emptyList()

            android.util.Log.d(TAG, "📋 Widget: Loaded ${doses.size} doses for $today")
            doses.forEach { dose ->
                android.util.Log.d(TAG, "  - ${dose.medicine.name} at ${dose.scheduledDateTime.toLocalTime()} (${dose.status})")
            }

            // 시간대별로 그룹화
            items = buildWidgetItems(doses)
            android.util.Log.d(TAG, "✅ Widget items created: ${items.size}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error in onDataSetChanged", e)
            items = emptyList()
        }
        android.util.Log.d(TAG, "========================================")
    }

    /**
     * 시간대별로 그룹화하여 위젯 아이템 리스트 생성
     * 복용 완료하거나 건너뛴 약은 표시하지 않음
     */
    private fun buildWidgetItems(doses: List<ScheduledDose>): List<WidgetItem> {
        val result = mutableListOf<WidgetItem>()

        // 복용 완료 및 건너뛴 약 필터링
        val pendingDoses = doses.filter {
            it.status != MedicationStatus.TAKEN && it.status != MedicationStatus.SKIPPED
        }

        MedicationTimePeriod.sortedValues().forEach { period ->
            val periodDoses = pendingDoses.filter { dose ->
                MedicationTimePeriod.fromTime(dose.scheduledDateTime.toLocalTime()) == period
            }

            if (periodDoses.isNotEmpty()) {
                // 헤더 추가
                result.add(WidgetItem.HeaderItem(period, periodDoses.size))

                // 약 아이템 추가
                periodDoses.forEach { dose ->
                    result.add(WidgetItem.MedicationItem(dose))
                }
            }
        }

        return result
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]

        return when (item) {
            is WidgetItem.HeaderItem -> createHeaderView(item)
            is WidgetItem.MedicationItem -> createMedicationItemView(item)
        }
    }

    /**
     * 시간대 헤더 뷰 생성
     */
    private fun createHeaderView(headerItem: WidgetItem.HeaderItem): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_time_period_header)
        views.setTextViewText(R.id.widget_period_text, headerItem.period.displayName)
        return views
    }

    /**
     * 약 아이템 뷰 생성
     */
    private fun createMedicationItemView(medicationItem: WidgetItem.MedicationItem): RemoteViews {
        val item = medicationItem.dose
        android.util.Log.d(TAG, "📋 Widget: Creating view for recordId=${item.recordId}, medicine=${item.medicine.name}, status=${item.status}")

        val views = RemoteViews(context.packageName, R.layout.widget_today_medication_item)
        views.setTextViewText(R.id.widget_item_medicine, item.medicine.name)
        val scheduleText = "${item.medicine.dosage} • ${item.scheduledDateTime.toLocalTime().format(timeFormatter)}"
        views.setTextViewText(R.id.widget_item_schedule, scheduleText)
        views.setInt(R.id.widget_item_border, "setColorFilter", item.medicine.color)

        // 상태에 따른 UI 설정
        when (item.status) {
            MedicationStatus.TAKEN -> {
                // 복용 완료 상태
                views.setTextViewText(R.id.widget_item_status, "✓ 완료")
                views.setViewVisibility(R.id.widget_item_status, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_item_take_button, android.view.View.GONE)

                // 복용 시간 표시
                item.takenDateTime?.let { takenTime ->
                    views.setTextViewText(
                        R.id.widget_item_taken_time,
                        "복용: ${takenTime.toLocalTime().format(timeFormatter)}"
                    )
                    views.setViewVisibility(R.id.widget_item_taken_time, android.view.View.VISIBLE)
                }
            }
            MedicationStatus.SKIPPED -> {
                // 건너뜀 상태
                views.setTextViewText(R.id.widget_item_status, "✗ 건너뜀")
                views.setViewVisibility(R.id.widget_item_status, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_item_take_button, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_item_taken_time, android.view.View.GONE)
            }
            MedicationStatus.PENDING -> {
                // 복용 예정 상태
                views.setViewVisibility(R.id.widget_item_status, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_item_take_button, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_item_taken_time, android.view.View.GONE)

                val takeIntent = TodayMedicationWidgetProvider.createToggleFillInIntent(context, item.recordId)
                android.util.Log.d(TAG, "🔘 Widget: Setting take button fillInIntent - action=${takeIntent.action}, recordId=${item.recordId}")
                views.setOnClickFillInIntent(R.id.widget_item_take_button, takeIntent)
            }
        }

        val openIntent = TodayMedicationWidgetProvider.createOpenAppFillInIntent(context)
        android.util.Log.d(TAG, "🔘 Widget: Setting root fillInIntent - action=${openIntent.action}")
        views.setOnClickFillInIntent(R.id.widget_item_root, openIntent)

        return views
    }

    companion object {
        private const val TAG = "WidgetRemoteViews"
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2  // Header, MedicationItem 두 가지 타입

    override fun getItemId(position: Int): Long = when (val item = items.getOrNull(position)) {
        is WidgetItem.MedicationItem -> item.dose.recordId
        is WidgetItem.HeaderItem -> ("header_${item.period.name}").hashCode().toLong()
        null -> position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
