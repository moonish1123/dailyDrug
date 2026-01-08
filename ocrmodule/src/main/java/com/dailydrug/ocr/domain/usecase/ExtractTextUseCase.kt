package com.dailydrug.ocr.domain.usecase

import android.graphics.Bitmap
import com.dailydrug.ocr.domain.repository.OcrRepository
import javax.inject.Inject

/**
 * 이미지에서 텍스트를 추출하는 UseCase
 */
class ExtractTextUseCase @Inject constructor(
    private val repository: OcrRepository
) {
    /**
     * 이미지에서 텍스트를 추출합니다
     *
     * @param image 텍스트를 추출할 이미지
     * @return Result<String> 추출된 텍스트 또는 실패 시 OcrException
     */
    suspend operator fun invoke(image: Bitmap): Result<String> {
        return repository.extractText(image)
    }
}
