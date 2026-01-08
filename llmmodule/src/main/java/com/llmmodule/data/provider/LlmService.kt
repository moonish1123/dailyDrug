package com.llmmodule.data.provider

import com.llmmodule.domain.model.*
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
