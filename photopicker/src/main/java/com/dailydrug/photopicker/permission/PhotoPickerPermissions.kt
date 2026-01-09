package com.dailydrug.photopicker.permission

import android.Manifest
import com.permissionmodule.domain.model.Permission
import com.permissionmodule.domain.model.PermissionType

/**
 * Camera permission definition for PhotoPicker module.
 */
object PhotoPickerPermissions {
    val CAMERA = Permission(
        id = "camera",
        androidPermission = Manifest.permission.CAMERA,
        type = PermissionType.RUNTIME,
        description = "사진 촬영을 위해 카메라 권한이 필요합니다."
    )
}
