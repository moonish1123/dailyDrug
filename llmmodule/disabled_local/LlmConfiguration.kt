package com.llmmodule.domain.config

import android.content.Context
import com.llmmodule.domain.model.LlmProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * Abstraction for retrieving provider selection and credentials.
 *
 * Implemented by the host application so LlmModule stays reusable.
 */
interface LlmConfiguration {
    suspend fun activeProvider(): LlmProvider?
    suspend fun apiKey(provider: LlmProvider): String?
}

/**
 * Local LLM specific configuration
 */
data class LocalLlmConfig(
    val modelPath: String,
    val tokenizerPath: String,
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val repetitionPenalty: Float = 1.1f,
    val useLora: Boolean = false,
    val loraPath: String? = null,
    val enableKvCache: Boolean = true,
    val maxCacheTokens: Int = 1024,
    val threadCount: Int = -1, // -1 = auto-detect
    val memoryThresholdMB: Long = 2048L,
    val timeoutMs: Long = 30000L,
    val systemPrompt: String = buildString {
        appendLine("당신은 약물 복용 코칭 전문 AI입니다.")
        appendLine("사용자의 질문에 친절하고 정확하게 답변해주세요.")
        appendLine()
        appendLine("지침:")
        appendLine("- 의학적 조언은 제공하지 말고, 일반적인 정보만 제공하세요")
        appendLine("- 부작용이나 심각한 증상이 있다면 즉시 의사와 상담하라고 알려주세요")
        appendLine("- 정확하고 이해하기 쉬운 언어를 사용하세요")
        appendLine("- 한국어로 답변하세요")
    }
)

/**
 * Local LLM configuration manager
 * Handles persistence and retrieval of local LLM settings
 */
