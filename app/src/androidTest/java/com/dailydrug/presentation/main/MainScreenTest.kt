package com.dailydrug.presentation.main

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dailydrug.domain.model.MedicationStatus
import com.dailydrug.presentation.theme.DailyDrugTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_showsPlaceholderWhenNoMedications() {
        composeTestRule.setContent {
            DailyDrugTheme {
                MainScreen(
                    state = MainUiState(
                        selectedDate = LocalDate.of(2024, 1, 1),
                        todayMedications = emptyList(),
                        isLoading = false
                    ),
                    snackbarHostState = SnackbarHostState(),
                    onAddSchedule = {},
                    onOpenSettings = {},
                    onPreviousDay = {},
                    onNextDay = {},
                    onToggleTaken = { _, _ -> },
                    onSkipDose = {},
                    onOpenMedicineDetail = {}
                )
            }
        }

        composeTestRule.onNodeWithText("오늘 복용할 약이 없습니다.").assertIsDisplayed()
        composeTestRule.onNodeWithText("오른쪽 아래 + 버튼을 눌러 스케줄을 등록하세요.").assertIsDisplayed()
    }

    @Test
    fun mainScreen_togglesMedicationStatusWhenButtonClicked() {
        val recordId = 24L
        var toggledRecord: Pair<Long, MedicationStatus>? = null

        val medication = TodayMedication(
            recordId = recordId,
            scheduleId = 10L,
            medicineId = 3L,
            medicineName = "아스피린",
            dosage = "1정",
            scheduledTime = LocalTime.of(9, 0),
            status = MedicationStatus.PENDING,
            takenTime = LocalDateTime.of(2024, 1, 1, 9, 30),
            color = 0xFF1A73E8.toInt()
        )

        composeTestRule.setContent {
            DailyDrugTheme {
                MainScreen(
                    state = MainUiState(
                        selectedDate = LocalDate.of(2024, 1, 1),
                        todayMedications = listOf(medication),
                        isLoading = false
                    ),
                    snackbarHostState = SnackbarHostState(),
                    onAddSchedule = {},
                    onOpenSettings = {},
                    onPreviousDay = {},
                    onNextDay = {},
                    onToggleTaken = { id, status -> toggledRecord = id to status },
                    onSkipDose = {},
                    onOpenMedicineDetail = {}
                )
            }
        }

        composeTestRule.onNodeWithText("복용").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(toggledRecord)
        assertEquals(recordId, toggledRecord?.first)
        assertEquals(MedicationStatus.PENDING, toggledRecord?.second)
    }
}
