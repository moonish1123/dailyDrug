package com.llmmodule.domain.model

/**
 * Supported Large Language Model providers.
 *
 * Stored identifiers map to persisted configuration values such as
 * "claude/KEY" or "gpt/KEY". Local provider does not require a key.
 */
sealed class LlmProvider(
    val id: String,
    val displayName: String,
    val isOnline: Boolean,
    val supportedModels: List<LlmModel> = emptyList(),
    val defaultModel: LlmModel? = null
) {
    data object Claude : LlmProvider(
        id = "claude",
        displayName = "Claude (Anthropic)",
        isOnline = true,
        supportedModels = listOf(
            LlmModel("claude-haiku-4-5-20251001", "Claude Haiku 4.5"),
            LlmModel("claude-sonnet-4-5-20250929", "Claude Sonnet 4.5")
        ),
        defaultModel = LlmModel("claude-haiku-4-5-20251001", "Claude Haiku 4.5")
    )

    data object Gpt : LlmProvider(
        id = "gpt",
        displayName = "GPT (OpenAI)",
        isOnline = true,
        supportedModels = listOf(
            LlmModel("gpt-5-mini", "GPT-5 Mini"),
            LlmModel("gpt-5.2", "GPT-5.2")
        ),
        defaultModel = LlmModel("gpt-5-mini", "GPT-5 Mini")
    )

    data object ZAI : LlmProvider(
        id = "zai",
        displayName = "Z.AI (GLM)",
        isOnline = true,
        supportedModels = listOf(
            LlmModel("glm-4.5-air", "GLM-4.5 Air"),
            LlmModel("glm-4.7", "GLM-4.7")
        ),
        defaultModel = LlmModel("glm-4.5-air", "GLM-4.5 Air")
    )

    data object Local : LlmProvider("local", "Local LLM", false)

    companion object {
        fun getAllProviders(): List<LlmProvider> = listOf(Claude, Gpt, ZAI, Local)

        fun getOnlineProviders(): List<LlmProvider> = getAllProviders().filter { it.isOnline }

        fun getOfflineProviders(): List<LlmProvider> = getAllProviders().filter { !it.isOnline }

        fun fromId(id: String?): LlmProvider? {
            if (id == null) return null

            // 하위 호환성: 이전 버전에서 "openai"로 저장된 ID를 Gpt로 매핑
            val normalizedId = when (id.lowercase()) {
                "openai" -> "gpt"
                else -> id.lowercase()
            }

            return getAllProviders().firstOrNull { it.id.equals(normalizedId, ignoreCase = true) }
        }

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

data class LlmModel(
    val id: String,
    val displayName: String
)
