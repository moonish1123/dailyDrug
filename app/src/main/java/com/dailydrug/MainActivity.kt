package com.dailydrug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.dailydrug.presentation.DailyDrugApp
import com.dailydrug.presentation.theme.DailyDrugTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyDrugTheme {
                DailyDrugApp()
            }
        }
    }

    companion object {
        private const val EXTRA_RECORD_ID = "extra_record_id"

        fun createDetailIntent(context: Context, recordId: Long): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_RECORD_ID, recordId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}
