package com.llmmodule.testing

import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.model.LlmResponse
import com.llmmodule.domain.model.LlmResult
import com.llmmodule.domain.repository.LlmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeLlmRepository : LlmRepository {

    private val responses = MutableSharedFlow<LlmResult<LlmResponse>>(replay = 1)

    override fun generateText(request: LlmRequest): Flow<LlmResult<LlmResponse>> =
        responses.asSharedFlow()

    suspend fun emit(result: LlmResult<LlmResponse>) {
        responses.emit(result)
    }
}
