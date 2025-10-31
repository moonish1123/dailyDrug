package com.dailydrug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import com.dailydrug.presentation.DailyDrugApp
import com.dailydrug.presentation.theme.DailyDrugTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val detailNavigationRequests = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            DailyDrugTheme {
                val navigateRequest by detailNavigationRequests.asStateFlow().collectAsState()
                DailyDrugApp(
                    targetMedicineId = navigateRequest,
                    onNavigationConsumed = {
                        detailNavigationRequests.value = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(newIntent: Intent?) {
        val medicineId = newIntent?.getLongExtra(EXTRA_MEDICINE_ID, -1L) ?: -1L
        if (medicineId > 0) {
            detailNavigationRequests.value = medicineId
        }
    }

    companion object {
        private const val EXTRA_MEDICINE_ID = "extra_medicine_id"

        fun createDetailIntent(context: Context, medicineId: Long): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_MEDICINE_ID, medicineId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}
