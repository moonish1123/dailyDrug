package com.dailydrug.di

import android.content.Context
import com.permissionmodule.domain.repository.PermissionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent

/**
 * Permission Repository에 대한 Hilt 진입점
 *
 * Composable에서 ViewModel을 통하지 않고 직접 Repository를 주입받기 위해 사용합니다.
 */
@EntryPoint
@InstallIn(ActivityComponent::class)
interface PermissionEntryPoint {
    fun permissionRepository(): PermissionRepository
}

/**
 * Activity의 Hilt 진입점에서 Permission Repository를 가져옵니다.
 */
fun getPermissionRepository(activity: android.app.Activity): PermissionRepository {
    val entryPoint = EntryPointAccessors.fromActivity(
        activity = activity,
        entryPoint = PermissionEntryPoint::class.java
    )
    return entryPoint.permissionRepository()
}

/**
 * Context에서 Activity를 찾습니다.
 */
fun Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}
