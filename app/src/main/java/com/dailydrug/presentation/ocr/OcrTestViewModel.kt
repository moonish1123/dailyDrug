package com.dailydrug.presentation.ocr

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydrug.ocr.domain.model.OcrLanguage
import com.dailydrug.ocr.domain.usecase.ExtractTextUseCase
import com.permissionmodule.domain.usecase.CheckPermissionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * OCR 테스트 ViewModel
 */
@HiltViewModel
class OcrTestViewModel @Inject constructor(
    private val extractTextUseCase: ExtractTextUseCase,
    val checkPermissionUseCase: CheckPermissionUseCase
) : ViewModel() {

    /**
     * Bitmap에서 텍스트 추출
     *
     * @param bitmap 텍스트를 추출할 이미지
     * @param language OCR 언어 설정
     * @param onResult 결과 콜백
     */
    fun extractTextFromBitmap(
        bitmap: Bitmap,
        language: OcrLanguage = OcrLanguage.KOREAN,
        onResult: (OcrTestUiState) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = extractTextUseCase(bitmap, language)
                result.fold(
                    onSuccess = { text ->
                        if (text.isBlank()) {
                            onResult(OcrTestUiState.Error("텍스트가 없습니다"))
                        } else {
                            onResult(OcrTestUiState.Success(text))
                        }
                    },
                    onFailure = { error ->
                        onResult(OcrTestUiState.Error("텍스트 추출 실패: ${error.message}"))
                    }
                )
            } catch (e: Exception) {
                onResult(OcrTestUiState.Error("OCR 오류: ${e.message}"))
            }
        }
    }
}
