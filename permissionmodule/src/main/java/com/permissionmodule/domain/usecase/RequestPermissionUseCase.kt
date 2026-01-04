package com.permissionmodule.domain.usecase

import android.app.Activity
import com.permissionmodule.domain.model.Permission
import com.permissionmodule.domain.model.PermissionResult
import com.permissionmodule.domain.repository.PermissionRepository
import javax.inject.Inject

/**
 * 단일 권한 요청을 위한 UseCase
 */
class RequestPermissionUseCase @Inject constructor(
    private val repository: PermissionRepository
) {
    suspend operator fun invoke(
        activity: Activity,
        permission: Permission,
        onResult: (PermissionResult) -> Unit
    ) {
        repository.requestPermission(activity, permission, onResult)
    }
}
