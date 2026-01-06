package com.llmmodule.di

import com.llmmodule.data.provider.LlmService
import com.llmmodule.data.provider.claude.ClaudeLlmService
import com.llmmodule.data.provider.gpt.GptLlmService
import com.llmmodule.data.provider.zai.ZaiLlmService
import com.llmmodule.data.repository.LlmRepositoryImpl
import com.llmmodule.domain.repository.LlmRepository
import com.networkmodule.api.NetworkClientFactory
import dagger.Binds
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
    fun provideZaiService(factory: NetworkClientFactory): LlmService =
        ZaiLlmService(factory)

    // Local LLM service temporarily disabled due to compilation issues
    // @Provides
    // @Singleton
    // @IntoSet
    // fun provideLocalService(
    //     executorRunner: ExecutorRunner,
    //     assetManager: ModelAssetManager
    // ): LlmService = LocalLlmService(executorRunner, assetManager)
}

@Module
@InstallIn(SingletonComponent::class)
object LlmBindingsModule {

    @Provides
    @Singleton
    fun provideLlmRepository(
        services: Set<@JvmSuppressWildcards LlmService>
    ): LlmRepository {
        return LlmRepositoryImpl(services)
    }
}

// Local LLM module temporarily disabled
// @Module
// @InstallIn(SingletonComponent::class)
// object LocalLlmModule {
//
//     @Provides
//     @Singleton
//     fun provideMemoryManager(): MemoryManager = MemoryManager()
//
//     @Provides
//     @Singleton
//     fun provideModelAssetManager(): ModelAssetManager = ModelAssetManager()
//
//     @Provides
//     @Singleton
//     fun provideExecutorRunner(
//         assetManager: ModelAssetManager,
//         memoryManager: MemoryManager
//     ): ExecutorRunner = ExecutorRunner(assetManager, memoryManager)
//
//     @Provides
//     @Singleton
//     fun provideLlmConfigurationManager(): LlmConfigurationManager = LlmConfigurationManager()
// }
