package com.permissionmodule.data.handler

import android.app.Activity
import com.permissionmodule.domain.model.Permission
import com.permissionmodule.domain.model.PermissionResult
import com.permissionmodule.domain.model.PermissionStatus

/**
 * 권한 처리를 위한 인터페이스
 * 각 권한 유형별로 구현해야 합니다.
 */
interface PermissionHandler {
    /**
     * 이 핸들러가 해당 권한을 처리할 수 있는지 확인합니다.
     */
    fun canHandle(permission: Permission): Boolean

    /**
     * 권한 상태를 확인합니다.
     *
     * @param permission 확인할 권한
     * @return 권한 상태
     */
    suspend fun checkPermission(permission: Permission): PermissionStatus

    /**
     * 권한을 요청합니다.
     *
     * @param activity Activity 인스턴스
     * @param permission 요청할 권한
     * @param onResult 권한 요청 결과 콜백
     */
    suspend fun requestPermission(
        activity: Activity,
        permission: Permission,
        onResult: (PermissionResult) -> Unit
    )
}
