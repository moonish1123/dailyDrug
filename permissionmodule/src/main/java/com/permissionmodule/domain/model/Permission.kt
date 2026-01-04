package com.permissionmodule.domain.model

import android.os.Build

/**
 * 권한 정의 data class
 *
 * @property id 권한의 고유 식별자
 * @property androidPermission AndroidManifest에 선언된 권한 문자열
 * @property type 권한 유형 (RUNTIME, SPECIAL, NORMAL)
 * @property minSdkVersion 이 권한이 필요한 최소 SDK 버전 (null인 경우 모든 버전에서 필요)
 * @property description 권한 설명 (UI 표시용)
 * @property settingsAction SPECIAL 권한용 시스템 설정 Action
 */
data class Permission(
    val id: String,
    val androidPermission: String,
    val type: PermissionType,
    val minSdkVersion: Int? = null,
    val description: String,
    val settingsAction: String? = null
) {
    /**
     * 현재 Android 버전에서 이 권한이 필요한지 확인
     */
    fun isRequiredForCurrentSdk(): Boolean {
        return minSdkVersion == null || Build.VERSION.SDK_INT >= minSdkVersion
    }
}
