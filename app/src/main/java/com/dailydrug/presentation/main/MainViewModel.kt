package com.dailydrug.presentation.main

import com.llmmodule.domain.model.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydrug.data.repository.LlmSettingsRepository
import com.dailydrug.domain.model.ScheduledDose
import com.dailydrug.domain.model.MedicationStatus
import com.dailydrug.domain.model.MedicationTimePeriod
import com.dailydrug.domain.usecase.GetTodayMedicationsUseCase
import com.dailydrug.domain.usecase.RecordMedicationUseCase
import com.dailydrug.domain.usecase.ScheduleNotificationUseCase
import com.dailydrug.domain.model.groupByTimePeriod

import com.llmmodule.domain.usecase.GenerateTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getTodayMedicationsUseCase: GetTodayMedicationsUseCase,
    private val recordMedicationUseCase: RecordMedicationUseCase,
    private val scheduleNotificationUseCase: ScheduleNotificationUseCase,
    private val createScheduleUseCase: com.dailydrug.domain.usecase.CreateScheduleUseCase,
    private val generateTextUseCase: GenerateTextUseCase,
    private val llmSettingsRepository: LlmSettingsRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    private val _events = MutableSharedFlow<MainUiEvent>()
    val events = _events.asSharedFlow()

    init {
        _uiState.update { it.copy(isLoading = true) }
        observeMedications()
    }

    private fun observeMedications() {
        viewModelScope.launch {
            selectedDate
                .flatMapLatest { date ->
                    getTodayMedicationsUseCase(date)
                        .map { doses -> date to doses }
                }
                .collect { (date, doses) ->
                    val medications = doses.map { it.toUi() }
                    _uiState.update { state ->
                        state.copy(
                            selectedDate = date,
                            todayMedications = medications,
                            medicationGroups = medications.groupByTimePeriodForUi(),
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun testLlm() {
        viewModelScope.launch {
            runCatching {
                // 1. API Key 확인
                val settings = llmSettingsRepository.getSettings().firstOrNull()
                    ?: throw IllegalStateException("LLM settings not available")

                val apiKey = when (settings.selectedProvider) {
                    is LlmProvider.ZAI -> settings.zaiApiKey
                    is LlmProvider.Claude -> settings.claudeApiKey
                    is LlmProvider.Gpt -> settings.gptApiKey
                    is LlmProvider.Local -> null
                    else -> null
                }

                if (apiKey.isNullOrBlank()) {
                    _events.emit(MainUiEvent.ShowMessage("API Key가 설정되지 않았습니다. 설정에서 API Key를 입력해주세요."))
                    return@launch
                }

                // 2. 고정된 질문 전송
                val request = LlmRequest(
                    prompt = "KF21 비행기에 대해 1000자 이내로 요약해죠 (성능, 공대공 전투능력, 스텔스 성능, 공대지 성능, 공대함 성능, 회전 반경, 근접 전투 능력 포함)",
                    model = settings.getModel(settings.selectedProvider),
                    systemInstructions = emptyList(),
                    temperature = 1.0,
                    maxOutputTokens = 2000
                )

                // 3. LLM 호출
                generateTextUseCase(request, settings.selectedProvider, apiKey).collect { result ->
                    when (result) {
                        is com.llmmodule.domain.model.LlmResult.Success -> {
                            _events.emit(MainUiEvent.ShowLlmResponse(result.data.text))
                        }
                        is com.llmmodule.domain.model.LlmResult.Error -> {
                            val errorMessage = when (result.error) {
                                is com.llmmodule.domain.model.LlmError.ApiKeyMissing -> "API Key가 필요합니다"
                                is com.llmmodule.domain.model.LlmError.Network -> "네트워크 오류"
                                else -> "LLM 오류: ${result.error.message}"
                            }
                            _events.emit(MainUiEvent.ShowMessage(errorMessage))
                        }
                    }
                }
            }.onFailure { e ->
                e.printStackTrace()
                _events.emit(MainUiEvent.ShowMessage("LLM 호출 실패: ${e.message}"))
            }
        }
    }

    fun onPreviousDay() {
        val date = _uiState.value.selectedDate.minusDays(1)
        selectedDate.value = date
        _uiState.update { it.copy(isLoading = true) }
    }

    fun onNextDay() {
        val date = _uiState.value.selectedDate.plusDays(1)
        selectedDate.value = date
        _uiState.update { it.copy(isLoading = true) }
    }

    fun onToday() {
        val today = LocalDate.now()
        if (_uiState.value.selectedDate == today) return
        selectedDate.value = today
        _uiState.update { it.copy(isLoading = true) }
    }

    fun onSelectDate(date: LocalDate) {
        selectedDate.value = date
        _uiState.update { it.copy(isLoading = true) }
    }

    fun onToggleTaken(recordId: Long, currentStatus: MedicationStatus) {
        val isCurrentlyTaken = currentStatus == MedicationStatus.TAKEN
        viewModelScope.launch {
            runCatching {
                recordMedicationUseCase(
                    RecordMedicationUseCase.Params(
                        recordId = recordId,
                        markAsTaken = !isCurrentlyTaken
                    )
                )
                if (isCurrentlyTaken) {
                    // 복용을 취소하면 지나간 시각일 경우 곧바로 알림이 울리지 않도록 1시간 후로 보정
                    scheduleNotificationUseCase(
                        recordId = recordId,
                        triggerAt = LocalDateTime.now().plusHours(1)
                    )
                }
                _events.emit(
                    MainUiEvent.ShowMessage(
                        if (isCurrentlyTaken) "복용 상태를 취소했어요." else "복용 완료 처리했어요."
                    )
                )
            }.onFailure {
                _events.emit(MainUiEvent.ShowMessage("복용 상태 변경에 실패했어요."))
            }
        }
    }

    fun onSkip(recordId: Long) {
        viewModelScope.launch {
            runCatching {
                recordMedicationUseCase(
                    RecordMedicationUseCase.Params(
                        recordId = recordId,
                        markAsTaken = false,
                        skip = true
                    )
                )
                _events.emit(MainUiEvent.ShowMessage("해당 복용을 건너뛰었습니다."))
            }.onFailure {
                _events.emit(MainUiEvent.ShowMessage("건너뛰기 처리에 실패했어요."))
            }
        }
    }

    private fun ScheduledDose.toUi(): TodayMedication = TodayMedication(
        recordId = recordId,
        scheduleId = scheduleId,
        medicineId = medicine.id,
        medicineName = medicine.name,
        dosage = medicine.dosage,
        scheduledTime = scheduledDateTime.toLocalTime(),
        status = status,
        takenTime = takenDateTime,
        color = medicine.color
    )
}

sealed interface MainUiEvent {
    data class ShowMessage(val message: String) : MainUiEvent
    data class ShowLlmResponse(val text: String) : MainUiEvent
}

/**
 * TodayMedication 리스트를 시간대별로 그룹화 (UI용)
 */
private fun List<TodayMedication>.groupByTimePeriodForUi(): List<MedicationTimeGroupUi> {
    return MedicationTimePeriod.sortedValues()
        .mapNotNull { period ->
            val periodMedications = filter { medication ->
                MedicationTimePeriod.fromTime(medication.scheduledTime) == period
            }
            if (periodMedications.isNotEmpty()) {
                MedicationTimeGroupUi(period, periodMedications.sortedBy { it.scheduledTime })
            } else {
                null
            }
        }
}
