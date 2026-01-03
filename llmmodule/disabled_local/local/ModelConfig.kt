package com.llmmodule.data.local

/**
 * Model configuration for local LLM
 * Contains model-specific parameters and metadata
 */
data class ModelConfig(
    val contextLength: Int = 2048,
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val repetitionPenalty: Float = 1.1f,
    val modelType: String = "llama",
    val hiddenSize: Int = 4096,
    val numLayers: Int = 32,
    val numHeads: Int = 32
)