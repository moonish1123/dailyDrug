package com.permissionmodule.domain.model

/**
 * 권한 상태를 나타내는 sealed class
 */
sealed class PermissionStatus {
    /**
     * 권한이 부여된 상태
     */
    object Granted : PermissionStatus()

    /**
     * 권한이 아직 요청되지 않은 상태
     */
    object NotRequested : PermissionStatus()

    /**
     * 권한이 거부된 상태
     * @property canRequestAgain true인 경우 다시 요청 가능, false인 경우 "다시 묻지 않음"이 체크됨
     */
    data class Denied(val canRequestAgain: Boolean = true) : PermissionStatus()
}
