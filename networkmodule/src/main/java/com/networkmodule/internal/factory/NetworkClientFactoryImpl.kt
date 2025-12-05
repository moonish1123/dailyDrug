package com.networkmodule.internal.factory

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.networkmodule.api.NetworkClientFactory
import com.networkmodule.api.NetworkConfig
import com.networkmodule.api.NetworkLogger
import com.networkmodule.internal.adapter.NetworkResultCallAdapterFactory
import com.networkmodule.internal.interceptor.HeaderInterceptor
import com.networkmodule.internal.interceptor.LoggingInterceptor
import com.networkmodule.internal.interceptor.RetryInterceptor
import com.networkmodule.internal.util.ErrorMapper
import com.networkmodule.internal.util.NetworkUtils
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

internal class NetworkClientFactoryImpl(
    private val cacheDir: File,
    private val defaultLogger: NetworkLogger,
    private val json: Json
) : NetworkClientFactory {

    private val errorMapper = ErrorMapper()

    override fun <T> createService(
        serviceClass: Class<T>,
        baseUrl: String,
        config: NetworkConfig
    ): T {
        val okHttpClient = buildOkHttpClient(config)
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(NetworkResultCallAdapterFactory(errorMapper))
            .build()
        return retrofit.create(serviceClass)
    }

    private fun buildOkHttpClient(config: NetworkConfig): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
            .retryOnConnectionFailure(config.retryOnConnectionFailure)

        NetworkUtils.buildCache(cacheDir, config.cacheConfig)?.let(builder::cache)

        val certificatePinner = NetworkUtils.buildCertificatePinner(config.certificatePinning)
        certificatePinner?.let(builder::certificatePinner)

        if (config.customHeaders.isNotEmpty()) {
            builder.addInterceptor(HeaderInterceptor(config.customHeaders))
        }

        if (config.maxRetries > 0) {
            builder.addInterceptor(RetryInterceptor(config.maxRetries, config.retryDelayMillis))
        }

        if (config.enableLogging) {
            builder.addInterceptor(LoggingInterceptor(defaultLogger))
        }

        config.interceptors.forEach { builder.addInterceptor(it) }

        return builder.build()
    }
}
