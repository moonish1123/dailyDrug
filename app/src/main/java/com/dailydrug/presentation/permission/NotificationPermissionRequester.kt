package com.dailydrug.presentation.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.dailydrug.presentation.permission.extensions.findActivity

@Composable
fun NotificationPermissionRequester() {
    val context = LocalContext.current
    val activity = context.findActivity()
    val manager = remember { NotificationPermissionManager(context) }
    val showNotificationDialog = remember { mutableStateOf(false) }
    val showExactAlarmDialog = remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                showNotificationDialog.value = true
            } else if (!manager.canScheduleExactAlarms()) {
                showExactAlarmDialog.value = true
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !manager.hasPostNotificationsPermission()) {
            showNotificationDialog.value = true
        } else if (!manager.canScheduleExactAlarms()) {
            showExactAlarmDialog.value = true
        }
    }

    NotificationPermissionDialog(
        isVisible = showNotificationDialog,
        title = "알림 권한 필요",
        message = "약 복용 알림을 받으려면 알림 권한을 허용해주세요.",
        confirmLabel = "허용",
        onConfirm = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )

    NotificationPermissionDialog(
        isVisible = showExactAlarmDialog,
        title = "정확한 알람 허용",
        message = "정확한 알람 권한을 허용해야 예정 시간에 알림을 보낼 수 있습니다.",
        confirmLabel = "설정 열기",
        onConfirm = {
            activity?.let { manager.openExactAlarmSettings(it) }
        }
    )
}

@Composable
private fun NotificationPermissionDialog(
    isVisible: MutableState<Boolean>,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit
) {
    val visibleState = rememberUpdatedState(isVisible.value)
    if (!visibleState.value) return

    AlertDialog(
        onDismissRequest = { isVisible.value = false },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                isVisible.value = false
                onConfirm()
            }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = { isVisible.value = false }) {
                Text("나중에")
            }
        }
    )
}
