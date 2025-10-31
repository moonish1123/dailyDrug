package com.dailydrug.presentation.schedule

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

class ScheduleInputScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scheduleInputContent_showsPreFilledState() {
        val state = ScheduleInputUiState(
            medicineName = "비타민C",
            dosage = "1정",
            memo = "식후",
            selectedColor = ScheduleInputUiState.DEFAULT_COLORS.first(),
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 10),
            timeSlots = listOf(LocalTime.of(9, 0)),
            takeDays = 5,
            restDays = 1,
            preview = listOf(LocalDateTime.of(2024, 1, 1, 9, 0))
        )

        composeRule.setContent {
            DailyDrugTheme {
                ScheduleInputContent(
                    uiState = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onNameChange = {},
                    onDosageChange = {},
                    onMemoChange = {},
                    onSelectColor = {},
                    onSelectStartDate = {},
                    onSelectEndDate = {},
                    onAddTime = {},
                    onRemoveTime = {},
                    onChangeTakeDays = {},
                    onChangeRestDays = {},
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithText("스케줄 등록").assertIsDisplayed()
        composeRule.onNodeWithText("비타민C").assertIsDisplayed()
        composeRule.onNodeWithText("09:00").assertIsDisplayed()
        composeRule.onNodeWithText("5일 복용 후 1일 휴식").assertIsDisplayed()
        composeRule.onNodeWithText("2024.01.01 09:00").assertIsDisplayed()
    }

    @Test
    fun scheduleInputContent_showsPlaceholdersWhenEmpty() {
        val state = ScheduleInputUiState(
            medicineName = "",
            dosage = "",
            memo = "",
            selectedColor = ScheduleInputUiState.DEFAULT_COLORS.first(),
            startDate = LocalDate.of(2024, 1, 1),
            endDate = null,
            timeSlots = emptyList(),
            takeDays = 1,
            restDays = 0,
            preview = emptyList()
        )

        composeRule.setContent {
            DailyDrugTheme {
                ScheduleInputContent(
                    uiState = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onNameChange = {},
                    onDosageChange = {},
                    onMemoChange = {},
                    onSelectColor = {},
                    onSelectStartDate = {},
                    onSelectEndDate = {},
                    onAddTime = {},
                    onRemoveTime = {},
                    onChangeTakeDays = {},
                    onChangeRestDays = {},
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithText("복용 시간을 추가해주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("복용 시간과 패턴을 입력하면 예시가 표시됩니다.").assertIsDisplayed()
    }
}
