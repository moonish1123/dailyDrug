package com.dailydrug.ocr.domain.model

sealed class OcrException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class TextExtractionFailed(cause: Throwable) : OcrException("텍스트 추출 실패", cause)
    class NoTextFound : OcrException("인식된 텍스트 없음")
    class NotDrugBag(text: String) : OcrException("약봉투가 아님: $text")
    class InvalidBitmapFormat : OcrException("잘못된 비트맵 형식")
    class MlKitNotInitialized : OcrException("ML Kit이 초기화되지 않음")
}