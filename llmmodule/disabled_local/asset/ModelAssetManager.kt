package com.llmmodule.data.asset

import android.content.Context
import com.llmmodule.data.local.ModelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelAssetManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val assetManager = context.assets

    companion object {
        private const val MODELS_DIR = "models"
        private const val MODEL_FILE = "llama-7b-4bit-q8.pte"
        private const val TOKENIZER_FILE = "tokenizer.model"
        private const val CONFIG_FILE = "model_config.json"
    }

    /**
     * Get the model file path in internal storage
     * Copies from assets if not exists
     */
    suspend fun getModelFile(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, MODEL_FILE)
            if (!modelFile.exists()) {
                copyAssetToInternalStorage(MODEL_FILE, modelFile)
            }
            Result.success(modelFile)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get model file: ${e.message}", e))
        }
    }

    /**
     * Get the tokenizer file path in internal storage
     * Copies from assets if not exists
     */
    suspend fun getTokenizerFile(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val tokenizerFile = File(context.filesDir, TOKENIZER_FILE)
            if (!tokenizerFile.exists()) {
                copyAssetToInternalStorage(TOKENIZER_FILE, tokenizerFile)
            }
            Result.success(tokenizerFile)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get tokenizer file: ${e.message}", e))
        }
    }

    /**
     * Get model configuration from assets
     */
    suspend fun getModelConfig(): Result<ModelConfig> = withContext(Dispatchers.IO) {
        try {
            val configJson = assetManager.open("$MODELS_DIR/$CONFIG_FILE")
                .bufferedReader()
                .use { it.readText() }

            // Simple JSON parsing (we could use a proper JSON library)
            val config = parseModelConfig(configJson)
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to load model config: ${e.message}", e))
        }
    }

    /**
     * Check if model assets exist in assets directory
     */
    suspend fun checkAssetsExist(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelExists = assetManager.list(MODELS_DIR)?.any {
                it == MODEL_FILE
            } == true

            val tokenizerExists = assetManager.list(MODELS_DIR)?.any {
                it == TOKENIZER_FILE
            } == true

            val configExists = assetManager.list(MODELS_DIR)?.any {
                it == CONFIG_FILE
            } == true

            modelExists && tokenizerExists && configExists
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get total size of model files in bytes
     */
    suspend fun getModelSize(): Long = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, MODEL_FILE)
            val tokenizerFile = File(context.filesDir, TOKENIZER_FILE)

            val size = (if (modelFile.exists()) modelFile.length() else 0) +
                       (if (tokenizerFile.exists()) tokenizerFile.length() else 0)

            size
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Delete model files from internal storage (for cleanup)
     */
    suspend fun cleanupModels(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, MODEL_FILE)
            val tokenizerFile = File(context.filesDir, TOKENIZER_FILE)

            if (modelFile.exists()) modelFile.delete()
            if (tokenizerFile.exists()) tokenizerFile.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to cleanup models: ${e.message}", e))
        }
    }

    /**
     * Copy asset file to internal storage
     */
    private suspend fun copyAssetToInternalStorage(assetPath: String, outFile: File) {
        withContext(Dispatchers.IO) {
            assetManager.open("$MODELS_DIR/$assetPath").use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    /**
     * Simple JSON parser for model config
     * In production, use proper JSON parsing library
     */
    private fun parseModelConfig(json: String): ModelConfig {
        // Simple key-value extraction (for demonstration)
        val contextLength = extractValue(json, "context_length")?.toIntOrNull() ?: 2048
        val maxTokens = extractValue(json, "max_tokens")?.toIntOrNull() ?: 512
        val temperature = extractValue(json, "temperature")?.toDoubleOrNull() ?: 0.7
        val topK = extractValue(json, "top_k")?.toIntOrNull() ?: 40
        val topP = extractValue(json, "top_p")?.toDoubleOrNull() ?: 0.9
        val repetitionPenalty = extractValue(json, "repetition_penalty")?.toDoubleOrNull() ?: 1.1
        val modelType = extractValue(json, "model_type") ?: "llama"
        val hiddenSize = extractValue(json, "hidden_size")?.toIntOrNull() ?: 4096
        val numLayers = extractValue(json, "num_layers")?.toIntOrNull() ?: 32
        val numHeads = extractValue(json, "num_heads")?.toIntOrNull() ?: 32

        return ModelConfig(
            contextLength = contextLength,
            maxTokens = maxTokens,
            temperature = temperature.toFloat(),
            topK = topK,
            topP = topP.toFloat(),
            repetitionPenalty = repetitionPenalty.toFloat(),
            modelType = modelType,
            hiddenSize = hiddenSize,
            numLayers = numLayers,
            numHeads = numHeads
        )
    }

    private fun extractValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1)
    }
}