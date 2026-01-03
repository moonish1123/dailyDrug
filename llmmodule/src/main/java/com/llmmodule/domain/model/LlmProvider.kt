package com.llmmodule.domain.model

/**
 * Supported Large Language Model providers.
 *
 * Stored identifiers map to persisted configuration values such as
 * "claude/KEY" or "gpt/KEY". Local provider does not require a key.
 */
sealed class LlmProvider(val id: String, val displayName: String, val isOnline: Boolean) {
    data object Claude : LlmProvider("claude", "Claude (Anthropic)", true)
    data object Gpt : LlmProvider("gpt", "GPT (OpenAI)", true)
    data object OpenAI : LlmProvider("openai", "OpenAI", true)
    data object Local : LlmProvider("local", "Local LLM", false)

    companion object {
        private val providers = listOf(Claude, Gpt, OpenAI, Local)

        fun getAllProviders(): List<LlmProvider> = providers

        fun getOnlineProviders(): List<LlmProvider> = providers.filter { it.isOnline }

        fun getOfflineProviders(): List<LlmProvider> = providers.filter { !it.isOnline }

        fun fromId(id: String?): LlmProvider? =
            providers.firstOrNull { it.id.equals(id, ignoreCase = true) }

        /**
         * Parse keys stored in the format "provider/actualKey".
         *
         * Returns null if the format is invalid.
         */
        fun parseApiKey(rawKey: String?): Pair<LlmProvider, String>? {
            if (rawKey.isNullOrBlank()) return null
            val delimiterIndex = rawKey.indexOf('/')
            if (delimiterIndex <= 0 || delimiterIndex >= rawKey.lastIndex) return null

            val providerId = rawKey.substring(0, delimiterIndex)
            val key = rawKey.substring(delimiterIndex + 1)

            val provider = fromId(providerId) ?: return null
            if (key.isBlank()) return null
            return provider to key
        }
    }
}
