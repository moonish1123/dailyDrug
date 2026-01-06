package com.llmmodule.data.repository

import com.llmmodule.data.provider.LlmService
import com.llmmodule.domain.model.LlmError
import com.llmmodule.domain.model.LlmProvider
import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.model.LlmResponse
import com.llmmodule.domain.model.LlmResult
import com.llmmodule.domain.repository.LlmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class LlmRepositoryImpl(
    services: @JvmSuppressWildcards Set<LlmService>
) : LlmRepository {

    private val serviceMap: Map<String, LlmService> = services.associateBy { it.provider.id }

    override fun generateText(request: LlmRequest, provider: LlmProvider, apiKey: String?): Flow<LlmResult<LlmResponse>> = flow {
        // Validate API key first
        if (apiKey.isNullOrBlank()) {
            emit(LlmResult.Error(LlmError.ApiKeyMissing(provider)))
            return@flow
        }

        // Select the service for the specified provider
        val service = serviceMap[provider.id]
            ?: run {
                emit(
                    LlmResult.Error(
                        LlmError.Provider(provider, "No LLM service registered for ${provider.displayName}")
                    )
                )
                return@flow
            }

        emitAll(service.generateText(request, apiKey))
    }.catch { throwable ->
        emit(LlmResult.Error(LlmError.fromThrowable(throwable)))
    }
}
