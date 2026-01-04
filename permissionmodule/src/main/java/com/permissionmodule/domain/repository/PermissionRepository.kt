package com.permissionmodule.domain.repository

import android.app.Activity
import com.permissionmodule.domain.model.Permission
import com.permissionmodule.domain.model.PermissionResult
import com.permissionmodule.domain.model.PermissionStatus
import kotlinx.coroutines.flow.Flow

/**
 * 권한 체크 및 요청을 위한 Repository 인터페이스
 */
interface PermissionRepository {
    /**
     * 단일 권한의 상태를 확인합니다.
     *
     * @param permission 확인할 권한
     * @return 권한 상태 (Granted, Denied, NotRequested)
     */
    suspend fun checkPermission(permission: Permission): PermissionStatus

    /**
     * 단일 권한을 요청합니다.
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

    /**
     * 여러 권한의 상태를 확인합니다.
     *
     * @param permissions 확인할 권한 목록
     * @return 권한별 상태 맵
     */
    suspend fun checkAllPermissions(permissions: List<Permission>): Map<Permission, PermissionStatus>

    /**
     * 여러 권한의 상태 변화를 Flow로 스트리밍합니다.
     *
     * @param permissions 관찰할 권한 목록
     * @return 권한 상태 변화 Flow
     */
    fun observePermissions(permissions: List<Permission>): Flow<Map<Permission, PermissionStatus>>
}
