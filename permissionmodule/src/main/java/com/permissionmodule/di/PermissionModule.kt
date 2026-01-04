package com.permissionmodule.di

import android.content.Context
import com.permissionmodule.data.PermissionRepositoryImpl
import com.permissionmodule.data.handler.PermissionHandler
import com.permissionmodule.data.handler.RuntimePermissionHandler
import com.permissionmodule.data.handler.SpecialPermissionHandler
import com.permissionmodule.domain.repository.PermissionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PermissionModule {

    /**
     * Runtime 권한 핸들러를 제공합니다.
     */
    @Provides
    @Singleton
    fun provideRuntimePermissionHandler(
        @ApplicationContext context: Context
    ): RuntimePermissionHandler {
        return RuntimePermissionHandler(context)
    }

    /**
     * Special 권한 핸들러를 제공합니다.
     */
    @Provides
    @Singleton
    fun provideSpecialPermissionHandler(
        @ApplicationContext context: Context
    ): SpecialPermissionHandler {
        return SpecialPermissionHandler(context)
    }

    /**
     * 모든 권한 핸들러를 제공합니다.
     */
    @Provides
    @Singleton
    fun providePermissionHandlers(
        runtimePermissionHandler: RuntimePermissionHandler,
        specialPermissionHandler: SpecialPermissionHandler
    ): Set<@JvmSuppressWildcards PermissionHandler> {
        return setOf(
            runtimePermissionHandler,
            specialPermissionHandler
        )
    }

    /**
     * 권한 Repository를 제공합니다.
     */
    @Provides
    @Singleton
    fun providePermissionRepository(
        @ApplicationContext context: Context,
        handlers: Set<@JvmSuppressWildcards PermissionHandler>
    ): PermissionRepository {
        return PermissionRepositoryImpl(context, handlers)
    }
}
