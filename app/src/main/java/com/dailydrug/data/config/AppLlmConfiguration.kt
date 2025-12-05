package com.dailydrug.data.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.llmmodule.domain.config.LlmConfiguration
import com.llmmodule.domain.model.LlmProvider
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.llmDataStore by preferencesDataStore(name = "llm_settings")

/**
 * Bridges DailyDrug settings to the reusable LlmModule configuration contract.
 */
@Singleton
class AppLlmConfiguration @Inject constructor(
    @ApplicationContext private val context: Context
) : LlmConfiguration {

    private val activeKey = stringPreferencesKey("llm_active_key")

    override suspend fun activeProvider(): LlmProvider? {
        val raw = readRawKey() ?: return LlmProvider.Local
        return LlmProvider.parseApiKey(raw)?.first ?: LlmProvider.fromId(raw)
    }

    override suspend fun apiKey(provider: LlmProvider): String? {
        val raw = readRawKey() ?: return null
        val parsed = LlmProvider.parseApiKey(raw) ?: return null
        return parsed.takeIf { it.first == provider }?.second
    }

    suspend fun updateConfiguration(rawKey: String) {
        context.llmDataStore.edit { prefs ->
            prefs[activeKey] = rawKey
        }
    }

    suspend fun clearConfiguration() {
        context.llmDataStore.edit { prefs ->
            prefs.remove(activeKey)
        }
    }

    private suspend fun readRawKey(): String? {
        return context.llmDataStore.data
            .map { prefs -> prefs[activeKey] }
            .first()
    }
}
