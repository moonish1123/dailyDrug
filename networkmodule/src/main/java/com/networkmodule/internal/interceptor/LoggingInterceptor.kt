package com.networkmodule.internal.interceptor

import com.networkmodule.api.NetworkError
import com.networkmodule.api.NetworkLogger
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer

private const val MAX_LOG_BYTES = 8_192L

internal class LoggingInterceptor(
    private val logger: NetworkLogger
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBody = bodyToString(request)
        logger.logRequest(
            url = request.url.toString(),
            method = request.method,
            headers = headersToMap(request.headers),
            body = requestBody
        )

        val startNs = System.nanoTime()

        return try {
            val response = chain.proceed(request)
            val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
            val peeked = response.peekBody(MAX_LOG_BYTES).string()
            logger.logResponse(
                url = response.request.url.toString(),
                statusCode = response.code,
                body = peeked.ifBlank { null },
                durationMs = tookMs
            )
            response
        } catch (throwable: Throwable) {
            logger.logError(
                url = request.url.toString(),
                error = NetworkError.Unknown(
                    message = throwable.message ?: "Network call failed",
                    cause = throwable
                )
            )
            throw throwable
        }
    }

    private fun headersToMap(headers: Headers): Map<String, String> =
        headers.names().associateWith { name -> headers.get(name).orEmpty() }

    private fun bodyToString(request: Request): String? {
        val body = request.body ?: return null
        if (bodyHasUnknownEncoding(request.headers)) return null
        return try {
            Buffer().apply { body.writeTo(this) }
                .readString(body.contentType()?.charset(Charset.forName("UTF-8")) ?: Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val encoding = headers["Content-Encoding"] ?: return false
        return !encoding.equals("identity", ignoreCase = true) &&
            !encoding.equals("gzip", ignoreCase = true)
    }
}
