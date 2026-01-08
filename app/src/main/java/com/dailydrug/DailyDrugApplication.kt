package com.dailydrug

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dailydrug.presentation.widget.TodayMedicationWidgetProvider
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DailyDrugApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            if (::workerFactory.isInitialized) {
                android.util.Log.i("DailyDrugApp", "✅ Configuring WorkManager with HiltWorkerFactory")
            } else {
                android.util.Log.e("DailyDrugApp", "❌ HiltWorkerFactory NOT initialized yet!")
            }
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()

        // Explicitly initialize WorkManager with Hilt configuration
        // This ensures HiltWorkerFactory is used instead of the default factory
        try {
            androidx.work.WorkManager.initialize(this, workManagerConfiguration)
            android.util.Log.i("DailyDrugApp", "✅ WorkManager initialized explicitly")
        } catch (e: Exception) {
            android.util.Log.w("DailyDrugApp", "WorkManager already initialized", e)
        }

        // 앱 시작 시 위젯 갱신 (설치/재설치 후 빈 위젯 문제 해결)
        try {
            TodayMedicationWidgetProvider.refreshAll(this)
            android.util.Log.d("DailyDrugApp", "✅ Widget refreshed on app start")
        } catch (e: Exception) {
            android.util.Log.e("DailyDrugApp", "⚠️ Failed to refresh widget on start", e)
        }
    }
}
