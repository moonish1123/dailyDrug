package com.networkmodule.internal.interceptor

import okhttp3.Interceptor
import okhttp3.Response

internal class HeaderInterceptor(
    private val customHeaders: Map<String, String>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (customHeaders.isEmpty()) return chain.proceed(chain.request())
        val request = chain.request().newBuilder().apply {
            customHeaders.forEach { (key, value) ->
                if (value.isNotBlank()) {
                    header(key, value)
                }
            }
        }.build()
        return chain.proceed(request)
    }
}
