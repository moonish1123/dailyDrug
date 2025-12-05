package com.llmmodule.domain.model

/**
 * Normalised LLM response.
 */
data class LlmResponse(
    val text: String,
    val provider: LlmProvider,
    val usage: TokenUsage? = null,
    val raw: Any? = null
) {
    data class TokenUsage(
        val inputTokens: Int? = null,
        val outputTokens: Int? = null,
        val totalTokens: Int? = null
    )
}
