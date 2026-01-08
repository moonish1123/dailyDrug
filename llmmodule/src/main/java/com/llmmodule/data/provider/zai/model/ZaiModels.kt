package com.llmmodule.data.provider.zai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Z.AI Chat Completions Request
 * @see <a href="https://docs.z.ai/api-reference/llm/chat-completion">Z.AI API Reference</a>
 */
@Serializable
internal data class ZaiChatCompletionsRequest(
    val model: String,
    val messages: List<ZaiMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    @SerialName("top_p")
    val topP: Double? = null,
    val stream: Boolean? = null
)

/**
 * Z.AI Chat Message
 * - role: "user" | "assistant" | "system" | "tool"
 * - content: Message content (required for non-tool messages)
 */
@Serializable
internal data class ZaiMessage(
    val role: String,
    val content: String? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ZaiToolCall>? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null
)

/**
 * Extract reasoning content from message (for GLM-4.5 series thinking mode)
 */
internal fun ZaiMessage.getReasoningContent(): String? = reasoningContent

@Serializable
internal data class ZaiToolCall(
    val id: String,
    val type: String,
    val function: ZaiFunction
)

@Serializable
internal data class ZaiFunction(
    val name: String,
    val arguments: String
)

/**
 * Z.AI Chat Completions Response
 */
@Serializable
internal data class ZaiChatCompletionsResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<ZaiChoice>,
    val usage: ZaiUsage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
internal data class ZaiChoice(
    val index: Int,
    val message: ZaiMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

/**
 * Token usage statistics
 */
@Serializable
internal data class ZaiUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int,
    @SerialName("prompt_tokens_details")
    val promptTokensDetails: ZaiPromptTokensDetails? = null
)

@Serializable
internal data class ZaiPromptTokensDetails(
    @SerialName("cached_tokens")
    val cachedTokens: Int? = null
)
