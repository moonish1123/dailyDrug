package com.dailydrug.ocr.domain.repository

import android.graphics.Bitmap
import com.dailydrug.ocr.domain.model.DrugInfo

interface OcrRepository {
    suspend fun analyzeDrugBag(image: Bitmap): Result<DrugInfo>
}
