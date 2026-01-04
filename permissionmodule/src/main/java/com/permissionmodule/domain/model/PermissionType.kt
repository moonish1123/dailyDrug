package com.permissionmodule.domain.model

/**
 * 권한 유형을 정의하는 enum 클래스
 *
 * @property RUNTIME 표준 runtime permission (POST_NOTIFICATIONS, etc.)
 * @property SPECIAL 시스템 설정이 필요한 special permission (SCHEDULE_EXACT_ALARM, etc.)
 * @property NORMAL 자동으로 부여되는 normal permission (VIBRATE, etc.)
 */
enum class PermissionType {
    RUNTIME,
    SPECIAL,
    NORMAL
}
