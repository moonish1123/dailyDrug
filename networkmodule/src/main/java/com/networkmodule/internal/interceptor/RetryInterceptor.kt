package com.networkmodule.internal.interceptor

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

internal class RetryInterceptor(
    private val maxRetries: Int,
    private val retryDelayMillis: Long
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var currentResponse: Response? = null
        var lastException: IOException? = null

        val request = chain.request()

        while (attempt <= maxRetries) {
            try {
                currentResponse?.close()
                currentResponse = chain.proceed(request)

                if (!shouldRetry(currentResponse)) {
                    return currentResponse
                }
            } catch (io: IOException) {
                lastException = io
                if (attempt >= maxRetries) throw io
            }

            attempt++
            if (retryDelayMillis > 0) {
                try {
                    Thread.sleep(retryDelayMillis)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw lastException ?: IOException("Retry interrupted")
                }
            }
        }

        currentResponse?.close()
        throw lastException ?: IOException("Request failed after retries")
    }

    private fun shouldRetry(response: Response): Boolean {
        return when (response.code) {
            in 500..599 -> true
            408 -> true
            else -> false
        }
    }
}
