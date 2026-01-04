package com.permissionmodule.data.registry

import com.permissionmodule.domain.model.Permission

/**
 * 권한 등록 및 관리를 위한 레지스트리 클래스
 */
object PermissionRegistry {
    private val permissions = mutableMapOf<String, Permission>()

    /**
     * 권한을 등록합니다.
     */
    fun registerPermission(permission: Permission) {
        permissions[permission.id] = permission
    }

    /**
     * 여러 권한을 한 번에 등록합니다.
     */
    fun registerPermissions(permissionList: List<Permission>) {
        permissionList.forEach { permission ->
            permissions[permission.id] = permission
        }
    }

    /**
     * ID로 권한을 조회합니다.
     */
    fun getPermissionById(id: String): Permission? {
        return permissions[id]
    }

    /**
     * 모든 등록된 권한을 반환합니다.
     */
    fun getAllPermissions(): List<Permission> {
        return permissions.values.toList()
    }

    /**
     * 등록된 모든 권한을 제거합니다.
     */
    fun clear() {
        permissions.clear()
    }
}
