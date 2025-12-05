package com.networkmodule.testing

import com.networkmodule.api.NetworkClientFactory
import com.networkmodule.api.NetworkConfig

/**
 * Fake implementation for unit tests.
 *
 * Allows complete control over responses without network calls.
 */
class FakeNetworkClientFactory : NetworkClientFactory {

    private val serviceMap = mutableMapOf<Class<*>, Any>()

    override fun <T> createService(
        serviceClass: Class<T>,
        baseUrl: String,
        config: NetworkConfig
    ): T {
        @Suppress("UNCHECKED_CAST")
        return serviceMap[serviceClass] as? T
            ?: throw IllegalStateException("No fake service registered for ${serviceClass.simpleName}")
    }

    fun <T> registerFakeService(serviceClass: Class<T>, fakeImpl: T) {
        serviceMap[serviceClass] = fakeImpl as Any
    }

    inline fun <reified T> registerFakeService(fakeImpl: T) {
        registerFakeService(T::class.java, fakeImpl)
    }
}
