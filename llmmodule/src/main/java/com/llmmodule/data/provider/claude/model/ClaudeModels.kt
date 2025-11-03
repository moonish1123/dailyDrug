package com.llmmodule.data.provider.claude.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ClaudeMessagesRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<ClaudeMessage>,
    val temperature: Double? = null,
    val system: String? = null
)

@Serializable
internal data class ClaudeMessage(
    val role: String,
    val content: String
)

@Serializable
internal data class ClaudeMessagesResponse(
    val content: List<ClaudeResponseContent> = emptyList(),
    val usage: ClaudeUsage? = null
)

@Serializable
internal data class ClaudeResponseContent(
    val type: String? = null,
    val text: String? = null
)

@Serializable
internal data class ClaudeUsage(
    @SerialName("input_tokens") val inputTokens: Int? = null,
    @SerialName("output_tokens") val outputTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null
)
