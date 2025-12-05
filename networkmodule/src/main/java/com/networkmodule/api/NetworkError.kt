package com.networkmodule.api

/**
 * Strongly typed error contract for network failures.
 */
sealed class NetworkError {

    abstract val message: String
    abstract val cause: Throwable?

    data class NoConnection(
        override val message: String = "No internet connection available",
        override val cause: Throwable? = null
    ) : NetworkError()

    data class Timeout(
        override val message: String = "Request timed out",
        override val cause: Throwable? = null,
        val timeoutType: TimeoutType = TimeoutType.READ
    ) : NetworkError() {
        enum class TimeoutType { CONNECT, READ, WRITE }
    }

    data class ClientError(
        override val message: String,
        override val cause: Throwable? = null,
        val statusCode: Int,
        val errorBody: String? = null
    ) : NetworkError() {
        fun isUnauthorized(): Boolean = statusCode == 401
        fun isForbidden(): Boolean = statusCode == 403
        fun isNotFound(): Boolean = statusCode == 404
        fun isRateLimited(): Boolean = statusCode == 429
    }

    data class ServerError(
        override val message: String = "Server error",
        override val cause: Throwable? = null,
        val statusCode: Int,
        val errorBody: String? = null
    ) : NetworkError()

    data class ParseError(
        override val message: String = "Failed to parse response",
        override val cause: Throwable? = null,
        val rawResponse: String? = null
    ) : NetworkError()

    data class SslError(
        override val message: String = "SSL certificate validation failed",
        override val cause: Throwable? = null
    ) : NetworkError()

    data class Unknown(
        override val message: String = "Unknown error occurred",
        override val cause: Throwable? = null
    ) : NetworkError()

    fun toException(): Exception = when (this) {
        is NoConnection -> java.net.UnknownHostException(message).also { cause?.let(it::initCause) }
        is Timeout -> java.net.SocketTimeoutException(message).also { cause?.let(it::initCause) }
        is ClientError -> HttpException(statusCode, message, cause)
        is ServerError -> HttpException(statusCode, message, cause)
        is ParseError -> kotlinx.serialization.SerializationException(message, cause)
        is SslError -> javax.net.ssl.SSLException(message, cause)
        is Unknown -> RuntimeException(message, cause)
    }
}

class HttpException(
    val statusCode: Int,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)
