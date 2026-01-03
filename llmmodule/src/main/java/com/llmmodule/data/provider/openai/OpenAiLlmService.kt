package com.llmmodule.data.provider.openai

import com.llmmodule.data.provider.LlmService
import com.llmmodule.domain.model.LlmError
import com.llmmodule.domain.model.LlmProvider
import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.model.LlmResponse
import com.llmmodule.domain.model.LlmResult
import com.networkmodule.api.NetworkClientFactory
import com.networkmodule.api.NetworkConfig
import com.networkmodule.api.createService
import java.io.IOException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val OPENAI_BASE_URL = "https://api.openai.com/"
private const val DEFAULT_MODEL = "gpt-3.5-turbo"
private const val DEFAULT_MAX_TOKENS = 1024

/**
 * OpenAI API service implementation
 * Provides access to GPT models via OpenAI's official API
 */
internal class OpenAiLlmService @Inject constructor(
    networkClientFactory: NetworkClientFactory
) : LlmService {

    override val provider: LlmProvider = LlmProvider.OpenAI

    private val api: OpenAiApiService = networkClientFactory.createService<OpenAiApiService>(
        baseUrl = OPENAI_BASE_URL,
        config = NetworkConfig(
            readTimeoutSeconds = 60,
            writeTimeoutSeconds = 60
        )
    )

    override fun generateText(request: LlmRequest, apiKey: String?): Flow<LlmResult<LlmResponse>> = flow {
        if (apiKey.isNullOrBlank()) {
            emit(LlmResult.Error(LlmError.ApiKeyMissing(provider)))
            return@flow
        }

        val systemText = request.systemInstructions
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n")

        val messages = mutableListOf<OpenAiMessage>()

        // Add system message if present
        systemText?.let {
            messages.add(OpenAiMessage(role = "system", content = it))
        }

        // Add user message
        messages.add(OpenAiMessage(role = "user", content = request.prompt))

        val maxTokens = request.maxOutputTokens?.takeIf { it > 0 } ?: DEFAULT_MAX_TOKENS

        try {
            val response = api.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = OpenAiRequest(
                    model = DEFAULT_MODEL,
                    messages = messages,
                    maxTokens = maxTokens,
                    temperature = request.temperature ?: 0.7
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) {
                    emit(LlmResult.Error(LlmError.Provider(provider, "OpenAI response body was empty")))
                } else {
                    val content = body.choices.firstOrNull()?.message?.content
                        ?.takeIf { it.isNotBlank() }

                    if (content == null) {
                        emit(
                            LlmResult.Error(
                                LlmError.Provider(
                                    provider = provider,
                                    message = "OpenAI response did not contain text"
                                )
                            )
                        )
                    } else {
                        val usage = body.usage?.let {
                            LlmResponse.TokenUsage(
                                inputTokens = it.promptTokens,
                                outputTokens = it.completionTokens,
                                totalTokens = it.totalTokens
                            )
                        }
                        emit(
                            LlmResult.Success(
                                LlmResponse(
                                    text = content.trim(),
                                    provider = provider,
                                    usage = usage,
                                    raw = body
                                )
                            )
                        )
                    }
                }
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.use { it.string() }
                emit(
                    LlmResult.Error(
                        LlmError.Provider(
                            provider = provider,
                            message = errorBody?.takeIf { it.isNotBlank() }
                                ?: "OpenAI request failed with HTTP $code"
                        )
                    )
                )
            }
        } catch (io: IOException) {
            emit(LlmResult.Error(LlmError.Network(io.message ?: "Network error", io)))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            emit(LlmResult.Error(LlmError.Unknown(throwable.message ?: "Unknown error", throwable)))
        }
    }
}

// OpenAI API Data Classes
@kotlinx.serialization.Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    @kotlinx.serialization.SerialName("max_tokens")
    val maxTokens: Int,
    val temperature: Double? = null,
    val stream: Boolean = false
)

@kotlinx.serialization.Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@kotlinx.serialization.Serializable
data class OpenAiResponse(
    val id: String,
    @kotlinx.serialization.SerialName("object")
    val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage?
)

@kotlinx.serialization.Serializable
data class OpenAiChoice(
    val index: Int,
    val message: OpenAiMessage,
    @kotlinx.serialization.SerialName("finish_reason")
    val finishReason: String
)

@kotlinx.serialization.Serializable
data class OpenAiUsage(
    @kotlinx.serialization.SerialName("prompt_tokens")
    val promptTokens: Int,
    @kotlinx.serialization.SerialName("completion_tokens")
    val completionTokens: Int,
    @kotlinx.serialization.SerialName("total_tokens")
    val totalTokens: Int
)