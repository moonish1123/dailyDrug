package com.dailydrug.ocr.data.repository

import android.graphics.Bitmap
import com.dailydrug.ocr.data.datasource.OcrDataSource
import com.dailydrug.ocr.domain.model.DrugInfo
import com.dailydrug.ocr.domain.repository.OcrRepository
import javax.inject.Inject

class OcrRepositoryImpl @Inject constructor(
    private val dataSource: OcrDataSource
) : OcrRepository {

    override suspend fun analyzeDrugBag(image: Bitmap): Result<DrugInfo> {
        return try {
            val text = dataSource.extractText(image)
            val info = parseText(text)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseText(text: String): DrugInfo {
        // Basic heuristic parsing logic
        var intakeTime = ""
        var cycle = ""
        var description = ""
        var drugName = ""

        val lines = text.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains("식후") || trimmed.contains("식전") || trimmed.contains("분")) {
                intakeTime = trimmed
            } else if (trimmed.contains("1일") || trimmed.contains("회") || trimmed.contains("일분")) {
                cycle = trimmed
            } else if (drugName.isEmpty() && trimmed.length > 2) {
                // First substantial line might be the name or pharmacy name
                // This is a placeholder logic
                drugName = trimmed
            }
        }
        
        return DrugInfo(
            drugName = drugName.ifEmpty { "Unknown" },
            intakeTime = intakeTime,
            cycle = cycle,
            description = description,
            rawText = text
        )
    }
}
