package com.llmmodule.domain.model

/**
 * Supported Large Language Model providers.
 *
 * Stored identifiers map to persisted configuration values such as
 * "claude/KEY" or "gpt/KEY". Local provider does not require a key.
 */
sealed class LlmProvider(val id: String) {
    data object Claude : LlmProvider("claude")
    data object Gpt : LlmProvider("gpt")
    data object Local : LlmProvider("local")

    companion object {
        private val providers = listOf(Claude, Gpt, Local)

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
