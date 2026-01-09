package com.dailydrug.permission

import android.Manifest
import android.os.Build
import android.provider.Settings
import com.permissionmodule.domain.model.Permission
import com.permissionmodule.domain.model.PermissionType

/**
 * DailyDrug 앱에서 사용하는 권한 정의
 */
object DailyDrugPermissions {
    /**
     * 알림 권한 (Android 13+)
     */
    val POST_NOTIFICATIONS = Permission(
        id = "post_notifications",
        androidPermission = Manifest.permission.POST_NOTIFICATIONS,
        type = PermissionType.RUNTIME,
        minSdkVersion = Build.VERSION_CODES.TIRAMISU,
        description = "약 알림을 표시하기 위해 알림 권한이 필요합니다.",
        settingsAction = null
    )

    /**
     * 정확한 알람 권한 (Android 12+)
     */
    val SCHEDULE_EXACT_ALARM = Permission(
        id = "schedule_exact_alarm",
        androidPermission = "android.permission.SCHEDULE_EXACT_ALARM",
        type = PermissionType.SPECIAL,
        minSdkVersion = Build.VERSION_CODES.S,
        description = "정확한 알람 시간을 예약하기 위해 권한이 필요합니다.",
        settingsAction = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
    )

    /**
     * 전체 화면 알림 권한 (Android 14+)
     */
    val USE_FULL_SCREEN_INTENT = Permission(
        id = "use_full_screen_intent",
        androidPermission = "android.permission.USE_FULL_SCREEN_INTENT",
        type = PermissionType.SPECIAL,
        minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        description = "알림 시 화면을 켜고 깨우기 위한 권한이 필요합니다.",
        settingsAction = Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
    )

    /**
     * 카메라 권한
     */
    val CAMERA = Permission(
        id = "camera",
        androidPermission = Manifest.permission.CAMERA,
        type = PermissionType.RUNTIME,
        description = "사진 촬영을 위해 카메라 권한이 필요합니다.",
        settingsAction = null
    )

    /**
     * 모든 필수 권한 목록
     */
    val ALL = listOf(
        POST_NOTIFICATIONS,
        SCHEDULE_EXACT_ALARM,
        USE_FULL_SCREEN_INTENT
    )
}
