package com.permissionmodule.data

import android.app.Activity
import android.content.Context
import com.permissionmodule.data.handler.PermissionHandler
import com.permissionmodule.data.handler.RuntimePermissionHandler
import com.permissionmodule.data.handler.SpecialPermissionHandler
import com.permissionmodule.domain.model.Permission
import com.permissionmodule.domain.model.PermissionResult
import com.permissionmodule.domain.model.PermissionStatus
import com.permissionmodule.domain.repository.PermissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PermissionRepository 구현체
 *
 * @property handlers 사용 가능한 권한 핸들러 목록
 */
@Singleton
class PermissionRepositoryImpl @Inject constructor(
    private val context: Context,
    private val handlers: Set<@JvmSuppressWildcards PermissionHandler>
) : PermissionRepository {

    // 권한 상태 캐시
    private val permissionStateCache = MutableStateFlow<Map<Permission, PermissionStatus>>(emptyMap())

    override suspend fun checkPermission(permission: Permission): PermissionStatus {
        val handler = findHandlerForPermission(permission)
        return handler?.checkPermission(permission) ?: PermissionStatus.NotRequested
    }

    override suspend fun requestPermission(
        activity: Activity,
        permission: Permission,
        onResult: (PermissionResult) -> Unit
    ) {
        val handler = findHandlerForPermission(permission)
        if (handler != null) {
            handler.requestPermission(activity, permission, onResult)
        } else {
            onResult(PermissionResult.Dismissed)
        }
    }

    override suspend fun checkAllPermissions(permissions: List<Permission>): Map<Permission, PermissionStatus> {
        val statusMap = mutableMapOf<Permission, PermissionStatus>()

        permissions.forEach { permission ->
            if (permission.isRequiredForCurrentSdk()) {
                val status = checkPermission(permission)
                statusMap[permission] = status
            }
        }

        return statusMap
    }

    override fun observePermissions(permissions: List<Permission>): Flow<Map<Permission, PermissionStatus>> {
        return permissionStateCache.map { cache ->
            permissions.filter { it.isRequiredForCurrentSdk() }
                .associateWith { permission ->
                    cache[permission] ?: PermissionStatus.NotRequested
                }
        }
    }

    /**
     * 권한 상태를 업데이트합니다.
     * 주로 권한 요청 결과가 도착했을 때 호출합니다.
     */
    fun updatePermissionStatus(permission: Permission, status: PermissionStatus) {
        val currentCache = permissionStateCache.value.toMutableMap()
        currentCache[permission] = status
        permissionStateCache.value = currentCache
    }

    /**
     * 권한에 적합한 핸들러를 찾습니다.
     */
    private fun findHandlerForPermission(permission: Permission): PermissionHandler? {
        return handlers.firstOrNull { it.canHandle(permission) }
    }
}
