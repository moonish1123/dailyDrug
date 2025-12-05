package com.llmmodule.domain.usecase

import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.repository.LlmRepository
import javax.inject.Inject

class GenerateTextUseCase @Inject constructor(
    private val repository: LlmRepository
) {
    operator fun invoke(request: LlmRequest) = repository.generateText(request)
}
