package com.llmmodule.domain.model

/**
 * Result wrapper for LLM operations.
 */
sealed class LlmResult<out T> {

    data class Success<T>(val data: T) : LlmResult<T>()
    data class Error(val error: LlmError) : LlmResult<Nothing>()

    inline fun onSuccess(block: (T) -> Unit): LlmResult<T> {
        if (this is Success) block(data)
        return this
    }

    inline fun onError(block: (LlmError) -> Unit): LlmResult<T> {
        if (this is Error) block(error)
        return this
    }
}
