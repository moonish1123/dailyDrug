package com.permissionmodule.presentation.component

import android.app.Activity
import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.permissionmodule.domain.model.Permission
import com.permissionmodule.domain.model.PermissionStatus
import com.permissionmodule.domain.repository.PermissionRepository
import com.permissionmodule.presentation.contract.PermissionRequestContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 순차적 권한 요청 Composable
 *
 * @param permissions 요청할 권한 목록
 * @param contract UI 커스터마이징을 위한 계약
 * @param repository 권한 repository
 * @param onAllGranted 모든 권한이 부여되었을 때 호출
 * @param onAnyDenied 권한이 거부되었을 때 호출
 */
@Composable
fun PermissionRequester(
    permissions: List<Permission>,
    contract: PermissionRequestContract,
    repository: PermissionRepository,
    onAllGranted: () -> Unit = {},
    onAnyDenied: (Permission) -> Unit = {}
) {
    // 현재 요청 중인 권한 인덱스
    var currentPermissionIndex by remember { mutableIntStateOf(0) }

    // 현재 표시할 다이얼로그 상태
    var showDialog by remember { mutableStateOf(false) }
    var currentPermission by remember { mutableStateOf<Permission?>(null) }

    // Lifecycle 관찰자 - Settings에서 돌아왔을 때 권한 재확인
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Settings에서 돌아왔을 때 권한 재확인
                // 권한 상태 재확인은 다음 권한 체크에서 자동으로 처리됨
                currentPermissionIndex = 0
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 초기 권한 체크
    LaunchedEffect(permissions) {
        checkAndRequestPermissions(
            permissions = permissions,
            repository = repository,
            contract = contract,
            startIndex = currentPermissionIndex,
            onIndexUpdate = { index -> currentPermissionIndex = index },
            onShowDialog = { permission ->
                currentPermission = permission
                showDialog = true
            },
            onAllGranted = onAllGranted,
            onAnyDenied = onAnyDenied
        )
    }

    // 권한 요청을 위한 Scope
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 권한 요청 다이얼로그 표시
    if (showDialog && currentPermission != null) {
        PermissionRequestDialog(
            permission = currentPermission!!,
            contract = contract,
            onConfirm = {
                showDialog = false
                val activity = context.findActivity()
                if (activity != null) {
                    scope.launch {
                        repository.requestPermission(activity, currentPermission!!) { result ->
                            // 요청 결과 처리
                            // 현재 인덱스부터 다시 체크하여 상태 갱신 확인
                            currentPermissionIndex = currentPermissionIndex
                        }
                    }
                } else {
                    // Activity를 찾을 수 없는 경우 (거의 없겠지만) 다음으로 넘어감
                    currentPermissionIndex++
                }
            },
            onDismiss = {
                showDialog = false
                contract.onPermissionDismissed(currentPermission!!)
                // 사용자가 거부한 경우 다음 권한으로 이동하지 않음 (또는 다음으로 이동? 정책 결정 필요)
                // 여기서는 사용자가 명시적으로 취소했으므로 루프를 멈추거나 다음으로 넘어가야 함.
                // 일반적인 UX상 다음 필수 권한을 물어보는게 맞음.
                currentPermissionIndex++
            }
        )
    }
}

@Composable
private fun PermissionRequestDialog(
    permission: Permission,
    contract: PermissionRequestContract,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(contract.getPermissionDialogTitle(permission)) },
        text = { Text(contract.getPermissionDialogMessage(permission)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(contract.getConfirmButtonText(permission))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(contract.getDismissButtonText(permission))
            }
        }
    )
}

/**
 * Context에서 Activity를 찾습니다.
 */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * 권한을 체크하고 요청합니다.
 */
private suspend fun checkAndRequestPermissions(
    permissions: List<Permission>,
    repository: PermissionRepository,
    contract: PermissionRequestContract,
    startIndex: Int,
    onIndexUpdate: (Int) -> Unit,
    onShowDialog: (Permission) -> Unit,
    onAllGranted: () -> Unit,
    onAnyDenied: (Permission) -> Unit
) = withContext(Dispatchers.Main) {
    val relevantPermissions = permissions.filter { it.isRequiredForCurrentSdk() }

    // 모든 권한이 부여된 경우
    if (relevantPermissions.isEmpty()) {
        onAllGranted()
        return@withContext
    }

    // 현재 인덱스부터 권한 체크
    for (i in startIndex until relevantPermissions.size) {
        val permission = relevantPermissions[i]
        val status = repository.checkPermission(permission)

        when (status) {
            is PermissionStatus.Granted -> {
                // 이미 부여됨, 다음 권한으로
                onIndexUpdate(i + 1)
                continue
            }
            is PermissionStatus.Denied, is PermissionStatus.NotRequested -> {
                // 권한이 필요함, 다이얼로그 표시
                if (contract.shouldShowPermissionRequest(permission)) {
                    onIndexUpdate(i)
                    onShowDialog(permission)
                    return@withContext
                }
            }
        }
    }

    // 모든 권한이 부여된 경우
    onAllGranted()
}
