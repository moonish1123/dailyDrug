package com.dailydrug.ocr.data.datasource

import android.graphics.Bitmap
import com.dailydrug.ocr.domain.model.OcrLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OcrDataSourceImpl @Inject constructor() : OcrDataSource {
    // Initialize recognizers for each language
    private val koreanRecognizer = TextRecognition.getClient(
        KoreanTextRecognizerOptions.Builder().build()
    )
    // Latin recognizer - Korean model also handles Latin script well
    private val latinRecognizer = TextRecognition.getClient(
        KoreanTextRecognizerOptions.Builder().build()
    )
    private val chineseRecognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )
    private val japaneseRecognizer = TextRecognition.getClient(
        JapaneseTextRecognizerOptions.Builder().build()
    )

    override suspend fun extractText(image: Bitmap): String {
        return extractText(image, OcrLanguage.KOREAN)
    }

    override suspend fun extractText(image: Bitmap, language: OcrLanguage): String {
        val recognizer = getRecognizerForLanguage(language)
        val inputImage = InputImage.fromBitmap(image, 0)
        val result = recognizer.process(inputImage).await()
        return result.text
    }

    private fun getRecognizerForLanguage(language: OcrLanguage): TextRecognizer {
        return when (language) {
            OcrLanguage.KOREAN -> koreanRecognizer
            OcrLanguage.LATIN -> latinRecognizer
            OcrLanguage.CHINESE -> chineseRecognizer
            OcrLanguage.JAPANESE -> japaneseRecognizer
        }
    }
}
