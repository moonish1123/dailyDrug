package com.llmmodule.domain.repository

import com.llmmodule.domain.model.LlmProvider
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
     *
     * @param request The text generation request
     * @param provider The LLM provider to use
     * @param apiKey The API key for the selected LLM provider
     */
    fun generateText(request: LlmRequest, provider: LlmProvider, apiKey: String?): Flow<LlmResult<LlmResponse>>
}
