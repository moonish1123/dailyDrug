package com.dailydrug.ocr.data.datasource

import android.graphics.Bitmap
import com.dailydrug.ocr.domain.model.OcrLanguage

interface OcrDataSource {
    suspend fun extractText(image: Bitmap): String
    suspend fun extractText(image: Bitmap, language: OcrLanguage): String
}
