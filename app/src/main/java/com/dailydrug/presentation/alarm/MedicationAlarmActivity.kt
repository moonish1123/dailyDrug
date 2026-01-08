package com.dailydrug.presentation.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.dailydrug.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailydrug.MainActivity
import com.dailydrug.data.alarm.MedicationAlarmReceiver
import com.dailydrug.data.notification.NotificationConstants.ACTION_DISMISS_ALARM_UI
import com.dailydrug.data.notification.NotificationConstants.ACTION_SNOOZE
import com.dailydrug.data.notification.NotificationConstants.ACTION_TAKE
import com.dailydrug.data.notification.NotificationConstants.ACTION_TAKE_TODAY
import com.dailydrug.data.notification.NotificationConstants.EXTRA_DOSAGE
import com.dailydrug.data.notification.NotificationConstants.EXTRA_MEDICINE_ID
import com.dailydrug.data.notification.NotificationConstants.EXTRA_MEDICINE_NAME
import com.dailydrug.data.notification.NotificationConstants.EXTRA_RECORD_ID
import com.dailydrug.data.notification.NotificationConstants.EXTRA_SCHEDULED_TIME
import com.dailydrug.presentation.theme.DailyDrugTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

class MedicationAlarmActivity : ComponentActivity() {

    private val alarmState: MutableState<AlarmPayload> = mutableStateOf(AlarmPayload())

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isReceiverRegistered = false
    private var isForeground = false

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val targetRecord = intent?.getLongExtra(EXTRA_RECORD_ID, -1L) ?: -1L
            val currentRecord = alarmState.value.recordId
            if (targetRecord == -1L || targetRecord == currentRecord) {
                finishAlarm()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyWindowFlags()

        if (!updateAlarmDataFromIntent(intent)) {
            finish()
            return
        }

        setContent {
            DailyDrugTheme {
                val payload by alarmState
                Surface(modifier = Modifier.fillMaxSize()) {
                    MedicationAlarmScreen(
                        payload = payload,
                        onTake = { handleAction(ACTION_TAKE) },
                        onTakeToday = { handleAction(ACTION_TAKE_TODAY) },
                        onSnooze = { handleAction(ACTION_SNOOZE) },
                        onViewDetails = { openDetailScreen() },
                        onDismiss = { finishAlarm() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        setIntent(intent)
        val updated = updateAlarmDataFromIntent(intent)
        if (!updated) {
            finishAlarm()
        }
    }

    override fun onStart() {
        super.onStart()
        registerDismissReceiver()
        isForeground = true
        startAlert()
    }

    override fun onStop() {
        super.onStop()
        isForeground = false
        stopAlert()
        unregisterDismissReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlert()
    }

    private fun applyWindowFlags() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun updateAlarmDataFromIntent(intent: Intent): Boolean {
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        if (recordId <= 0L) {
            Log.w(TAG, "Invalid recordId for MedicationAlarmActivity")
            return false
        }

        val payload = AlarmPayload(
            recordId = recordId,
            medicineId = intent.getLongExtra(EXTRA_MEDICINE_ID, -1L),
            medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME).orEmpty(),
            dosage = intent.getStringExtra(EXTRA_DOSAGE).orEmpty(),
            scheduledTime = intent.getStringExtra(EXTRA_SCHEDULED_TIME).orEmpty()
        )

        alarmState.value = payload
        if (isForeground) {
            restartAlert()
        }
        return true
    }

    private fun registerDismissReceiver() {
        if (!isReceiverRegistered) {
            registerReceiver(dismissReceiver, IntentFilter(ACTION_DISMISS_ALARM_UI))
            isReceiverRegistered = true
        }
    }

    private fun unregisterDismissReceiver() {
        if (isReceiverRegistered) {
            runCatching { unregisterReceiver(dismissReceiver) }
            isReceiverRegistered = false
        }
    }

    private fun handleAction(action: String) {
        val payload = alarmState.value
        if (payload.recordId <= 0L) return

        val broadcast = MedicationAlarmReceiver.createIntent(
            context = this,
            action = action,
            recordId = payload.recordId,
            medicineName = payload.medicineName,
            dosage = payload.dosage,
            scheduledTime = payload.scheduledTime,
            medicineId = payload.medicineId
        )
        sendBroadcast(broadcast)
        finishAlarm()
    }

    private fun openDetailScreen() {
        val payload = alarmState.value
        if (payload.medicineId <= 0L) return
        val detailIntent = MainActivity.createDetailIntent(this, payload.medicineId)
        startActivity(detailIntent)
    }

    private fun finishAlarm() {
        stopAlert()
        if (!isFinishing) {
            finish()
        }
    }

    private fun startAlert() {
        startSound()
        startVibration()
    }

    private fun restartAlert() {
        stopAlert()
        startAlert()
    }

    private fun stopAlert() {
        stopSound()
        stopVibration()
    }

    private fun startSound() {
        stopSound()
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        mediaPlayer = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@MedicationAlarmActivity, soundUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (error: Exception) {
            Log.e(TAG, "Failed to start alarm sound", error)
            null
        }
    }

    private fun stopSound() {
        mediaPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
        }
        mediaPlayer = null
    }

    private fun startVibration() {
        val pattern = longArrayOf(0L, 700L, 300L, 700L)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java) ?: getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 0)
            }
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    data class AlarmPayload(
        val recordId: Long = -1L,
        val medicineId: Long = -1L,
        val medicineName: String = "",
        val dosage: String = "",
        val scheduledTime: String = ""
    )

    companion object {
        private const val TAG = "MedicationAlarmActivity"

        fun createIntent(
            context: Context,
            recordId: Long,
            medicineId: Long,
            medicineName: String,
            dosage: String,
            scheduledTime: String
        ): Intent = Intent(context, MedicationAlarmActivity::class.java).apply {
            putExtra(EXTRA_RECORD_ID, recordId)
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}

@Composable
private fun MedicationAlarmScreen(
    payload: MedicationAlarmActivity.AlarmPayload,
    onTake: () -> Unit,
    onTakeToday: () -> Unit,
    onSnooze: () -> Unit,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentTimeState = remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeState.value = LocalTime.now()
            delay(1_000)
        }
    }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "약 복용 알림",
                style = MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(alpha = 0.9f))
            )
            Text(
                text = currentTimeState.value.format(timeFormatter),
                fontSize = 58.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (payload.scheduledTime.isNotBlank()) {
                Text(
                    text = "예정 시간 ${payload.scheduledTime}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f))
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = payload.medicineName.ifBlank { "약 이름 미확인" },
                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
            Text(
                text = payload.dosage.ifBlank { "복용량 정보 없음" },
                style = MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(alpha = 0.9f)),
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onTake,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text("복용 완료", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("1시간 후 알림")
                }

                OutlinedButton(
                    onClick = onTakeToday,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("오늘 복용 처리")
                }
            }

            TextButton(
                onClick = onViewDetails,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("자세히 보기", color = Color.White.copy(alpha = 0.9f))
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("닫기", color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}
