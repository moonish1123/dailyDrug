package com.networkmodule.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import okhttp3.Interceptor

/**
 * Immutable configuration used to tune network behaviour at call-site.
 */
@Serializable
data class NetworkConfig(
    val connectTimeoutSeconds: Long = 30,
    val readTimeoutSeconds: Long = 60,
    val writeTimeoutSeconds: Long = 60,
    val enableLogging: Boolean = false,
    val retryOnConnectionFailure: Boolean = true,
    val maxRetries: Int = 0,
    val retryDelayMillis: Long = 1_000,
    val customHeaders: Map<String, String> = emptyMap(),
    val certificatePinning: CertificatePinningConfig? = null,
    val cacheConfig: CacheConfig? = null,
    @Transient
    val interceptors: List<Interceptor> = emptyList()
) {
    companion object {
        val DEFAULT = NetworkConfig()

        val STREAMING = NetworkConfig(
            readTimeoutSeconds = 300,
            writeTimeoutSeconds = 300
        )

        val FAST_FAIL = NetworkConfig(
            connectTimeoutSeconds = 10,
            readTimeoutSeconds = 15,
            maxRetries = 1,
            retryDelayMillis = 500
        )
    }
}

/**
 * Certificate pinning configuration.
 *
 * @param hostname Hostname to pin certificates for.
 * @param pins SHA-256 pins in OkHttp hash format (sha256/BASE64_HASH).
 */
@Serializable
data class CertificatePinningConfig(
    val hostname: String,
    val pins: List<String>
)

/**
 * Cache configuration for OkHttp.
 *
 * @param maxSizeMb Maximum cache size in megabytes.
 * @param maxAgeSeconds Stale cache age before eviction.
 */
@Serializable
data class CacheConfig(
    val maxSizeMb: Int = 10,
    val maxAgeSeconds: Long = 300
)
