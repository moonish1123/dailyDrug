package com.networkmodule.api

/**
 * Factory for creating type-safe Retrofit service instances.
 *
 * Consumers inject this interface and request concrete API services without
 * needing to know about Retrofit or OkHttp configuration details.
 */
interface NetworkClientFactory {

    /**
     * Creates a Retrofit service instance for the specified API interface.
     *
     * @param serviceClass Retrofit interface that describes the HTTP contract.
     * @param baseUrl Base URL used for every request dispatched by [serviceClass].
     * @param config Optional configuration that overrides the defaults.
     */
    fun <T> createService(
        serviceClass: Class<T>,
        baseUrl: String,
        config: NetworkConfig = NetworkConfig.DEFAULT
    ): T
}

/**
 * Inline, reified convenience overload so callers do not need to pass the class literal.
 */
inline fun <reified T> NetworkClientFactory.createService(
    baseUrl: String,
    config: NetworkConfig = NetworkConfig.DEFAULT
): T = createService(T::class.java, baseUrl, config)
