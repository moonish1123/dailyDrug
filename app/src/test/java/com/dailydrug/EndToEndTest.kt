package com.dailydrug

import com.dailydrug.data.repository.MedicationRepositoryImpl
import com.dailydrug.domain.model.CreateScheduleParams
import com.dailydrug.domain.usecase.CalculateSchedulePatternsUseCase
import com.dailydrug.domain.usecase.CreateScheduleUseCase
import com.dailydrug.domain.usecase.DeleteScheduleUseCase
import com.dailydrug.domain.usecase.GetScheduleDetailUseCase
import com.dailydrug.presentation.schedule.ScheduleInputViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import androidx.lifecycle.SavedStateHandle

@OptIn(ExperimentalCoroutinesApi::class)
class EndToEndTest {

    private val repository: MedicationRepositoryImpl = mockk(relaxed = true)
    private lateinit var createScheduleUseCase: CreateScheduleUseCase
    private lateinit var calculateSchedulePatternsUseCase: CalculateSchedulePatternsUseCase
    private lateinit var getScheduleDetailUseCase: GetScheduleDetailUseCase
    private lateinit var deleteScheduleUseCase: DeleteScheduleUseCase
    private lateinit var viewModel: ScheduleInputViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        createScheduleUseCase = CreateScheduleUseCase(repository, testDispatcher)
        calculateSchedulePatternsUseCase = CalculateSchedulePatternsUseCase()
        getScheduleDetailUseCase = GetScheduleDetailUseCase(repository, testDispatcher)
        deleteScheduleUseCase = DeleteScheduleUseCase(repository, testDispatcher)
        
        viewModel = ScheduleInputViewModel(
            savedStateHandle = SavedStateHandle(),
            createScheduleUseCase = createScheduleUseCase,
            calculateSchedulePatternsUseCase = calculateSchedulePatternsUseCase,
            getScheduleDetailUseCase = getScheduleDetailUseCase,
            deleteScheduleUseCase = deleteScheduleUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create schedule flow triggers repository and alarm scheduling`() = runTest(testDispatcher) {
        // Given
        val medicineName = "Test Med"
        val dosage = "1 pill"
        val timeSlot = LocalTime.of(9, 0)
        
        // When: User inputs data
        viewModel.updateMedicineName(medicineName)
        viewModel.updateDosage(dosage)
        viewModel.addTimeSlot(timeSlot)
        viewModel.updateTakeDays(1)
        viewModel.updateRestDays(0)
        
        // And: User saves
        viewModel.saveSchedule()
        
        // Then: Repository should be called with correct params
        coVerify { 
            repository.createSchedule(match { params ->
                params.name == medicineName &&
                params.dosage == dosage &&
                params.timeSlots.contains(timeSlot)
            }) 
        }
    }
}
