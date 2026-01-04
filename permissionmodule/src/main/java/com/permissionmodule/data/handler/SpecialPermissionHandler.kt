package com.permissionmodule.data.handler

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.permissionmodule.domain.model.Permission
import com.permissionmodule.domain.model.PermissionResult
import com.permissionmodule.domain.model.PermissionStatus
import com.permissionmodule.domain.model.PermissionType

/**
 * Special 권한 (SCHEDULE_EXACT_ALARM, USE_FULL_SCREEN_INTENT 등)을 처리하는 핸들러
 */
class SpecialPermissionHandler(
    private val context: Context
) : PermissionHandler {

    override fun canHandle(permission: Permission): Boolean {
        return permission.type == PermissionType.SPECIAL
    }

    override suspend fun checkPermission(permission: Permission): PermissionStatus {
        return when (permission.id) {
            "schedule_exact_alarm" -> checkExactAlarmPermission()
            "use_full_screen_intent" -> checkFullScreenIntentPermission()
            else -> checkGenericSpecialPermission(permission)
        }
    }

    override suspend fun requestPermission(
        activity: Activity,
        permission: Permission,
        onResult: (PermissionResult) -> Unit
    ) {
        val intent = when (permission.id) {
            "schedule_exact_alarm" -> createExactAlarmIntent()
            "use_full_screen_intent" -> createFullScreenIntentIntent()
            else -> createGenericIntent(permission)
        }

        if (intent != null) {
            context.startActivity(intent)
            onResult(PermissionResult.Granted) // Special 권한은 설정 화면 이동만 수행
        } else {
            onResult(PermissionResult.Dismissed)
        }
    }

    private fun checkExactAlarmPermission(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager?.canScheduleExactAlarms() == true) {
                PermissionStatus.Granted
            } else {
                PermissionStatus.Denied(canRequestAgain = true)
            }
        } else {
            // Android 12 미만에서는 항상 부여된 것으로 간주
            PermissionStatus.Granted
        }
    }

    private fun checkFullScreenIntentPermission(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager =
                ContextCompat.getSystemService(context, android.app.NotificationManager::class.java)
            if (notificationManager?.canUseFullScreenIntent() == true) {
                PermissionStatus.Granted
            } else {
                PermissionStatus.Denied(canRequestAgain = true)
            }
        } else {
            // Android 14 미만에서는 항상 부여된 것으로 간주
            PermissionStatus.Granted
        }
    }

    private fun checkGenericSpecialPermission(permission: Permission): PermissionStatus {
        // 일반적인 special permission 체크 로직
        // 특별한 체크 로직이 없으면 부여된 것으로 간주
        return PermissionStatus.Granted
    }

    private fun createExactAlarmIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }
    }

    private fun createFullScreenIntentIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }
    }

    private fun createGenericIntent(permission: Permission): Intent? {
        return permission.settingsAction?.let { action ->
            Intent(action).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }
}
