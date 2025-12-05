package com.networkmodule.api

import android.util.Log

/**
 * Logging abstraction so consumers can plug their own logger if required.
 */
interface NetworkLogger {

    fun logRequest(url: String, method: String, headers: Map<String, String>, body: String?)
    fun logResponse(url: String, statusCode: Int, body: String?, durationMs: Long)
    fun logError(url: String, error: NetworkError)

    companion object {
        val DEFAULT: NetworkLogger = object : NetworkLogger {
            override fun logRequest(url: String, method: String, headers: Map<String, String>, body: String?) {
                Log.d("Network", "→ $method $url headers=$headers body=${body.orEmpty()}")
            }

            override fun logResponse(url: String, statusCode: Int, body: String?, durationMs: Long) {
                Log.d("Network", "← $statusCode $url (${durationMs}ms) body=${body.orEmpty()}")
            }

            override fun logError(url: String, error: NetworkError) {
                Log.e("Network", "✗ $url: ${error.message}", error.cause)
            }
        }

        val NONE: NetworkLogger = object : NetworkLogger {
            override fun logRequest(url: String, method: String, headers: Map<String, String>, body: String?) = Unit
            override fun logResponse(url: String, statusCode: Int, body: String?, durationMs: Long) = Unit
            override fun logError(url: String, error: NetworkError) = Unit
        }
    }
}
