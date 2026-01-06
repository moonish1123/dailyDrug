package com.llmmodule.data.provider.zai

import com.llmmodule.data.provider.LlmService
import com.llmmodule.data.provider.zai.model.ZaiChatCompletionsRequest
import com.llmmodule.data.provider.zai.model.ZaiMessage
import com.llmmodule.domain.model.LlmError
import com.llmmodule.domain.model.LlmProvider
import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.model.LlmResponse
import com.llmmodule.domain.model.LlmResult
import com.networkmodule.api.NetworkClientFactory
import com.networkmodule.api.NetworkConfig
import com.networkmodule.api.createService
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.collections.buildList

private const val ZAI_BASE_URL = "https://open.bigmodel.cn/"
private const val DEFAULT_MODEL = "glm-4.7"

internal class ZaiLlmService @Inject constructor(
    networkClientFactory: NetworkClientFactory
) : LlmService {

    override val provider: LlmProvider = LlmProvider.ZAI

    private val api: ZaiApiService = networkClientFactory.createService<ZaiApiService>(
        baseUrl = ZAI_BASE_URL,
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
                add(ZaiMessage(role = "system", content = instruction))
            }
            add(ZaiMessage(role = "user", content = request.prompt))
        }

        try {
            val response = api.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = ZaiChatCompletionsRequest(
                    model = DEFAULT_MODEL,
                    messages = messages,
                    maxTokens = request.maxOutputTokens,
                    temperature = request.temperature?.toDouble(),
                    topP = request.topP?.toDouble()
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) {
                    emit(
                        LlmResult.Error(
                            LlmError.Provider(provider, "Z.AI response body was empty")
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
                                    message = "Z.AI response did not contain text"
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
                                ?: "Z.AI request failed with HTTP $code"
                        )
                    )
                )
            }
        } catch (io: IOException) {
            emit(LlmResult.Error(LlmError.Network(io.message ?: "Network error", io)))
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            emit(LlmResult.Error(LlmError.Unknown(throwable.message ?: "Unknown error", throwable)))
        }
    }
}
