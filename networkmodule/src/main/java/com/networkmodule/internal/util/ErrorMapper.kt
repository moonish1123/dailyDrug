package com.networkmodule.internal.util

import com.networkmodule.api.NetworkError
import com.networkmodule.api.NetworkResult
import java.lang.reflect.Type
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.serialization.SerializationException
import okhttp3.Headers
import okhttp3.ResponseBody
import retrofit2.Response

internal class ErrorMapper {

    fun <T> toNetworkResult(response: Response<T>, successType: Type): NetworkResult<T> {
        val statusCode = response.code()
        val headers = response.headers().toSingleValueMap()

        if (response.isSuccessful) {
            val body = response.body()
            @Suppress("UNCHECKED_CAST")
            return when {
                body != null -> NetworkResult.Success(
                    data = body,
                    statusCode = statusCode,
                    headers = headers
                )

                isUnit(successType) -> NetworkResult.Success(
                    data = Unit as T,
                    statusCode = statusCode,
                    headers = headers
                )

                else -> NetworkResult.Error(
                    error = NetworkError.ParseError(
                        message = "Expected response body but was null"
                    ),
                    statusCode = statusCode
                )
            }
        }

        val errorBody = response.errorBody()?.consume()
        val error = mapHttpError(
            statusCode = statusCode,
            message = response.message().ifBlank { defaultMessageForCode(statusCode) },
            raw = errorBody
        )
        return NetworkResult.Error(
            error = error,
            statusCode = statusCode,
            rawResponse = errorBody
        )
    }

    fun mapFailure(throwable: Throwable): NetworkError = when (throwable) {
        is UnknownHostException -> NetworkError.NoConnection(
            message = throwable.message ?: "Unable to resolve host",
            cause = throwable
        )
        is ConnectException -> NetworkError.Timeout(
            message = throwable.message ?: "Failed to connect to server",
            cause = throwable,
            timeoutType = NetworkError.Timeout.TimeoutType.CONNECT
        )
        is SocketTimeoutException -> NetworkError.Timeout(
            message = throwable.message ?: "Network operation timed out",
            cause = throwable,
            timeoutType = NetworkError.Timeout.TimeoutType.READ
        )
        is SSLException -> NetworkError.SslError(
            message = throwable.message ?: "SSL error",
            cause = throwable
        )
        is SerializationException -> NetworkError.ParseError(
            message = throwable.message ?: "Failed to parse response",
            cause = throwable
        )
        is java.io.IOException -> NetworkError.Unknown(
            message = throwable.message ?: "I/O error occurred",
            cause = throwable
        )
        else -> NetworkError.Unknown(
            message = throwable.message ?: "Unexpected error occurred",
            cause = throwable
        )
    }

    private fun mapHttpError(
        statusCode: Int,
        message: String,
        raw: String?
    ): NetworkError {
        return when (statusCode) {
            in 400..499 -> NetworkError.ClientError(
                message = message,
                statusCode = statusCode,
                errorBody = raw
            )
            in 500..599 -> NetworkError.ServerError(
                message = message,
                statusCode = statusCode,
                errorBody = raw
            )
            else -> NetworkError.Unknown(
                message = message,
                cause = null
            )
        }
    }

    private fun Headers.toSingleValueMap(): Map<String, String> =
        names().associateWith { name -> values(name).firstOrNull().orEmpty() }

    private fun isUnit(type: Type): Boolean {
        return type == Unit::class.java || type == Void.TYPE || type == Void::class.java
    }

    private fun defaultMessageForCode(code: Int): String = when (code) {
        in 400..499 -> "Client error ($code)"
        in 500..599 -> "Server error ($code)"
        else -> "HTTP $code"
    }

    private fun ResponseBody.consume(): String? = use { it.string() }
}
