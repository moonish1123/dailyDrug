package com.llmmodule.domain.usecase

import com.llmmodule.domain.model.LlmProvider
import javax.inject.Inject

class ParseApiKeyUseCase @Inject constructor() {
    operator fun invoke(rawKey: String?): Pair<LlmProvider, String>? =
        LlmProvider.parseApiKey(rawKey)
}
