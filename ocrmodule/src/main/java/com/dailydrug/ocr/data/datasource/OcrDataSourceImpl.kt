package com.dailydrug.ocr.data.datasource

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OcrDataSourceImpl @Inject constructor() : OcrDataSource {
    // Initialize the recognizer with Korean options
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    override suspend fun extractText(image: Bitmap): String {
        val inputImage = InputImage.fromBitmap(image, 0) // Assuming 0 rotation for now, or passed in
        val result = recognizer.process(inputImage).await()
        return result.text
    }
}