@Singleton
class LlmConfigurationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("llm_config", Context.MODE_PRIVATE)

    fun getLocalLlmConfig(): LocalLlmConfig {
        return LocalLlmConfig(
            modelPath = prefs.getString("model_path", "llama-7b-4bit-q8.pte") ?: "llama-7b-4bit-q8.pte",
            tokenizerPath = prefs.getString("tokenizer_path", "tokenizer.model") ?: "tokenizer.model",
            maxTokens = prefs.getInt("max_tokens", 512),
            temperature = prefs.getFloat("temperature", 0.7f),
            topK = prefs.getInt("top_k", 40),
            topP = prefs.getFloat("top_p", 0.9f),
            repetitionPenalty = prefs.getFloat("repetition_penalty", 1.1f),
            useLora = prefs.getBoolean("use_lora", false),
            loraPath = prefs.getString("lora_path", null),
            enableKvCache = prefs.getBoolean("enable_kv_cache", true),
            maxCacheTokens = prefs.getInt("max_cache_tokens", 1024),
            threadCount = prefs.getInt("thread_count", -1),
            memoryThresholdMB = prefs.getLong("memory_threshold_mb", 2048L),
            timeoutMs = prefs.getLong("timeout_ms", 30000L),
            systemPrompt = prefs.getString("system_prompt", LocalLlmConfig().systemPrompt) ?: LocalLlmConfig().systemPrompt
        )
    }

    fun saveLocalLlmConfig(config: LocalLlmConfig) {
        prefs.edit().apply {
            putString("model_path", config.modelPath)
            putString("tokenizer_path", config.tokenizerPath)
            putInt("max_tokens", config.maxTokens)
            putFloat("temperature", config.temperature)
            putInt("top_k", config.topK)
            putFloat("top_p", config.topP)
            putFloat("repetition_penalty", config.repetitionPenalty)
            putBoolean("use_lora", config.useLora)
            putString("lora_path", config.loraPath)
            putBoolean("enable_kv_cache", config.enableKvCache)
            putInt("max_cache_tokens", config.maxCacheTokens)
            putInt("thread_count", config.threadCount)
            putLong("memory_threshold_mb", config.memoryThresholdMB)
            putLong("timeout_ms", config.timeoutMs)
            putString("system_prompt", config.systemPrompt)
            apply()
        }
    }

    /**
     * Reset to default configuration
     */
    fun resetToDefaults() {
        saveLocalLlmConfig(
            LocalLlmConfig(
                modelPath = "llama-7b-4bit-q8.pte",
                tokenizerPath = "tokenizer.model",
                maxTokens = 512,
                temperature = 0.7f,
                topK = 40,
                topP = 0.9f,
                repetitionPenalty = 1.1f,
                useLora = false,
                loraPath = null,
                enableKvCache = true,
                maxCacheTokens = 1024,
                threadCount = -1,
                memoryThresholdMB = 2048L,
                timeoutMs = 30000L,
                systemPrompt = buildString {
                    appendLine("당신은 약물 복용 코칭 전문 AI입니다.")
                    appendLine("사용자의 질문에 친절하고 정확하게 답변해주세요.")
                    appendLine()
                    appendLine("지침:")
                    appendLine("- 의학적 조언은 제공하지 말고, 일반적인 정보만 제공하세요")
                    appendLine("- 부작용이나 심각한 증상이 있다면 즉시 의사와 상담하라고 알려주세요")
                    appendLine("- 정확하고 이해하기 쉬운 언어를 사용하세요")
                    appendLine("- 한국어로 답변하세요")
                }
            )
        )
    }

    /**
     * Get memory-optimized configuration based on device capabilities
     */
    fun getOptimizedConfig(): LocalLlmConfig {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val availableProcessors = runtime.availableProcessors()

        return LocalLlmConfig(
            modelPath = "llama-7b-4bit-q8.pte",
            tokenizerPath = "tokenizer.model",
            maxTokens = when {
                maxMemory > 4L * 1024 * 1024 * 1024 -> 512 // >4GB RAM
                maxMemory > 2L * 1024 * 1024 * 1024 -> 256 // >2GB RAM
                else -> 128 // Low memory devices
            },
            temperature = 0.7f,
            topK = 40,
            topP = 0.9f,
            repetitionPenalty = 1.1f,
            useLora = false,
            loraPath = null,
            enableKvCache = maxMemory > 2L * 1024 * 1024 * 1024, // Only enable with >2GB RAM
            maxCacheTokens = when {
                maxMemory > 4L * 1024 * 1024 * 1024 -> 1024
                maxMemory > 2L * 1024 * 1024 * 1024 -> 512
                else -> 256
            },
            threadCount = when {
                availableProcessors >= 8 -> availableProcessors - 2
                availableProcessors >= 4 -> availableProcessors - 1
                else -> 2
            },
            memoryThresholdMB = when {
                maxMemory > 4L * 1024 * 1024 * 1024 -> 1024L
                maxMemory > 2L * 1024 * 1024 * 1024 -> 512L
                else -> 256L
            },
            timeoutMs = 30000L,
            systemPrompt = buildString {
                appendLine("당신은 약물 복용 코칭 전문 AI입니다.")
                appendLine("사용자의 질문에 친절하고 정확하게 답변해주세요.")
                appendLine()
                appendLine("지침:")
                appendLine("- 의학적 조언은 제공하지 말고, 일반적인 정보만 제공하세요")
                appendLine("- 부작용이나 심각한 증상이 있다면 즉시 의사와 상담하라고 알려주세요")
                appendLine("- 정확하고 이해하기 쉬운 언어를 사용하세요")
                appendLine("- 한국어로 답변하세요")
            }
        )
    }

    /**
     * Validate configuration
     */
    fun validateConfig(config: LocalLlmConfig): List<String> {
        val errors = mutableListOf<String>()

        if (config.modelPath.isBlank()) {
            errors.add("모델 경로가 비어있습니다")
        }

        if (config.tokenizerPath.isBlank()) {
            errors.add("토크나이저 경로가 비어있습니다")
        }

        if (config.maxTokens <= 0 || config.maxTokens > 2048) {
            errors.add("최대 토큰 수는 1-2048 사이여야 합니다")
        }

        if (config.temperature < 0.1f || config.temperature > 2.0f) {
            errors.add("온도는 0.1-2.0 사이여야 합니다")
        }

        if (config.topK <= 0 || config.topK > 1000) {
            errors.add("Top-K는 1-1000 사이여야 합니다")
        }

        if (config.topP <= 0.0f || config.topP > 1.0f) {
            errors.add("Top-P는 0.0-1.0 사이여야 합니다")
        }

        if (config.repetitionPenalty < 1.0f || config.repetitionPenalty > 2.0f) {
            errors.add("반복 패널티는 1.0-2.0 사이여야 합니다")
        }

        return errors
    }
}
