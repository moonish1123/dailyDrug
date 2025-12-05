package com.dailydrug.di

import com.dailydrug.data.config.AppLlmConfiguration
import com.llmmodule.domain.config.LlmConfiguration
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface LlmConfigModule {

    @Binds
    @Singleton
    fun bindLlmConfiguration(
        impl: AppLlmConfiguration
    ): LlmConfiguration
}
