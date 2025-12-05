package com.dailydrug.ocr.data.datasource

import android.graphics.Bitmap

interface OcrDataSource {
    suspend fun extractText(image: Bitmap): String
}
