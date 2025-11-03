package com.llmmodule.data.provider.local

import com.llmmodule.data.provider.LlmService
import com.llmmodule.domain.model.LlmProvider
import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.model.LlmResponse
import com.llmmodule.domain.model.LlmResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Placeholder local provider used for offline testing or demos.
 */
internal class LocalLlmService : LlmService {

    override val provider: LlmProvider = LlmProvider.Local

    override fun generateText(request: LlmRequest, apiKey: String?): Flow<LlmResult<LlmResponse>> = flow {
        emit(
            LlmResult.Success(
                LlmResponse(
                    text = "[LOCAL] ${request.prompt}",
                    provider = provider
                )
            )
        )
    }
}
