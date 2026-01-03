package com.dailydrug.presentation.llm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydrug.data.model.ConnectionTestResult
import com.dailydrug.data.model.LlmSettings
import com.dailydrug.data.model.LlmSettingsEvent
import com.dailydrug.data.model.LlmSettingsUiState
import com.dailydrug.data.repository.LlmSettingsRepository
import com.llmmodule.domain.model.LlmProvider
import com.llmmodule.domain.usecase.GenerateTextUseCase
import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.model.LlmResponse
import com.llmmodule.domain.model.LlmResult
import com.networkmodule.api.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * LLM 설정 관리 ViewModel
 */
@HiltViewModel
class LlmSettingsViewModel @Inject constructor(
    private val settingsRepository: LlmSettingsRepository,
    private val generateTextUseCase: GenerateTextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LlmSettingsUiState())
    val uiState: StateFlow<LlmSettingsUiState> = _uiState.asStateFlow()

    private var testConnectionJob: Job? = null

    init {
        loadSettings()
    }

    /**
     * 저장된 설정 로드
     */
    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                settingsRepository.getSettings().collectLatest { settings ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            settings = settings
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "설정을 불러오는 중 오류가 발생했습니다: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 이벤트 처리
     */
    fun onEvent(event: LlmSettingsEvent) {
        when (event) {
            is LlmSettingsEvent.ProviderSelected -> {
                selectProvider(event.provider)
            }
            is LlmSettingsEvent.ApiKeyUpdated -> {
                updateApiKey(event.provider, event.apiKey)
            }
            is LlmSettingsEvent.TestConnection -> {
                testConnection(event.provider)
            }
            is LlmSettingsEvent.ShowApiKeyDialog -> {
                showApiKeyDialog(event.provider)
            }
            is LlmSettingsEvent.DismissDialog -> {
                dismissDialog()
            }
            is LlmSettingsEvent.SaveSettings -> {
                saveSettings()
            }
        }
    }

    /**
     * 프로바이더 선택
     */
    private fun selectProvider(provider: LlmProvider) {
        viewModelScope.launch {
            try {
                settingsRepository.updateSelectedProvider(provider)

                // 온라인 프로바이더를 선택한 경우 선호하는 온라인 프로바이더로도 저장
                if (provider.isOnline) {
                    settingsRepository.updatePreferredOnlineProvider(provider)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "프로바이더 선택 실패: ${e.message}")
                }
            }
        }
    }

    /**
     * API 키 업데이트
     */
    private fun updateApiKey(provider: LlmProvider, apiKey: String) {
        viewModelScope.launch {
            try {
                settingsRepository.updateApiKey(provider, apiKey)
                dismissDialog()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "API 키 저장 실패: ${e.message}")
                }
            }
        }
    }

    /**
     * API 연결 테스트
     */
    private fun testConnection(provider: LlmProvider) {
        if (testConnectionJob?.isActive == true) return

        testConnectionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    testConnectionInProgress = true,
                    testConnectionResult = null,
                    error = null
                )
            }

            try {
                val startTime = System.currentTimeMillis()

                // API 키 가져오기
                val apiKey = settingsRepository.getApiKey(provider)
                if (apiKey.isEmpty()) {
                    throw Exception("API 키가 설정되지 않았습니다")
                }

                // 테스트용 요청
                val testPrompt = "테스트: 약물 복용에 대한 기본 정보를 알려주세요"
                val request = LlmRequest(
                    prompt = testPrompt,
                    maxOutputTokens = 10 // 테스트용으로 짧게
                )

                val responseTime = measureTimeMillis {
                    val result = generateTextUseCase.invoke(request)

                    when (result) {
                        is LlmResult.Success<LlmResponse> -> {
                            val response = result.data
                            if (response.text.isBlank()) {
                                throw Exception("응답이 비어있습니다")
                            }
                        }
                        is LlmResult.Error -> {
                            throw Exception(result.error.message)
                        }
                    }
                }

                val testResult = ConnectionTestResult(
                    provider = provider,
                    success = true,
                    message = "${provider.displayName} 연결 성공",
                    responseTime = responseTime
                )

                _uiState.update {
                    it.copy(
                        testConnectionInProgress = false,
                        testConnectionResult = testResult
                    )
                }

            } catch (e: Exception) {
                val testResult = ConnectionTestResult(
                    provider = provider,
                    success = false,
                    message = "연결 실패: ${e.message}",
                    responseTime = null
                )

                _uiState.update {
                    it.copy(
                        testConnectionInProgress = false,
                        testConnectionResult = testResult
                    )
                }
            }
        }
    }

    /**
     * API 키 설정 다이얼로그 표시
     */
    private fun showApiKeyDialog(provider: LlmProvider) {
        _uiState.update {
            it.copy(
                showApiKeyDialog = true,
                editingProvider = provider,
                error = null
            )
        }
    }

    /**
     * 다이얼로그 닫기
     */
    private fun dismissDialog() {
        _uiState.update {
            it.copy(
                showApiKeyDialog = false,
                editingProvider = null
            )
        }
    }

    /**
     * 설정 저장
     */
    private fun saveSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.updateSettings(_uiState.value.settings)
                _uiState.update {
                    it.copy(error = null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "설정 저장 실패: ${e.message}")
                }
            }
        }
    }

    /**
     * Local LLM 활성화 토글
     */
    fun toggleLocalLlmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateLocalLlmEnabled(enabled)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Local LLM 설정 변경 실패: ${e.message}")
                }
            }
        }
    }

    /**
     * 오프라인 자동 전환 토글
     */
    fun toggleAutoSwitchToOffline(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateAutoSwitchToOffline(enabled)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "자동 전환 설정 변경 실패: ${e.message}")
                }
            }
        }
    }

    /**
     * 오류 메시지 지우기
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 연결 테스트 결과 지우기
     */
    fun clearTestResult() {
        _uiState.update { it.copy(testConnectionResult = null) }
    }

    override fun onCleared() {
        super.onCleared()
        testConnectionJob?.cancel()
    }
}

/**
 * 시간 측정을 위한 간단 함수
 */
private suspend fun <T> measureTimeMillis(block: suspend () -> T): Long {
    val startTime = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - startTime
}