package com.llmmodule.domain.config

import com.llmmodule.domain.model.LlmProvider

/**
 * Abstraction for retrieving provider selection and credentials.
 *
 * Implemented by the host application so LlmModule stays reusable.
 */
interface LlmConfiguration {
    suspend fun activeProvider(): LlmProvider?
    suspend fun apiKey(provider: LlmProvider): String?
}
