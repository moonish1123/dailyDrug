package com.dailydrug.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CalculateSchedulePatternsUseCaseTest {

    private val useCase = CalculateSchedulePatternsUseCase()

    @Test
    fun `shouldTakeMedicationOn returns true when restDays is zero`() {
        val start = LocalDate.of(2024, 1, 1)
        val target = LocalDate.of(2024, 1, 31)

        val shouldTake = useCase.shouldTakeMedicationOn(
            startDate = start,
            targetDate = target,
            takeDays = 1,
            restDays = 0
        )

        assertTrue(shouldTake)
    }

    @Test
    fun `shouldTakeMedicationOn respects take plus rest cycle`() {
        val start = LocalDate.of(2024, 1, 1)
        val takeDays = 5
        val restDays = 1

        // Day 5 (zero-indexed) -> still intake day.
        assertTrue(
            useCase.shouldTakeMedicationOn(
                startDate = start,
                targetDate = start.plusDays(4),
                takeDays = takeDays,
                restDays = restDays
            )
        )
        // Day 6 -> rest day.
        assertFalse(
            useCase.shouldTakeMedicationOn(
                startDate = start,
                targetDate = start.plusDays(5),
                takeDays = takeDays,
                restDays = restDays
            )
        )
        // Next cycle day 7 -> intake resumes.
        assertTrue(
            useCase.shouldTakeMedicationOn(
                startDate = start,
                targetDate = start.plusDays(6),
                takeDays = takeDays,
                restDays = restDays
            )
        )
    }

    @Test
    fun `generateActiveDates returns expected pattern`() {
        val start = LocalDate.of(2024, 4, 1)
        val end = LocalDate.of(2024, 4, 10)

        val dates = useCase.generateActiveDates(
            startDate = start,
            endDate = end,
            takeDays = 2,
            restDays = 1,
            maxOccurrences = 10
        )

        val expected = listOf(
            LocalDate.of(2024, 4, 1),
            LocalDate.of(2024, 4, 2),
            LocalDate.of(2024, 4, 4),
            LocalDate.of(2024, 4, 5),
            LocalDate.of(2024, 4, 7),
            LocalDate.of(2024, 4, 8),
            LocalDate.of(2024, 4, 10)
        )
        assertEquals(expected, dates)
    }

    @Test
    fun `calculateOccurrences generates sorted date times`() {
        val start = LocalDate.of(2024, 3, 1)
        val end = LocalDate.of(2024, 3, 3)
        val slots = listOf(LocalTime.of(8, 0), LocalTime.of(21, 30))

        val occurrences = useCase.calculateOccurrences(
            startDate = start,
            endDate = end,
            timeSlots = slots,
            takeDays = 1,
            restDays = 0,
            maxOccurrences = 10
        )

        assertEquals(6, occurrences.size)
        assertEquals("2024-03-01T08:00", occurrences.first().toString())
        assertEquals("2024-03-03T21:30", occurrences.last().toString())
        assertTrue(occurrences == occurrences.sorted())
    }
}
