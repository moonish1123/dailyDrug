package com.llmmodule.data.provider.claude

import com.llmmodule.data.provider.LlmService
import com.llmmodule.data.provider.claude.model.ClaudeMessage
import com.llmmodule.data.provider.claude.model.ClaudeMessagesRequest
import com.llmmodule.data.provider.claude.model.ClaudeResponseContent
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

private const val CLAUDE_BASE_URL = "https://api.anthropic.com/"
private const val CLAUDE_VERSION = "2023-06-01"
private const val DEFAULT_MODEL = "claude-3-haiku-20240307"
private const val DEFAULT_MAX_TOKENS = 1024

internal class ClaudeLlmService @Inject constructor(
    networkClientFactory: NetworkClientFactory
) : LlmService {

    override val provider: LlmProvider = LlmProvider.Claude

    private val api: ClaudeApiService = networkClientFactory.createService(
        baseUrl = CLAUDE_BASE_URL,
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

        val messages = listOf(
            ClaudeMessage(
                role = "user",
                content = request.prompt
            )
        )

        val maxTokens = request.maxOutputTokens?.takeIf { it > 0 } ?: DEFAULT_MAX_TOKENS

        try {
            val response = api.createMessage(
                apiKey = apiKey,
                version = CLAUDE_VERSION,
                request = ClaudeMessagesRequest(
                    model = DEFAULT_MODEL,
                    maxTokens = maxTokens,
                    messages = messages,
                    temperature = request.temperature,
                    system = systemText
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) {
                    emit(LlmResult.Error(LlmError.Provider(provider, "Claude response body was empty")))
                } else {
                    val primaryText = body.content
                        .firstOrNull { it.type.equals("text", ignoreCase = true) }
                        ?.text
                        ?.takeIf { it.isNotBlank() }

                    if (primaryText == null) {
                        emit(
                            LlmResult.Error(
                                LlmError.Provider(
                                    provider = provider,
                                    message = "Claude response did not contain text"
                                )
                            )
                        )
                    } else {
                        val usage = body.usage?.let {
                            LlmResponse.TokenUsage(
                                inputTokens = it.inputTokens,
                                outputTokens = it.outputTokens,
                                totalTokens = it.totalTokens
                            )
                        }
                        emit(
                            LlmResult.Success(
                                LlmResponse(
                                    text = primaryText,
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
                                ?: "Claude request failed with HTTP $code"
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
