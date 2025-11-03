package com.llmmodule.di

import com.llmmodule.data.provider.LlmService
import com.llmmodule.data.provider.claude.ClaudeLlmService
import com.llmmodule.data.provider.gpt.GptLlmService
import com.llmmodule.data.provider.local.LocalLlmService
import com.llmmodule.data.repository.LlmRepositoryImpl
import com.llmmodule.domain.config.LlmConfiguration
import com.llmmodule.domain.repository.LlmRepository
import com.networkmodule.api.NetworkClientFactory
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.util.Optional
import javax.inject.Singleton
import kotlin.jvm.JvmSuppressWildcards

@Module
@InstallIn(SingletonComponent::class)
object LlmProviderModule {

    @Provides
    @Singleton
    @IntoSet
    fun provideClaudeService(factory: NetworkClientFactory): LlmService =
        ClaudeLlmService(factory)

    @Provides
    @Singleton
    @IntoSet
    fun provideGptService(factory: NetworkClientFactory): LlmService =
        GptLlmService(factory)

    @Provides
    @Singleton
    @IntoSet
    fun provideLocalService(): LlmService = LocalLlmService()
}

@Module
@InstallIn(SingletonComponent::class)
object LlmBindingsModule {

    @Provides
    @Singleton
    fun provideLlmRepository(
        configuration: Optional<LlmConfiguration>,
        services: Set<@JvmSuppressWildcards LlmService>
    ): LlmRepository {
        return LlmRepositoryImpl(configuration.orElse(null), services)
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface LlmConfigurationModule {

    @BindsOptionalOf
    fun optionalLlmConfiguration(): LlmConfiguration
}
