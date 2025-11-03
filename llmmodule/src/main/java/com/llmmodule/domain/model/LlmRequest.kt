package com.llmmodule.domain.model

/**
 * Request payload shared across LLM providers.
 */
data class LlmRequest(
    val prompt: String,
    val systemInstructions: List<String> = emptyList(),
    val temperature: Double? = null,
    val topK: Int? = null,
    val topP: Double? = null,
    val maxOutputTokens: Int? = null
)
