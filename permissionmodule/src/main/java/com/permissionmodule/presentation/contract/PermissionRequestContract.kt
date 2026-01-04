package com.permissionmodule.presentation.contract

import com.permissionmodule.domain.model.Permission

/**
 * 권한 요청 UI 커스터마이징을 위한 계약 인터페이스
 *
 * 앱에서 이 인터페이스를 구현하여 권한 요청 UI를 완전 커스터마이징할 수 있습니다.
 */
interface PermissionRequestContract {
    /**
     * 권한 요청 다이얼로그를 표시해야 하는지 여부를 결정합니다.
     */
    fun shouldShowPermissionRequest(permission: Permission): Boolean = true

    /**
     * 권한 요청 다이얼로그의 제목을 반환합니다.
     */
    fun getPermissionDialogTitle(permission: Permission): String

    /**
     * 권한 요청 다이얼로그의 메시지를 반환합니다.
     */
    fun getPermissionDialogMessage(permission: Permission): String

    /**
     * 확인 버튼의 텍스트를 반환합니다.
     */
    fun getConfirmButtonText(permission: Permission): String = "허용"

    /**
     * 해제(나중에) 버튼의 텍스트를 반환합니다.
     */
    fun getDismissButtonText(permission: Permission): String = "나중에"

    /**
     * 사용자가 권한 요청을 해제했을 때 호출됩니다.
     */
    fun onPermissionDismissed(permission: Permission) {}
}
