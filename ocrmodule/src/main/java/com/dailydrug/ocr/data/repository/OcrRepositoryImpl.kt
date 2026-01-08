package com.dailydrug.ocr.data.repository

import android.graphics.Bitmap
import com.dailydrug.ocr.data.datasource.OcrDataSource
import com.dailydrug.ocr.data.parser.DrugInfoParser
import com.dailydrug.ocr.domain.model.DrugInfo
import com.dailydrug.ocr.domain.model.OcrException
import com.dailydrug.ocr.domain.repository.OcrRepository
import javax.inject.Inject

class OcrRepositoryImpl @Inject constructor(
    private val dataSource: OcrDataSource,
    private val drugInfoParser: DrugInfoParser
) : OcrRepository {

    override suspend fun analyzeDrugBag(image: Bitmap): Result<DrugInfo> {
        return try {
            // 1. 텍스트 추출
            val extractedText = dataSource.extractText(image)
                .takeIf { it.isNotBlank() }
                    ?: throw OcrException.NoTextFound()

            // 2. 약봉투 여부 검증
            if (!drugInfoParser.isLikelyDrugBag(extractedText)) {
                throw OcrException.NotDrugBag(extractedText.take(100))
            }

            // 3. 약물 정보 파싱
            val drugInfo = drugInfoParser.parseDrugText(extractedText)
            Result.success(drugInfo)

        } catch (e: OcrException) {
            // 이미 정의된 OCR 예외는 그대로 전파
            Result.failure(e)
        } catch (e: Exception) {
            // 그 외 예외는 일반적인 텍스트 추출 실패로 처리
            when (e.message?.lowercase()) {
                "failed to process image" -> throw OcrException.InvalidBitmapFormat()
                "ml kit not initialized" -> throw OcrException.MlKitNotInitialized()
                else -> throw OcrException.TextExtractionFailed(e)
            }
        }
    }

    override suspend fun extractText(image: Bitmap): Result<String> {
        return try {
            val extractedText = dataSource.extractText(image)
            Result.success(extractedText)
        } catch (e: Exception) {
            when (e.message?.lowercase()) {
                "failed to process image" -> Result.failure(OcrException.InvalidBitmapFormat())
                "ml kit not initialized" -> Result.failure(OcrException.MlKitNotInitialized())
                else -> Result.failure(OcrException.TextExtractionFailed(e))
            }
        }
    }
}
