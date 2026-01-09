package com.dailydrug.ocr.domain.usecase

import android.graphics.Bitmap
import com.dailydrug.ocr.domain.model.OcrLanguage
import com.dailydrug.ocr.domain.repository.OcrRepository
import javax.inject.Inject

/**
 * 이미지에서 텍스트를 추출하는 UseCase
 */
class ExtractTextUseCase @Inject constructor(
    private val repository: OcrRepository
) {
    /**
     * 이미지에서 텍스트를 추출합니다 (기본: 한국어)
     *
     * @param image 텍스트를 추출할 이미지
     * @return Result<String> 추출된 텍스트 또는 실패 시 OcrException
     */
    suspend operator fun invoke(image: Bitmap): Result<String> {
        return repository.extractText(image)
    }

    /**
     * 이미지에서 텍스트를 추출합니다 (언어 선택 가능)
     *
     * @param image 텍스트를 추출할 이미지
     * @param language OCR 언어 설정
     * @return Result<String> 추출된 텍스트 또는 실패 시 OcrException
     */
    suspend operator fun invoke(image: Bitmap, language: OcrLanguage): Result<String> {
        return repository.extractText(image, language)
    }
}
