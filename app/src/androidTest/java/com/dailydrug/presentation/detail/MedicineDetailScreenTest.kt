package com.dailydrug.presentation.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.dailydrug.presentation.theme.DailyDrugTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test

class MedicineDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun medicineDetailContent_showsHeaderAndHistory() {
        val header = MedicineDetailHeader(
            name = "아스피린",
            dosage = "1정",
            color = 0xFF1A73E8.toInt(),
            memo = "식후 복용",
            adherencePercent = 80,
            totalDoses = 10,
            takenCount = 8,
            skippedCount = 1,
            streak = 3
        )
        val schedules = listOf(
            ScheduleUiModel(
                id = 1L,
                period = "2024-01-01 ~ 2024-02-01",
                times = "09:00",
                isActive = true
            )
        )
        val history = listOf(
            HistoryItem(
                scheduledDate = LocalDate.of(2024, 1, 1),
                scheduledTime = LocalTime.of(9, 0),
                takenTime = LocalDateTime.of(2024, 1, 1, 9, 5),
                status = HistoryStatus.TAKEN
            ),
            HistoryItem(
                scheduledDate = LocalDate.of(2024, 1, 2),
                scheduledTime = LocalTime.of(9, 0),
                takenTime = null,
                status = HistoryStatus.MISSED
            )
        )

        val state = MedicineDetailUiState(
            isLoading = false,
            header = header,
            schedules = schedules,
            history = history,
            errorMessage = null
        )

        composeRule.setContent {
            DailyDrugTheme {
                MedicineDetailContent(
                    uiState = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onEditSchedule = {},
                    onDeleteSchedule = {}
                )
            }
        }

        composeRule.onNodeWithText("약 상세").assertIsDisplayed()
        composeRule.onNodeWithText("아스피린").assertIsDisplayed()
        composeRule.onNodeWithText("복용률 80%").assertIsDisplayed()
        composeRule.onNodeWithText("스케줄").assertIsDisplayed()
        composeRule.onNodeWithText("복용 이력").assertIsDisplayed()
        composeRule.onNodeWithText("복용 완료").assertIsDisplayed()
        composeRule.onNodeWithText("미복용").assertIsDisplayed()
    }

    @Test
    fun medicineDetailContent_showsHistoryPlaceholderWhenEmpty() {
        val state = MedicineDetailUiState(
            isLoading = false,
            header = null,
            schedules = emptyList(),
            history = emptyList(),
            errorMessage = null
        )

        composeRule.setContent {
            DailyDrugTheme {
                MedicineDetailContent(
                    uiState = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onEditSchedule = {},
                    onDeleteSchedule = {}
                )
            }
        }

        composeRule.onNodeWithText("복용 이력").assertIsDisplayed()
        composeRule.onNodeWithText("복용 이력이 없습니다.").assertIsDisplayed()
    }
}
