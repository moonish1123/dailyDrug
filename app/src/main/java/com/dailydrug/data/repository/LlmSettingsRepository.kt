package com.dailydrug.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dailydrug.data.model.LlmSettings
import com.llmmodule.domain.model.LlmProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LLM 설정을 관리하는 Repository
 * DataStore를 사용하여 영구적으로 설정 저장
 */
@Singleton
class LlmSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "llm_settings")

    private val selectedProviderKey = stringPreferencesKey("selected_provider")
    private val claudeApiKeyKey = stringPreferencesKey("claude_api_key")
    private val gptApiKeyKey = stringPreferencesKey("gpt_api_key")
    private val openAiApiKeyKey = stringPreferencesKey("openai_api_key")
    private val localLlmEnabledKey = booleanPreferencesKey("local_llm_enabled")
    private val autoSwitchToOfflineKey = booleanPreferencesKey("auto_switch_to_offline")
    private val preferredOnlineProviderKey = stringPreferencesKey("preferred_online_provider")

    /**
     * 현재 LLM 설정을 Flow로 반환
     */
    fun getSettings(): Flow<LlmSettings> {
        return context.dataStore.data.map { preferences ->
            val selectedProviderString = preferences[selectedProviderKey] ?: LlmProvider.Gpt.id
            val selectedProvider = LlmProvider.fromId(selectedProviderString) ?: LlmProvider.Gpt

            val preferredOnlineProviderString = preferences[preferredOnlineProviderKey] ?: LlmProvider.Gpt.id
            val preferredOnlineProvider = LlmProvider.fromId(preferredOnlineProviderString) ?: LlmProvider.Gpt

            LlmSettings(
                selectedProvider = selectedProvider,
                claudeApiKey = preferences[claudeApiKeyKey] ?: "",
                gptApiKey = preferences[gptApiKeyKey] ?: "",
                openAiApiKey = preferences[openAiApiKeyKey] ?: "",
                localLlmEnabled = preferences[localLlmEnabledKey] ?: false,
                autoSwitchToOffline = preferences[autoSwitchToOfflineKey] ?: false,
                preferredOnlineProvider = preferredOnlineProvider
            )
        }
    }

    /**
     * 선택된 프로바이더 업데이트
     */
    suspend fun updateSelectedProvider(provider: LlmProvider) {
        context.dataStore.edit { preferences ->
            preferences[selectedProviderKey] = provider.id
        }
    }

    /**
     * API 키 업데이트
     */
    suspend fun updateApiKey(provider: LlmProvider, apiKey: String) {
        context.dataStore.edit { preferences ->
            when (provider) {
                is LlmProvider.Claude -> preferences[claudeApiKeyKey] = apiKey
                is LlmProvider.Gpt -> preferences[gptApiKeyKey] = apiKey
                is LlmProvider.OpenAI -> preferences[openAiApiKeyKey] = apiKey
                is LlmProvider.Local -> {
                    // Local은 API 키가 필요 없음
                }
            }
        }
    }

    /**
     * API 키 가져오기
     */
    suspend fun getApiKey(provider: LlmProvider): String {
        return when (provider) {
            is LlmProvider.Claude -> {
                context.dataStore.data.map { it[claudeApiKeyKey] ?: "" }.first()
            }
            is LlmProvider.Gpt -> {
                context.dataStore.data.map { it[gptApiKeyKey] ?: "" }.first()
            }
            is LlmProvider.OpenAI -> {
                context.dataStore.data.map { it[openAiApiKeyKey] ?: "" }.first()
            }
            is LlmProvider.Local -> {
                ""
            }
        }
    }

    /**
     * Local LLM 활성화 상태 업데이트
     */
    suspend fun updateLocalLlmEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[localLlmEnabledKey] = enabled
        }
    }

    /**
     * 오프라인 자동 전환 활성화 상태 업데이트
     */
    suspend fun updateAutoSwitchToOffline(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[autoSwitchToOfflineKey] = enabled
        }
    }

    /**
     * 선호하는 온라인 프로바이더 업데이트
     */
    suspend fun updatePreferredOnlineProvider(provider: LlmProvider) {
        if (provider.isOnline) {
            context.dataStore.edit { preferences ->
                preferences[preferredOnlineProviderKey] = provider.id
            }
        }
    }

    /**
     * 모든 설정을 한 번에 업데이트
     */
    suspend fun updateSettings(settings: LlmSettings) {
        context.dataStore.edit { preferences ->
            preferences[selectedProviderKey] = settings.selectedProvider.id
            preferences[claudeApiKeyKey] = settings.claudeApiKey
            preferences[gptApiKeyKey] = settings.gptApiKey
            preferences[openAiApiKeyKey] = settings.openAiApiKey
            preferences[localLlmEnabledKey] = settings.localLlmEnabled
            preferences[autoSwitchToOfflineKey] = settings.autoSwitchToOffline
            preferences[preferredOnlineProviderKey] = settings.preferredOnlineProvider.id
        }
    }

    /**
     * 설정 초기화 (기본값으로 리셋)
     */
    suspend fun resetToDefaults() {
        updateSettings(getDefaultLlmSettings())
    }

    /**
     * 설정 내보내 (백업용)
     */
    suspend fun exportSettings(): Map<String, String> {
        return context.dataStore.data.map { preferences ->
            mapOf(
                "selected_provider" to (preferences[selectedProviderKey] ?: ""),
                "claude_api_key" to (preferences[claudeApiKeyKey] ?: ""),
                "gpt_api_key" to (preferences[gptApiKeyKey] ?: ""),
                "openai_api_key" to (preferences[openAiApiKeyKey] ?: ""),
                "local_llm_enabled" to (preferences[localLlmEnabledKey]?.toString() ?: "true"),
                "auto_switch_to_offline" to (preferences[autoSwitchToOfflineKey]?.toString() ?: "true"),
                "preferred_online_provider" to (preferences[preferredOnlineProviderKey] ?: "")
            )
        }.first()
    }

    /**
     * 설정 가져오기 (백업 복원용)
     */
    suspend fun importSettings(exportedSettings: Map<String, String>) {
        context.dataStore.edit { preferences ->
            preferences[selectedProviderKey] = exportedSettings["selected_provider"] ?: LlmProvider.Gpt.id
            preferences[claudeApiKeyKey] = exportedSettings["claude_api_key"] ?: ""
            preferences[gptApiKeyKey] = exportedSettings["gpt_api_key"] ?: ""
            preferences[openAiApiKeyKey] = exportedSettings["openai_api_key"] ?: ""
            preferences[localLlmEnabledKey] = (exportedSettings["local_llm_enabled"] ?: "false").toBoolean()
            preferences[autoSwitchToOfflineKey] = (exportedSettings["auto_switch_to_offline"] ?: "false").toBoolean()
            preferences[preferredOnlineProviderKey] = exportedSettings["preferred_online_provider"] ?: LlmProvider.Gpt.id
        }
    }
}

/**
 * 기본 LLM 설정 생성
 */
private fun getDefaultLlmSettings(): LlmSettings {
    return LlmSettings(
        selectedProvider = LlmProvider.Gpt,
        claudeApiKey = "",
        gptApiKey = "",
        openAiApiKey = "",
        localLlmEnabled = false,
        autoSwitchToOffline = false,
        preferredOnlineProvider = LlmProvider.Gpt
    )
}