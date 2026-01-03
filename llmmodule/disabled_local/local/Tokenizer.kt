package com.llmmodule.data.local

import java.io.File

/**
 * Mock tokenizer implementation for local LLM
 * In production, this would use a real tokenizer library
 */
class Tokenizer private constructor(
    private val vocab: Map<String, Int>,
    private val reverseVocab: Map<Int, String>
) {

    /**
     * Encode text to token IDs
     */
    fun encode(text: String): List<Long> {
        return text.split(" ")
            .map { word -> vocab.getOrDefault(word.lowercase(), 0).toLong() }
            .filter { it > 0L }
    }

    /**
     * Decode token IDs to text
     */
    fun decode(tokens: List<Long>): String {
        return tokens.map { token ->
            reverseVocab.getOrDefault(token.toInt(), "<unk>")
        }.joinToString(" ")
    }

    /**
     * Decode single token to text
     */
    fun decodeSingleToken(token: Long): String {
        return reverseVocab.getOrDefault(token.toInt(), "<unk>")
    }

    /**
     * Cleanup resources
     */
    fun destroy() {
        // No-op for mock implementation
    }

    /**
     * Builder for tokenizer
     */
    class Builder {
        private var vocab: Map<String, Int> = emptyMap()

        fun fromFile(file: File): Builder {
            // Mock implementation - in production would load real vocab
            vocab = mapOf(
                "<pad>" to 0,
                "<unk>" to 1,
                "<s>" to 2,
                "</s>" to 3,
                "the" to 4,
                "a" to 5,
                "an" to 6,
                "and" to 7,
                "is" to 8,
                "are" to 9,
                "was" to 10,
                "were" to 11
            )
            return this
        }

        fun build(): Tokenizer {
            val reverseVocab = vocab.entries.associate { (word, id) -> id to word }
            return Tokenizer(vocab, reverseVocab)
        }
    }
}