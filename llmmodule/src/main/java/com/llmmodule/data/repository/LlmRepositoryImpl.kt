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

    private val serviceMap: Map<LlmProvider, LlmService> = services.associateBy { it.provider }

    override fun generateText(request: LlmRequest): Flow<LlmResult<LlmResponse>> = flow {
        // For now, default to Claude service
        // TODO: Use AppLlmConfiguration when configuration is fully integrated
        val activeProvider = LlmProvider.Claude

        val service = serviceMap[activeProvider]
            ?: run {
                emit(
                    LlmResult.Error(
                        LlmError.Provider(activeProvider, "No service registered for provider")
                    )
                )
                return@flow
            }

        // TODO: Get API key from AppLlmConfiguration
        val apiKey = ""

        emitAll(service.generateText(request, apiKey))
    }.catch { throwable ->
        emit(LlmResult.Error(LlmError.fromThrowable(throwable)))
    }
}
