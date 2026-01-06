package com.llmmodule.data.provider.zai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ZaiChatCompletionsRequest(
    val model: String,
    val messages: List<ZaiMessage>,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null
)

@Serializable
internal data class ZaiMessage(
    val role: String,
    val content: String
)

@Serializable
internal data class ZaiChatCompletionsResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<ZaiChoice> = emptyList(),
    val usage: ZaiUsage? = null
)

@Serializable
internal data class ZaiChoice(
    val index: Int,
    val message: ZaiMessage,
    @SerialName("finish_reason") val finishReason: String
)

@Serializable
internal data class ZaiUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)
