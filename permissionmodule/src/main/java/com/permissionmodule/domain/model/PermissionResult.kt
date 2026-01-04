package com.permissionmodule.domain.model

/**
 * 권한 요청 결과를 나타내는 sealed class
 */
sealed class PermissionResult {
    /**
     * 권한 요청이 성공한 경우
     */
    object Granted : PermissionResult()

    /**
     * 권한 요청이 거부된 경우
     * @property canRequestAgain true인 경우 다시 요청 가능, false인 경우 영구 거부됨
     */
    data class Denied(val canRequestAgain: Boolean = true) : PermissionResult()

    /**
     * 사용자가 권한 요청을 해제한 경우 (나중에하기)
     */
    object Dismissed : PermissionResult()
}
