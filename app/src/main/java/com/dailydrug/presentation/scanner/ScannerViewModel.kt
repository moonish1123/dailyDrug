package com.dailydrug.presentation.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydrug.data.model.LlmSettings
import com.dailydrug.data.repository.LlmSettingsRepository
import com.dailydrug.ocr.domain.model.OcrLanguage
import com.dailydrug.ocr.domain.usecase.ExtractTextUseCase
import com.llmmodule.domain.model.LlmProvider
import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.model.LlmResult
import com.llmmodule.domain.usecase.GenerateTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing prescription scanning with OCR → LLM pipeline.
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val extractTextUseCase: ExtractTextUseCase,
    private val generateTextUseCase: GenerateTextUseCase,
    private val llmSettingsRepository: LlmSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /**
     * Process a prescription image through OCR → LLM pipeline.
     *
     * @param bitmap The image to process
     * @param language The OCR language to use (default: Korean)
     */
    fun processPrescriptionImage(
        bitmap: Bitmap,
        language: OcrLanguage = OcrLanguage.KOREAN
    ) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.OcrProcessing

            // Step 1: OCR Extraction
            val ocrResult = extractTextUseCase(bitmap, language)

            ocrResult.fold(
                onSuccess = { ocrText ->
                    if (ocrText.isBlank()) {
                        _uiState.value = ScannerUiState.Error("텍스트를 찾을 수 없습니다")
                    } else {
                        // Step 2: LLM Processing
                        processWithLlm(ocrText)
                    }
                },
                onFailure = { error ->
                    _uiState.value = ScannerUiState.Error("OCR 실패: ${error.message}")
                }
            )
        }
    }

    /**
     * Process OCR text with LLM to extract medication schedule.
     */
    private suspend fun processWithLlm(ocrText: String) {
        _uiState.value = ScannerUiState.LlmProcessing

        try {
            val settings = llmSettingsRepository.getSettings().first()
            val provider = settings.selectedProvider
            val apiKey = settings.getCurrentApiKey()

            if (provider is LlmProvider.Local) {
                _uiState.value = ScannerUiState.Error("Local LLM은 처방전 분석을 지원하지 않습니다")
                return
            }

            if (apiKey.isNullOrBlank()) {
                _uiState.value = ScannerUiState.Error("LLM API 키가 설정되지 않았습니다. 설정에서 API 키를 입력해주세요.")
                return
            }

            val request = LlmRequest(
                prompt = buildMedicationAnalysisPrompt(ocrText),
                model = settings.getModel(provider),
                systemInstructions = listOf(
                    "당신은 의약품 정보를 분석하여 복용 스케줄을 만드는 전문가입니다.",
                    "추출한 정보를 명확하고 간결하게 정리해주세요."
                ),
                temperature = 0.3,
                maxOutputTokens = 2000
            )

            val result = generateTextUseCase(request, provider, apiKey).first()

            when (result) {
                is LlmResult.Success -> {
                    _uiState.value = ScannerUiState.Success(result.data.text)
                }
                is LlmResult.Error -> {
                    _uiState.value = ScannerUiState.Error("LLM 처리 실패: ${result.error.message}")
                }
            }
        } catch (e: Exception) {
            _uiState.value = ScannerUiState.Error("처리 중 오류: ${e.message}")
        }
    }

    /**
     * Build medication analysis prompt for LLM.
     */
    private fun buildMedicationAnalysisPrompt(ocrText: String): String {
        return """
            [context]
            다음은 처방전/약 봉투에서 OCR로 추출한 텍스트입니다:

            $ocrText

            [ask]
            이 context를 정리해서 내가 먹어야 할 약에 대한 스케줄을 정리해주세요.

            다음 형식으로 정리해주세요:
            - 약품명
            - 복용량
            - 1일 복용 횟수
            - 복용 기간
            - 복용 시간
            - 주의사항

            가능하면 간단하고 명확하게 정리해주세요.
        """.trimIndent()
    }

    /**
     * Reset the scanner state to idle.
     */
    fun reset() {
        _uiState.value = ScannerUiState.Idle
    }
}

/**
 * Scanner UI state representing the OCR → LLM pipeline stages.
 */
sealed class ScannerUiState {
    /** Initial state, no processing in progress */
    data object Idle : ScannerUiState()

    /** OCR text extraction in progress */
    data object OcrProcessing : ScannerUiState()

    /** LLM medication analysis in progress */
    data object LlmProcessing : ScannerUiState()

    /** Successfully processed, contains medication schedule */
    data class Success(val medicationSchedule: String) : ScannerUiState()

    /** Error occurred during processing */
    data class Error(val message: String) : ScannerUiState()
}
