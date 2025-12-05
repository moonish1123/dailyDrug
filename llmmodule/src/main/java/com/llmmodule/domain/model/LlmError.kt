package com.llmmodule.domain.model

/**
 * Typed errors exposed by the LLM module.
 */
sealed class LlmError(open val message: String, open val cause: Throwable? = null) {

    data class ConfigurationMissing(
        override val message: String = "LLM provider configuration is missing"
    ) : LlmError(message)

    data class ApiKeyMissing(
        val provider: LlmProvider,
        override val message: String = "API key is missing for provider ${provider.id}"
    ) : LlmError(message)

    data class UnsupportedOperation(
        val provider: LlmProvider,
        override val message: String
    ) : LlmError(message)

    data class Network(
        override val message: String,
        override val cause: Throwable? = null
    ) : LlmError(message, cause)

    data class Provider(
        val provider: LlmProvider,
        override val message: String,
        override val cause: Throwable? = null
    ) : LlmError(message, cause)

    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : LlmError(message, cause)

    companion object {
        fun fromThrowable(throwable: Throwable): LlmError =
            Unknown(throwable.message ?: "Unexpected error", throwable)
    }
}
