package com.dailydrug.permission

import com.dailydrug.util.Log
import com.permissionmodule.domain.model.Permission
import com.permissionmodule.presentation.contract.PermissionRequestContract

/**
 * DailyDrug 앱을 위한 권한 요청 UI 계약 구현
 */
class DailyDrugPermissionRequestContract : PermissionRequestContract {

    companion object {
        private const val TAG = "PermissionRequest"
    }

    override fun shouldShowPermissionRequest(permission: Permission): Boolean {
        // 앱별 로직: 항상 권한 요청 표시
        return true
    }

    override fun getPermissionDialogTitle(permission: Permission): String {
        return when (permission.id) {
            DailyDrugPermissions.POST_NOTIFICATIONS.id -> "알림 권한 허용"
            DailyDrugPermissions.SCHEDULE_EXACT_ALARM.id -> "정확한 알람 권한 허용"
            DailyDrugPermissions.USE_FULL_SCREEN_INTENT.id -> "전체 화면 알림 권한 허용"
            else -> "권한 허용"
        }
    }

    override fun getPermissionDialogMessage(permission: Permission): String {
        return permission.description
    }

    override fun getConfirmButtonText(permission: Permission): String {
        return "허용"
    }

    override fun getDismissButtonText(permission: Permission): String {
        return "나중에"
    }

    override fun onPermissionDismissed(permission: Permission) {
        Log.d(TAG, "Permission request dismissed: ${permission.id}")
        // 필요한 경우 여기서 analytics 이벤트 전송 등 추가 작업 수행
    }
}
