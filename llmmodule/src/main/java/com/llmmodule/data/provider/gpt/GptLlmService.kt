package com.llmmodule.data.provider.gpt

import com.llmmodule.domain.model.*
import com.llmmodule.data.provider.LlmService
import com.llmmodule.data.provider.gpt.model.GptChatCompletionsRequest
import com.llmmodule.data.provider.gpt.model.GptMessage

import com.networkmodule.api.NetworkClientFactory
import com.networkmodule.api.NetworkConfig
import com.networkmodule.api.createService
import java.io.IOException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import kotlin.collections.buildList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val GPT_BASE_URL = "https://api.openai.com/"
private const val DEFAULT_MODEL = "gpt-3.5-turbo"

internal class GptLlmService @Inject constructor(
    networkClientFactory: NetworkClientFactory
) : LlmService {

    override val provider: LlmProvider = LlmProvider.Gpt

    private val api: GptApiService = networkClientFactory.createService<GptApiService>(
        baseUrl = GPT_BASE_URL,
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

        val messages = buildList {
            request.systemInstructions.forEach { instruction ->
                add(GptMessage(role = "system", content = instruction))
            }
            add(GptMessage(role = "user", content = request.prompt))
        }

        try {
            val response = api.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = GptChatCompletionsRequest(
                    model = request.model ?: DEFAULT_MODEL,
                    messages = messages,
                    maxTokens = request.maxOutputTokens,
                    temperature = request.temperature
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) {
                    emit(
                        LlmResult.Error(
                            LlmError.Provider(provider, "GPT response body was empty")
                        )
                    )
                } else {
                    val message = body.choices.firstOrNull()?.message
                    val text = message?.content
                    if (text.isNullOrBlank()) {
                        emit(
                            LlmResult.Error(
                                LlmError.Provider(
                                    provider = provider,
                                    message = "GPT response did not contain text"
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
                                    text = text,
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
                                ?: "GPT request failed with HTTP $code"
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
