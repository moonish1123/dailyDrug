package com.dailydrug.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydrug.domain.model.MedicineDetail
import com.dailydrug.domain.model.MedicationRecord
import com.dailydrug.domain.model.MedicationStatus
import com.dailydrug.domain.usecase.ObserveMedicineDetailUseCase
import com.dailydrug.presentation.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MedicineDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeMedicineDetailUseCase: ObserveMedicineDetailUseCase
) : ViewModel() {

    private val medicineId: Long = savedStateHandle.get<Long>(AppDestination.MedicineDetail.ARG_MEDICINE_ID) ?: -1L

    private val _uiState = MutableStateFlow(MedicineDetailUiState())
    val uiState: StateFlow<MedicineDetailUiState> = _uiState

    init {
        if (medicineId <= 0) {
            _uiState.value = MedicineDetailUiState(errorMessage = "약 정보를 찾을 수 없습니다.")
        } else {
            observeDetail()
        }
    }

    private fun observeDetail() {
        viewModelScope.launch {
            observeMedicineDetailUseCase(medicineId).collectLatest { detail ->
                if (detail == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "약 정보를 불러올 수 없습니다.") }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            header = detail.toHeader(),
                            schedules = detail.schedules.map { schedule -> schedule.toUi() },
                            history = detail.records.sortedByDescending { record -> record.scheduledDateTime }
                                .take(30)
                                .map { record -> record.toHistoryItem() },
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    private fun MedicineDetail.toHeader(): MedicineDetailHeader {
        val adherencePercent = (adherenceRate * 100).toInt()
        val taken = records.count { it.status == MedicationStatus.TAKEN }
        val skipped = records.count { it.status == MedicationStatus.SKIPPED }
        return MedicineDetailHeader(
            name = medicine.name,
            dosage = medicine.dosage,
            color = medicine.color,
            memo = medicine.memo,
            adherencePercent = adherencePercent.coerceIn(0, 100),
            totalDoses = totalDoses,
            takenCount = taken,
            skippedCount = skipped,
            streak = records.calculateStreak()
        )
    }

    private fun MedicationRecord.toHistoryItem(): HistoryItem = HistoryItem(
        scheduledDate = scheduledDateTime.toLocalDate(),
        scheduledTime = scheduledDateTime.toLocalTime(),
        takenTime = takenDateTime,
        status = when (status) {
            MedicationStatus.TAKEN -> HistoryStatus.TAKEN
            MedicationStatus.SKIPPED -> HistoryStatus.SKIPPED
            MedicationStatus.PENDING -> HistoryStatus.MISSED
        }
    )

    private fun List<MedicationRecord>.calculateStreak(): Int {
        val sorted = this.sortedByDescending { it.scheduledDateTime }
        var streak = 0
        var lastDate: LocalDate? = null
        for (record in sorted) {
            if (record.status != MedicationStatus.TAKEN) {
                if (record.status == MedicationStatus.SKIPPED) continue else break
            }
            val date = record.scheduledDateTime.toLocalDate()
            if (lastDate == null) {
                streak++
                lastDate = date
                continue
            }
            if (lastDate == date) {
                continue
            }
            if (lastDate.minusDays(1) == date) {
                streak++
                lastDate = date
            } else {
                break
            }
        }
        return streak
    }
}

data class MedicineDetailUiState(
    val isLoading: Boolean = true,
    val header: MedicineDetailHeader? = null,
    val schedules: List<ScheduleUiModel> = emptyList(),
    val history: List<HistoryItem> = emptyList(),
    val errorMessage: String? = null
)

data class MedicineDetailHeader(
    val name: String,
    val dosage: String,
    val color: Int,
    val memo: String,
    val adherencePercent: Int,
    val totalDoses: Int,
    val takenCount: Int,
    val skippedCount: Int,
    val streak: Int
)

data class ScheduleUiModel(
    val id: Long,
    val period: String,
    val times: String,
    val isActive: Boolean
)

data class HistoryItem(
    val scheduledDate: LocalDate,
    val scheduledTime: java.time.LocalTime,
    val takenTime: java.time.LocalDateTime?,
    val status: HistoryStatus
)

enum class HistoryStatus { TAKEN, MISSED, SKIPPED }

private fun com.dailydrug.domain.model.MedicationSchedule.toUi(): ScheduleUiModel {
    val periodText = if (endDate != null) {
        "${startDate} ~ ${endDate}"
    } else {
        "${startDate} ~"
    }
    val timesText = timeSlots.joinToString { it.toString() }
    return ScheduleUiModel(
        id = id,
        period = periodText,
        times = timesText,
        isActive = isActive
    )
}
