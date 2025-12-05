package com.dailydrug.ocr.domain.usecase

import android.graphics.Bitmap
import androidx.annotation.RequiresPermission
import com.dailydrug.ocr.domain.model.DrugInfo
import com.dailydrug.ocr.domain.repository.OcrRepository
import javax.inject.Inject

class AnalyzeDrugBagUseCase @Inject constructor(
    private val repository: OcrRepository
) {
    /**
     * Analyzes the provided image (expected to be from Camera) to extract drug information.
     * Requires Camera permission to be granted by the caller as the image source is the camera.
     */
    @RequiresPermission(android.Manifest.permission.CAMERA)
    suspend operator fun invoke(image: Bitmap): Result<DrugInfo> {
        return repository.analyzeDrugBag(image)
    }
}
