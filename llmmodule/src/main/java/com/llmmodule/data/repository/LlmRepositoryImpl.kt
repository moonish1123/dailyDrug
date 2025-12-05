package com.llmmodule.data.repository

import com.llmmodule.data.provider.LlmService
import com.llmmodule.domain.config.LlmConfiguration
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
    private val configuration: LlmConfiguration?,
    services: @JvmSuppressWildcards Set<LlmService>
) : LlmRepository {

    private val serviceMap: Map<LlmProvider, LlmService> = services.associateBy { it.provider }

    override fun generateText(request: LlmRequest): Flow<LlmResult<LlmResponse>> = flow {
        val resolvedConfiguration = configuration
            ?: run {
                emit(LlmResult.Error(LlmError.ConfigurationMissing()))
                return@flow
            }

        val activeProvider = resolvedConfiguration.activeProvider()
            ?: run {
                emit(LlmResult.Error(LlmError.ConfigurationMissing()))
                return@flow
            }

        val service = serviceMap[activeProvider]
            ?: run {
                emit(
                    LlmResult.Error(
                        LlmError.Provider(activeProvider, "No service registered for provider")
                    )
                )
                return@flow
            }

        val apiKey = resolvedConfiguration.apiKey(activeProvider)

        emitAll(service.generateText(request, apiKey))
    }.catch { throwable ->
        emit(LlmResult.Error(LlmError.fromThrowable(throwable)))
    }
}
