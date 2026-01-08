package com.llmmodule.domain.usecase

import com.llmmodule.domain.model.*
import com.llmmodule.domain.repository.LlmRepository
import javax.inject.Inject

class GenerateTextUseCase @Inject constructor(
    private val repository: LlmRepository
) {
    operator fun invoke(request: LlmRequest, provider: LlmProvider, apiKey: String?) =
        repository.generateText(request, provider, apiKey)
}
