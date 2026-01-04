package com.dailydrug.presentation.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dailydrug.di.findActivity
import com.dailydrug.di.getPermissionRepository
import com.dailydrug.permission.DailyDrugPermissions
import com.permissionmodule.domain.model.PermissionStatus
import com.permissionmodule.domain.repository.PermissionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenLlmSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val permissionRepository: PermissionRepository? = remember(activity) {
        activity?.let { getPermissionRepository(it) }
    }

    if (permissionRepository == null) {
        // Activity를 찾을 수 없는 경우 (매우 드뭄)
        onBack()
        return
    }

    var permissionStates by remember { mutableStateOf<Map<com.permissionmodule.domain.model.Permission, PermissionStatus>>(emptyMap()) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Refresh permission states after request
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            permissionStates = permissionRepository.checkAllPermissions(DailyDrugPermissions.ALL)
        }
    }

    LaunchedEffect(Unit) {
        permissionStates = permissionRepository.checkAllPermissions(DailyDrugPermissions.ALL)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PermissionCard(
                icon = Icons.Rounded.Notifications,
                title = "알림 권한",
                statusGranted = permissionStates[DailyDrugPermissions.POST_NOTIFICATIONS] is PermissionStatus.Granted,
                statusLabel = if (permissionStates[DailyDrugPermissions.POST_NOTIFICATIONS] is PermissionStatus.Granted) "허용됨" else "미허용",
                description = "약 복용 알림을 정상적으로 받으려면 알림 권한이 필요합니다.",
                onPrimaryAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    permissionStates[DailyDrugPermissions.POST_NOTIFICATIONS] !is PermissionStatus.Granted) {
                    {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else null,
                primaryActionLabel = "권한 요청",
                onSecondaryAction = {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        permissionStates = permissionRepository.checkAllPermissions(DailyDrugPermissions.ALL)
                    }
                },
                secondaryActionLabel = "상태 새로고침"
            )

            PermissionCard(
                icon = Icons.Rounded.Alarm,
                title = "정확한 알람",
                statusGranted = permissionStates[DailyDrugPermissions.SCHEDULE_EXACT_ALARM] is PermissionStatus.Granted,
                statusLabel = if (permissionStates[DailyDrugPermissions.SCHEDULE_EXACT_ALARM] is PermissionStatus.Granted) "허용됨" else "미허용",
                description = "정확한 알람 권한을 허용하면 예정된 시간에 정확히 알림을 받을 수 있습니다.",
                onPrimaryAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    permissionStates[DailyDrugPermissions.SCHEDULE_EXACT_ALARM] !is PermissionStatus.Granted) {
                    {
                        // Open settings for exact alarm permission
                        DailyDrugPermissions.SCHEDULE_EXACT_ALARM.settingsAction?.let { action ->
                            context.startActivity(android.content.Intent(action).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            })
                        }
                    }
                } else null,
                primaryActionLabel = "설정에서 허용",
                onSecondaryAction = {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        permissionStates = permissionRepository.checkAllPermissions(DailyDrugPermissions.ALL)
                    }
                },
                secondaryActionLabel = "상태 새로고침"
            )

            // LLM 설정 카드
            LlmSettingsCard(
                onOpenLlmSettings = onOpenLlmSettings,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "뒤로가기")
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    statusGranted: Boolean,
    statusLabel: String,
    description: String,
    onPrimaryAction: (() -> Unit)?,
    primaryActionLabel: String?,
    onSecondaryAction: (() -> Unit)?,
    secondaryActionLabel: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AssistChip(
                onClick = {},
                label = { Text("상태: $statusLabel") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (statusGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    labelColor = if (statusGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onPrimaryAction != null && primaryActionLabel != null) {
                    FilledTonalButton(onClick = onPrimaryAction) {
                        Text(primaryActionLabel)
                    }
                }
                FilledTonalButton(onClick = onSecondaryAction ?: {}) { Text(secondaryActionLabel) }
            }
        }
    }
}

@Composable
private fun LlmSettingsCard(
    onOpenLlmSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "AI Assistant 설정",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = "AI 제공업체를 선택하고 API 키를 관리하세요. 온라인/오프라인을 전환할 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onOpenLlmSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("LLM 설정")
            }
        }
    }
}
