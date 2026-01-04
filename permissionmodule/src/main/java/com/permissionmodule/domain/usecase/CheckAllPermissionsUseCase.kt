package com.permissionmodule.domain.usecase

import com.permissionmodule.domain.model.Permission
import com.permissionmodule.domain.model.PermissionStatus
import com.permissionmodule.domain.repository.PermissionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 다중 권한 상태 확인을 위한 UseCase
 */
class CheckAllPermissionsUseCase @Inject constructor(
    private val repository: PermissionRepository
) {
    /**
     * 현재 모든 권한의 상태를 일회성으로 확인합니다.
     */
    suspend fun checkOnce(permissions: List<Permission>): Map<Permission, PermissionStatus> {
        return repository.checkAllPermissions(permissions)
    }

    /**
     * 권한 상태 변화를 지속적으로 관찰합니다.
     */
    fun observe(permissions: List<Permission>): Flow<Map<Permission, PermissionStatus>> {
        return repository.observePermissions(permissions)
    }
}
