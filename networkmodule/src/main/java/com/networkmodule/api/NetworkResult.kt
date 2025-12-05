package com.networkmodule.api

/**
 * Type-safe wrapper for network responses to avoid throwing unchecked exceptions.
 */
sealed class NetworkResult<out T> {

    /**
     * Successful response with deserialized data.
     */
    data class Success<T>(
        val data: T,
        val statusCode: Int = 200,
        val headers: Map<String, String> = emptyMap()
    ) : NetworkResult<T>()

    /**
     * Failed response with structured error information.
     */
    data class Error(
        val error: NetworkError,
        val statusCode: Int? = null,
        val rawResponse: String? = null
    ) : NetworkResult<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw error.toException()
    }

    inline fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data), statusCode, headers)
        is Error -> this
    }

    inline fun onSuccess(block: (T) -> Unit): NetworkResult<T> {
        if (this is Success) block(data)
        return this
    }

    inline fun onError(block: (NetworkError) -> Unit): NetworkResult<T> {
        if (this is Error) block(error)
        return this
    }
}

suspend fun <T> NetworkResult<T>.suspendOnSuccess(
    block: suspend (T) -> Unit
): NetworkResult<T> {
    if (this is NetworkResult.Success) block(data)
    return this
}
