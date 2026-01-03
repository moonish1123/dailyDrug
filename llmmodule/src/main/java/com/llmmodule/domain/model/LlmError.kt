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

    // Local LLM specific errors
    sealed class LocalLlmError(
        override val message: String,
        override val cause: Throwable? = null
    ) : LlmError(message, cause) {

        data class ModelLoadFailed(
            override val cause: Throwable? = null
        ) : LocalLlmError("Failed to load ExecuTorch model", cause)

        data class ModelNotFound(
            val modelPath: String
        ) : LocalLlmError("Model file not found: $modelPath")

        data class ModelCorrupted(
            val modelPath: String
        ) : LocalLlmError("Model file appears to be corrupted: $modelPath")

        data class TokenizerNotInitialized(
            override val cause: Throwable? = null
        ) : LocalLlmError("Tokenizer not properly initialized", cause)

        data class TokenizerModelNotFound(
            override val cause: Throwable? = null
        ) : LocalLlmError("Tokenizer model file not found", cause)

        data class TokenizationFailed(
            val details: String,
            override val cause: Throwable? = null
        ) : LocalLlmError("Text tokenization failed: $details", cause)

        data class InsufficientMemory(
            val requiredMB: Long,
            val availableMB: Long
        ) : LocalLlmError("Insufficient memory: ${requiredMB}MB required, ${availableMB}MB available")

        data class GenerationTimeout(
            val timeoutMs: Long
        ) : LocalLlmError("Text generation timed out after ${timeoutMs}ms")

        data class GenerationFailed(
            val details: String,
            override val cause: Throwable? = null
        ) : LocalLlmError("Text generation failed: $details", cause)

        data class InferenceNotInitialized(
            override val cause: Throwable? = null
        ) : LocalLlmError("ExecuTorch inference engine not initialized", cause)

        data class AssetCopyFailed(
            val assetName: String,
            override val cause: Throwable? = null
        ) : LocalLlmError("Failed to copy model asset: $assetName", cause)

        data class ConfigurationInvalid(
            val configKey: String,
            override val cause: Throwable? = null
        ) : LocalLlmError("Invalid configuration for: $configKey", cause)

        data class ContextLengthExceeded(
            val maxLength: Int,
            val actualLength: Int
        ) : LocalLlmError("Context length exceeded: $actualLength > $maxLength")
    }

    companion object {
        fun fromThrowable(throwable: Throwable): LlmError =
            Unknown(throwable.message ?: "Unexpected error", throwable)
    }
}
