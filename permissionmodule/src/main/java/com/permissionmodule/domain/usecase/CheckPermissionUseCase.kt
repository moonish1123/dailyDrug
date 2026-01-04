package com.permissionmodule.domain.usecase

import com.permissionmodule.domain.model.Permission
import com.permissionmodule.domain.model.PermissionStatus
import com.permissionmodule.domain.repository.PermissionRepository
import javax.inject.Inject

/**
 * 단일 권한 상태 확인을 위한 UseCase
 */
class CheckPermissionUseCase @Inject constructor(
    private val repository: PermissionRepository
) {
    suspend operator fun invoke(permission: Permission): PermissionStatus {
        return repository.checkPermission(permission)
    }
}
