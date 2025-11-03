package com.llmmodule.data.provider

import com.llmmodule.domain.model.LlmProvider
import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.model.LlmResponse
import com.llmmodule.domain.model.LlmResult
import kotlinx.coroutines.flow.Flow

/**
 * Provider-specific service. Each implementation handles mapping from the
 * shared [LlmRequest] contract to provider APIs.
 */
interface LlmService {
    val provider: LlmProvider

    fun generateText(
        request: LlmRequest,
        apiKey: String?
    ): Flow<LlmResult<LlmResponse>>
}
