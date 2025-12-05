package com.dailydrug.presentation.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydrug.domain.model.CreateScheduleParams
import com.dailydrug.domain.usecase.CalculateSchedulePatternsUseCase
import com.dailydrug.domain.usecase.CreateScheduleUseCase
import com.dailydrug.domain.usecase.DeleteScheduleUseCase
import com.dailydrug.domain.usecase.GetScheduleDetailUseCase
import com.dailydrug.presentation.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ScheduleInputViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createScheduleUseCase: CreateScheduleUseCase,
    private val calculateSchedulePatternsUseCase: CalculateSchedulePatternsUseCase,
    private val getScheduleDetailUseCase: GetScheduleDetailUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase
) : ViewModel() {

    private val scheduleIdArg: Long? = savedStateHandle.get<Long>(AppDestination.ScheduleInput.ARG_SCHEDULE_ID)?.takeIf { it > 0 }
    private val medicineIdArg: Long? = savedStateHandle.get<Long>(AppDestination.ScheduleInput.ARG_MEDICINE_ID)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(ScheduleInputUiState())
    val uiState: StateFlow<ScheduleInputUiState> = _uiState

    private val _events = MutableSharedFlow<ScheduleInputEvent>()
    val events = _events.asSharedFlow()

    init {
        if (scheduleIdArg != null) {
            loadSchedule(scheduleIdArg)
        } else if (medicineIdArg != null) {
            _uiState.update { it.copy(medicineId = medicineIdArg) }
        }
    }

    fun deleteSchedule() {
        val targetId = _uiState.value.scheduleId
        if (targetId == null) {
            viewModelScope.launch { _events.emit(ScheduleInputEvent.ShowMessage("삭제할 스케줄이 없습니다.")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            val result = runCatching { deleteScheduleUseCase(targetId) }
            _uiState.update { it.copy(isDeleting = false) }
            result.onSuccess {
                _events.emit(ScheduleInputEvent.ShowMessage("스케줄을 삭제했습니다."))
                _events.emit(ScheduleInputEvent.Deleted)
            }.onFailure {
                _events.emit(ScheduleInputEvent.ShowMessage("스케줄 삭제에 실패했습니다."))
            }
        }
    }

    private fun loadSchedule(scheduleId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getScheduleDetailUseCase(scheduleId) }
                .onSuccess { detail ->
                    if (detail == null) {
                        _events.emit(ScheduleInputEvent.ShowMessage("스케줄 정보를 불러올 수 없습니다."))
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        _uiState.update {
                            it.copy(
                                medicineId = detail.medicine.id,
                                scheduleId = detail.schedule.id,
                                medicineName = detail.medicine.name,
                                dosage = detail.medicine.dosage,
                                memo = detail.medicine.memo,
                                selectedColor = detail.medicine.color,
                                startDate = detail.schedule.startDate,
                                endDate = detail.schedule.endDate,
                                timeSlots = detail.schedule.timeSlots.sorted(),
                                takeDays = detail.schedule.takeDays,
                                restDays = detail.schedule.restDays,
                                isEditing = true,
                                isLoading = false
                            )
                        }
                        recalcPreview()
                    }
                }
                .onFailure {
                    _events.emit(ScheduleInputEvent.ShowMessage("스케줄 정보를 불러올 수 없습니다."))
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun updateMedicineName(value: String) {
        _uiState.update { it.copy(medicineName = value) }
    }

    fun updateDosage(value: String) {
        _uiState.update { it.copy(dosage = value) }
    }

    fun updateMemo(value: String) {
        _uiState.update { it.copy(memo = value) }
    }

    fun selectColor(color: Int) {
        _uiState.update { it.copy(selectedColor = color) }
    }

    fun updateStartDate(date: LocalDate) {
        _uiState.update { current ->
            val endDate = current.endDate?.takeIf { !it.isBefore(date) }
            current.copy(startDate = date, endDate = endDate)
        }
        recalcPreview()
    }

    fun updateEndDate(date: LocalDate?) {
        _uiState.update { it.copy(endDate = date) }
        recalcPreview()
    }

    fun addTimeSlot(time: LocalTime) {
        _uiState.update { state ->
            if (state.timeSlots.contains(time)) state else state.copy(timeSlots = (state.timeSlots + time).sorted())
        }
        recalcPreview()
    }

    fun removeTimeSlot(time: LocalTime) {
        _uiState.update { state -> state.copy(timeSlots = state.timeSlots.filterNot { it == time }) }
        recalcPreview()
    }

    fun updateTakeDays(days: Int) {
        val value = days.coerceAtLeast(1)
        _uiState.update { it.copy(takeDays = value) }
        recalcPreview()
    }

    fun updateRestDays(days: Int) {
        val value = days.coerceAtLeast(0)
        _uiState.update { it.copy(restDays = value) }
        recalcPreview()
    }

    fun saveSchedule() {
        val state = _uiState.value
        val validationError = validateState(state)
        if (validationError != null) {
            viewModelScope.launch { _events.emit(ScheduleInputEvent.ShowMessage(validationError)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                val params = CreateScheduleParams(
                    scheduleId = state.scheduleId,
                    medicineId = state.medicineId,
                    name = state.medicineName.trim(),
                    dosage = state.dosage.trim(),
                    color = state.selectedColor,
                    memo = state.memo.trim(),
                    startDate = state.startDate,
                    endDate = state.endDate,
                    timeSlots = state.timeSlots,
                    takeDays = state.takeDays,
                    restDays = state.restDays
                )
                createScheduleUseCase(params)
                _events.emit(ScheduleInputEvent.Success)
            }.onFailure {
                _events.emit(ScheduleInputEvent.ShowMessage("스케줄 저장에 실패했습니다."))
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun recalcPreview() {
        val state = _uiState.value
        if (state.isLoading) return
        if (state.timeSlots.isEmpty()) {
            _uiState.update { it.copy(preview = emptyList()) }
            return
        }
        val occurrences = calculateSchedulePatternsUseCase.calculateOccurrences(
            startDate = state.startDate,
            endDate = state.endDate,
            timeSlots = state.timeSlots,
            takeDays = state.takeDays,
            restDays = state.restDays,
            maxOccurrences = 6
        )
        _uiState.update { it.copy(preview = occurrences) }
    }

    private fun validateState(state: ScheduleInputUiState): String? {
        return when {
            state.medicineName.isBlank() -> "약 이름을 입력해주세요."
            state.dosage.isBlank() -> "복용량을 입력해주세요."
            state.timeSlots.isEmpty() -> "복용 시간을 하나 이상 추가해주세요."
            state.takeDays < 1 -> "복용 일수는 최소 1일 이상이어야 합니다."
            state.restDays < 0 -> "휴식 일수는 0 이상이어야 합니다."
            state.endDate != null && state.endDate.isBefore(state.startDate) -> "종료일은 시작일 이후여야 합니다."
            else -> null
        }
    }
}

sealed interface ScheduleInputEvent {
    data object Success : ScheduleInputEvent
    data object Deleted : ScheduleInputEvent
    data class ShowMessage(val message: String) : ScheduleInputEvent
}

data class ScheduleInputUiState(
    val medicineId: Long? = null,
    val scheduleId: Long? = null,
    val medicineName: String = "",
    val dosage: String = "",
    val memo: String = "",
    val selectedColor: Int = DEFAULT_COLORS.first(),
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val timeSlots: List<LocalTime> = emptyList(),
    val takeDays: Int = 1,
    val restDays: Int = 0,
    val preview: List<LocalDateTime> = emptyList(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isEditing: Boolean = false
) {
    companion object {
        val DEFAULT_COLORS = listOf(
            0xFF1A73E8.toInt(),
            0xFF0D47A1.toInt(),
            0xFF26A69A.toInt(),
            0xFFFF7043.toInt(),
            0xFF8E24AA.toInt(),
            0xFF5C6BC0.toInt()
        )
    }
}
