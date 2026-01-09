package com.dailydrug.ocr.data.repository

import android.graphics.Bitmap
import com.dailydrug.ocr.data.datasource.OcrDataSource
import com.dailydrug.ocr.domain.model.OcrException
import com.dailydrug.ocr.domain.model.OcrLanguage
import com.dailydrug.ocr.domain.repository.OcrRepository
import javax.inject.Inject

class OcrRepositoryImpl @Inject constructor(
    private val dataSource: OcrDataSource
) : OcrRepository {

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

    override suspend fun extractText(image: Bitmap, language: OcrLanguage): Result<String> {
        return try {
            val extractedText = dataSource.extractText(image, language)
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
