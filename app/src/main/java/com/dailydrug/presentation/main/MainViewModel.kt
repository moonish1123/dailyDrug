package com.dailydrug.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydrug.domain.model.ScheduledDose
import com.dailydrug.domain.model.MedicationStatus
import com.dailydrug.domain.model.MedicationTimePeriod
import com.dailydrug.domain.usecase.GetTodayMedicationsUseCase
import com.dailydrug.domain.usecase.RecordMedicationUseCase
import com.dailydrug.domain.usecase.ScheduleNotificationUseCase
import com.dailydrug.domain.model.groupByTimePeriod
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
    private val createScheduleUseCase: com.dailydrug.domain.usecase.CreateScheduleUseCase
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

    fun scheduleTestAlarm() {
        viewModelScope.launch {
            runCatching {
                val now = LocalDateTime.now()
                val testTime = now.plusMinutes(1).toLocalTime()
                val params = com.dailydrug.domain.model.CreateScheduleParams(
                    name = "테스트 알람",
                    dosage = "1정",
                    color = 0xFFE91E63.toInt(), // Pink color for test
                    memo = "1분 뒤 알람 테스트",
                    startDate = LocalDate.now(),
                    endDate = LocalDate.now(),
                    timeSlots = listOf(testTime),
                    takeDays = 1,
                    restDays = 0
                )
                createScheduleUseCase(params)
                _events.emit(MainUiEvent.ShowMessage("1분 뒤 울리는 테스트 알람을 설정했어요."))
                // 강제로 오늘 날짜 데이터 갱신을 위해 날짜 재선택 트리거 (필요하다면)
                onToday() 
            }.onFailure {
                _events.emit(MainUiEvent.ShowMessage("테스트 알람 설정에 실패했어요."))
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
