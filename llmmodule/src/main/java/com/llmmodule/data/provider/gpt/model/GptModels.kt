package com.llmmodule.data.provider.gpt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class GptChatCompletionsRequest(
    val model: String,
    val messages: List<GptMessage>,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null
)

@Serializable
internal data class GptMessage(
    val role: String,
    val content: String
)

@Serializable
internal data class GptChatCompletionsResponse(
    val choices: List<GptChoice> = emptyList(),
    val usage: GptUsage? = null
)

@Serializable
internal data class GptChoice(
    val index: Int,
    val message: GptMessage? = null
)

@Serializable
internal data class GptUsage(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null
)
