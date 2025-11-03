package com.llmmodule.domain.repository

import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.model.LlmResponse
import com.llmmodule.domain.model.LlmResult
import kotlinx.coroutines.flow.Flow

/**
 * Entry point for all LLM interactions, exposed to consuming apps.
 */
interface LlmRepository {

    /**
     * Generates text based on the provided [LlmRequest].
     */
    fun generateText(request: LlmRequest): Flow<LlmResult<LlmResponse>>
}
