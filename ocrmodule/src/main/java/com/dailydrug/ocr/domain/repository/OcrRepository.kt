package com.dailydrug.ocr.domain.repository

import android.graphics.Bitmap
import com.dailydrug.ocr.domain.model.DrugInfo

interface OcrRepository {
    suspend fun analyzeDrugBag(image: Bitmap): Result<DrugInfo>

    /**
     * 이미지에서 텍스트를 추출합니다 (raw text 반환)
     *
     * @param image 텍스트를 추출할 이미지
     * @return Result<String> 추출된 텍스트 또는 실패 시 OcrException
     */
    suspend fun extractText(image: Bitmap): Result<String>
}
