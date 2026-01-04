package com.permissionmodule.domain.model

/**
 * 권한 요청 모드
 *
 * @property SEQUENTIAL 권한을 순차적으로 하나씩 요청 (기본값)
 * @property PARALLEL 모든 권한을 동시에 요청
 */
enum class RequestMode {
    SEQUENTIAL,
    PARALLEL
}
