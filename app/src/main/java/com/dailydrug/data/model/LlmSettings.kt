package com.dailydrug.data.model

import com.llmmodule.domain.model.LlmProvider

/**
 * LLM 설정 상태를 나타내는 데이터 클래스
 */
data class LlmSettings(
    val selectedProvider: LlmProvider,
    val claudeApiKey: String = "",
    val gptApiKey: String = "",
    val openAiApiKey: String = "",
    val localLlmEnabled: Boolean = true,
    val autoSwitchToOffline: Boolean = true,
    val preferredOnlineProvider: LlmProvider = LlmProvider.Claude
) {
    /**
     * 현재 선택된 프로바이더의 API 키를 반환
     */
    fun getCurrentApiKey(): String? {
        return when (selectedProvider) {
            is LlmProvider.Claude -> claudeApiKey.takeIf { it.isNotBlank() }
            is LlmProvider.Gpt -> gptApiKey.takeIf { it.isNotBlank() }
            is LlmProvider.OpenAI -> openAiApiKey.takeIf { it.isNotBlank() }
            is LlmProvider.Local -> null
        }
    }

    /**
     * 프로바이더에 해당하는 API 키를 업데이트
     */
    fun updateApiKey(provider: LlmProvider, apiKey: String): LlmSettings {
        return when (provider) {
            is LlmProvider.Claude -> copy(claudeApiKey = apiKey)
            is LlmProvider.Gpt -> copy(gptApiKey = apiKey)
            is LlmProvider.OpenAI -> copy(openAiApiKey = apiKey)
            is LlmProvider.Local -> this
        }
    }

    /**
     * 현재 설정이 유효한지 확인
     */
    fun isValid(): Boolean {
        return when (selectedProvider) {
            is LlmProvider.Local -> true
            else -> getCurrentApiKey()?.isNotBlank() == true
        }
    }

    /**
     * 온라인 프로바이더 중 현재 API 키가 설정된 프로바이더 목록
     */
    fun getAvailableOnlineProviders(): List<LlmProvider> {
        val providers = mutableListOf<LlmProvider>()

        if (claudeApiKey.isNotBlank()) providers.add(LlmProvider.Claude)
        if (gptApiKey.isNotBlank()) providers.add(LlmProvider.Gpt)
        if (openAiApiKey.isNotBlank()) providers.add(LlmProvider.OpenAI)

        return providers
    }

    /**
     * 디버그 정보
     */
    fun toDebugString(): String {
        return """
            LLMSettings:
            - Selected Provider: ${selectedProvider.displayName}
            - Claude API Key: ${if (claudeApiKey.isNotBlank()) "***" else "Not Set"}
            - GPT API Key: ${if (gptApiKey.isNotBlank()) "***" else "Not Set"}
            - OpenAI API Key: ${if (openAiApiKey.isNotBlank()) "***" else "Not Set"}
            - Local LLM Enabled: $localLlmEnabled
            - Auto Switch: $autoSwitchToOffline
            - Preferred Online: ${preferredOnlineProvider.displayName}
            - Is Valid: ${isValid()}
            - Available Online: ${getAvailableOnlineProviders().map { it.displayName }}
        """.trimIndent()
    }
}

/**
 * LLM 설정 UI 상태
 */
data class LlmSettingsUiState(
    val settings: LlmSettings = LlmSettings(
        selectedProvider = LlmProvider.Gpt
    ),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showApiKeyDialog: Boolean = false,
    val editingProvider: LlmProvider? = null,
    val testConnectionInProgress: Boolean = false,
    val testConnectionResult: ConnectionTestResult? = null
) {
    fun isApiKeyConfigured(provider: LlmProvider): Boolean {
        return when (provider) {
            is LlmProvider.Claude -> settings.claudeApiKey.isNotBlank()
            is LlmProvider.Gpt -> settings.gptApiKey.isNotBlank()
            is LlmProvider.OpenAI -> settings.openAiApiKey.isNotBlank()
            is LlmProvider.Local -> true
        }
    }
}

/**
 * API 연결 테스트 결과
 */
data class ConnectionTestResult(
    val provider: LlmProvider,
    val success: Boolean,
    val message: String,
    val responseTime: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * LLM 설정 이벤트
 */
sealed class LlmSettingsEvent {
    data class ProviderSelected(val provider: LlmProvider) : LlmSettingsEvent()
    data class ApiKeyUpdated(val provider: LlmProvider, val apiKey: String) : LlmSettingsEvent()
    data class TestConnection(val provider: LlmProvider) : LlmSettingsEvent()
    data class ShowApiKeyDialog(val provider: LlmProvider) : LlmSettingsEvent()
    object DismissDialog : LlmSettingsEvent()
    object SaveSettings : LlmSettingsEvent()
}