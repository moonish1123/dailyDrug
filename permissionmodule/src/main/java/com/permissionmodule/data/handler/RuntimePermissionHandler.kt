package com.permissionmodule.data.handler

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import androidx.core.app.ActivityCompat
import com.permissionmodule.domain.model.Permission
import com.permissionmodule.domain.model.PermissionResult
import com.permissionmodule.domain.model.PermissionStatus
import com.permissionmodule.domain.model.PermissionType

/**
 * Runtime 권한 (POST_NOTIFICATIONS 등)을 처리하는 핸들러
 */
class RuntimePermissionHandler(
    private val context: Context
) : PermissionHandler {

    override fun canHandle(permission: Permission): Boolean {
        return permission.type == PermissionType.RUNTIME
    }

    override suspend fun checkPermission(permission: Permission): PermissionStatus {
        val androidPermission = permission.androidPermission

        return when {
            !isPermissionDeclared(androidPermission) -> {
                // AndroidManifest에 선언되지 않은 권한
                PermissionStatus.NotRequested
            }
            ActivityCompat.checkSelfPermission(
                context,
                androidPermission
            ) == PackageManager.PERMISSION_GRANTED -> {
                PermissionStatus.Granted
            }
            else -> {
                val canRequestAgain = !ActivityCompat.shouldShowRequestPermissionRationale(
                    context as? Activity ?: return PermissionStatus.Denied(canRequestAgain = true),
                    androidPermission
                )
                PermissionStatus.Denied(canRequestAgain = !canRequestAgain)
            }
        }
    }

    override suspend fun requestPermission(
        activity: Activity,
        permission: Permission,
        onResult: (PermissionResult) -> Unit
    ) {
        val androidPermission = permission.androidPermission

        // 이미 부여된 권한인지 확인
        if (ActivityCompat.checkSelfPermission(
                activity,
                androidPermission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onResult(PermissionResult.Granted)
            return
        }

        // 권한 요청
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(androidPermission),
            generateRequestCode(permission)
        )

        // NOTE: 결과는 Activity의 onRequestPermissionsResult에서 처리해야 합니다.
        // 이 핸들러는 요청만 수행하고 실제 결과 콜백은 Activity에서 처리합니다.
    }

    /**
     * AndroidManifest에 권한이 선언되어 있는지 확인합니다.
     */
    private fun isPermissionDeclared(permission: String): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            packageInfo?.requestedPermissions?.contains(permission) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 권한별 고유한 requestCode를 생성합니다.
     */
    private fun generateRequestCode(permission: Permission): Int {
        // 권한 ID의 hash를 사용하여 고유한 코드 생성
        return permission.id.hashCode() % 256
    }
}
