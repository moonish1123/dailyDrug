package com.networkmodule.di

import android.content.Context
import com.networkmodule.BuildConfig
import com.networkmodule.api.NetworkClientFactory
import com.networkmodule.api.NetworkLogger
import com.networkmodule.internal.factory.NetworkClientFactoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideNetworkLogger(): NetworkLogger {
        return if (BuildConfig.DEBUG) {
            NetworkLogger.DEFAULT
        } else {
            NetworkLogger.NONE
        }
    }

    @Provides
    @Singleton
    fun provideNetworkClientFactory(
        @ApplicationContext context: Context,
        logger: NetworkLogger,
        json: Json
    ): NetworkClientFactory {
        return NetworkClientFactoryImpl(
            cacheDir = context.cacheDir,
            defaultLogger = logger,
            json = json
        )
    }
}
