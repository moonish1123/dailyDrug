package com.dailydrug.ocr.domain.repository

import android.graphics.Bitmap
import com.dailydrug.ocr.domain.model.OcrLanguage

interface OcrRepository {
    /**
     * 이미지에서 텍스트를 추출합니다 (raw text 반환)
     *
     * @param image 텍스트를 추출할 이미지
     * @return Result<String> 추출된 텍스트 또는 실패 시 OcrException
     */
    suspend fun extractText(image: Bitmap): Result<String>

    /**
     * 이미지에서 텍스트를 추출합니다 (언어 선택 가능)
     *
     * @param image 텍스트를 추출할 이미지
     * @param language OCR 언어 설정
     * @return Result<String> 추출된 텍스트 또는 실패 시 OcrException
     */
    suspend fun extractText(image: Bitmap, language: OcrLanguage): Result<String>
}
