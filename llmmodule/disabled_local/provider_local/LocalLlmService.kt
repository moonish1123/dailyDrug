package com.llmmodule.data.provider.local

import com.llmmodule.data.asset.ModelAssetManager
import com.llmmodule.data.local.ExecutorRunner
import com.llmmodule.data.provider.LlmService
import com.llmmodule.domain.model.LlmError
import com.llmmodule.domain.model.LlmProvider
import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.model.LlmResponse
import com.llmmodule.domain.model.LlmResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real ExecuTorch-based local LLM service
 * Provides on-device text generation without internet connectivity
 */
@Singleton
internal class LocalLlmService @Inject constructor(
    private val executorRunner: ExecutorRunner,
    private val assetManager: ModelAssetManager
) : LlmService {

    override val provider: LlmProvider = LlmProvider.Local

    override fun generateText(request: LlmRequest, apiKey: String?): Flow<LlmResult<LlmResponse>> = flow {
        try {
            // Check if model assets exist
            val assetsExist = assetManager.checkAssetsExist()
            if (!assetsExist) {
                throw LlmError.LocalLlmError.ModelNotFound("assets/models/")
            }

            // Initialize model if not already loaded
            if (!executorRunner.isModelLoaded()) {
                emit(LlmResult.Progress("모델 로딩 중..."))
                executorRunner.initialize()
            }

            emit(LlmResult.Progress("텍스트 생성 중..."))

            // Build enhanced prompt with system instructions
            val enhancedPrompt = buildMedicationPrompt(request.prompt)

            // Generate text with timeout
            val generatedText = withTimeout(60000L) { // 1 minute timeout
                val responseBuilder = StringBuilder()

                executorRunner.generate(request.copy(prompt = enhancedPrompt))
                    .onEach { token ->
                        responseBuilder.append(token)
                        emit(LlmResult.Progress("생성 중..."))
                    }
                    .catch { error ->
                        when (error) {
                            is LlmError -> throw error
                            else -> throw LlmError.LocalLlmError.GenerationFailed(
                                "Generation stream error: ${error.message}", error
                            )
                        }
                    }
                    .collect { /* Collect all tokens */ }

                responseBuilder.toString()
            }

            // Clean up response and format
            val cleanedResponse = cleanAndFormatResponse(generatedText)

            emit(
                LlmResult.Success(
                    LlmResponse(
                        text = cleanedResponse,
                        provider = provider,
                        metadata = mapOf(
                            "model" to "local-executorch",
                            "tokensGenerated" to cleanedResponse.length,
                            "generatedAt" to System.currentTimeMillis()
                        )
                    )
                )
            )

        } catch (e: LlmError) {
            emit(LlmResult.Error(e))
        } catch (e: Exception) {
            val error = LlmError.LocalLlmError.GenerationFailed(
                "Unexpected error during generation: ${e.message}", e
            )
            emit(LlmResult.Error(error))
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    /**
     * Build medication coaching prompt
     * Combines system instructions with user input
     */
    private fun buildMedicationPrompt(userInput: String): String {
        return """
            당신은 약물 복용 코칭 전문 AI입니다. 사용자의 질문에 친절하고 정확하게 답변해주세요.

            지침:
            - 의학적 조언은 제공하지 말고, 일반적인 정보만 제공하세요
            - 부작용이나 심각한 증상이 있다면 즉시 의사와 상담하라고 알려주세요
            - 정확하고 이해하기 쉬운 언어를 사용하세요
            - 한국어로 답변하세요
            - 복용 시간, 주의사항, 생활 습관 등 실용적인 조언을 제공하세요
            - 너무 긴 답변은 피하고 핵심적인 내용만 답아주세요

            사용자 질문: $userInput

            답변:
        """.trimIndent()
    }

    /**
     * Clean and format the generated response
     * Removes unwanted tokens, artifacts, and formats properly
     */
    private fun cleanAndFormatResponse(response: String): String {
        return response
            .trim()
            .replace(Regex("""<[^>]*>"""), "") // Remove HTML/XML tags
            .replace(Regex("""\b(BOS|EOS|PAD|UNK)\b"""), "") // Remove special tokens
            .replace(Regex("""\s+"""), " ") // Normalize whitespace
            .replace(Regex("""\n{3,}"""), "\n\n") // Limit multiple newlines
            .take(1000) // Limit response length
    }

    /**
     * Check if the service is ready for generation
     */
    suspend fun isServiceReady(): Boolean {
        return try {
            assetManager.checkAssetsExist() && executorRunner.isModelLoaded()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get model information
     */
    suspend fun getModelInfo(): ModelInfo? {
        return try {
            val config = executorRunner.getModelConfig()
            val modelSize = assetManager.getModelSize()

            ModelInfo(
                name = config?.modelName ?: "Unknown",
                size = modelSize,
                contextLength = config?.contextLength ?: 0,
                maxTokens = config?.maxTokens ?: 0,
                isLoaded = executorRunner.isModelLoaded()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Initialize the service (model loading)
     */
    suspend fun initializeService() {
        try {
            val assetsExist = assetManager.checkAssetsExist()
            if (!assetsExist) {
                throw LlmError.LocalLlmError.AssetCopyFailed("Model assets not found")
            }

            if (!executorRunner.isModelLoaded()) {
                executorRunner.initialize()
            }
        } catch (e: Exception) {
            when (e) {
                is LlmError -> throw e
                else -> throw LlmError.LocalLlmError.ModelLoadFailed(e)
            }
        }
    }

    /**
     * Cleanup resources
     */
    suspend fun cleanup() {
        try {
            executorRunner.cleanup()
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }
}

/**
 * Model information data class
 */
data class ModelInfo(
    val name: String,
    val size: Long,
    val contextLength: Int,
    val maxTokens: Int,
    val isLoaded: Boolean
) {
    val sizeInMB: Double get() = size / (1024.0 * 1024.0)
    val sizeFormatted: String get() = "%.1f MB".format(sizeInMB)
}
