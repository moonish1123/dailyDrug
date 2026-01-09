package com.dailydrug.ocr.domain.model

/**
 * OCR 지원 언어
 */
enum class OcrLanguage(val displayName: String, val code: String) {
    KOREAN("한국어", "ko"),
    LATIN("영어", "en"),
    CHINESE("중국어", "zh"),
    JAPANESE("일본어", "ja");

    companion object {
        fun fromCode(code: String): OcrLanguage {
            return values().find { it.code == code } ?: KOREAN
        }
    }
}
