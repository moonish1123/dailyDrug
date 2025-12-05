package com.networkmodule.testing

import com.networkmodule.api.NetworkError
import com.networkmodule.api.NetworkResult

/**
 * Helper functions for testing.
 */
object NetworkTestUtils {

    fun <T> successResult(data: T, statusCode: Int = 200): NetworkResult<T> {
        return NetworkResult.Success(data, statusCode)
    }

    fun <T> errorResult(error: NetworkError, statusCode: Int? = null): NetworkResult<T> {
        return NetworkResult.Error(error, statusCode)
    }

    fun noConnectionError(): NetworkError = NetworkError.NoConnection()

    fun timeoutError(): NetworkError = NetworkError.Timeout()

    fun unauthorizedError(): NetworkError = NetworkError.ClientError(
        message = "Unauthorized",
        statusCode = 401
    )

    fun serverError(): NetworkError = NetworkError.ServerError(
        message = "Internal Server Error",
        statusCode = 500
    )
}
