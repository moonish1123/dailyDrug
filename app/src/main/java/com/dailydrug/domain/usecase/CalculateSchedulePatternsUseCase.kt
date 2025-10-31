package com.dailydrug.domain.usecase

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class CalculateSchedulePatternsUseCase {

    fun shouldTakeMedicationOn(
        startDate: LocalDate,
        targetDate: LocalDate,
        takeDays: Int,
        restDays: Int
    ): Boolean {
        if (targetDate.isBefore(startDate)) return false
        require(takeDays > 0) { "takeDays must be > 0" }
        require(restDays >= 0) { "restDays must be >= 0" }

        if (restDays == 0) return true

        val daysSinceStart = ChronoUnit.DAYS.between(startDate, targetDate)
        val cycleLength = takeDays + restDays
        val dayInCycle = (daysSinceStart % cycleLength).toInt()
        return dayInCycle < takeDays
    }

    fun generateActiveDates(
        startDate: LocalDate,
        endDate: LocalDate?,
        takeDays: Int,
        restDays: Int,
        maxOccurrences: Int = 365
    ): List<LocalDate> {
        require(maxOccurrences > 0) { "maxOccurrences must be positive." }
        val result = mutableListOf<LocalDate>()
        var current = startDate
        var occurrences = 0

        while (true) {
            if (endDate != null && current.isAfter(endDate)) break
            if (occurrences >= maxOccurrences) break

            if (shouldTakeMedicationOn(startDate, current, takeDays, restDays)) {
                result += current
                occurrences += 1
            }

            current = current.plusDays(1)
            if (endDate == null && occurrences >= maxOccurrences) {
                break
            }
        }

        return result
    }

    fun calculateOccurrences(
        startDate: LocalDate,
        endDate: LocalDate?,
        timeSlots: List<LocalTime>,
        takeDays: Int,
        restDays: Int,
        maxOccurrences: Int = 1000
    ): List<LocalDateTime> {
        require(timeSlots.isNotEmpty()) { "timeSlots must not be empty." }
        val normalizedSlots = timeSlots.distinct().sorted()
        val activeDates = generateActiveDates(
            startDate = startDate,
            endDate = endDate,
            takeDays = takeDays,
            restDays = restDays,
            maxOccurrences = maxOccurrences
        )

        val occurrences = mutableListOf<LocalDateTime>()
        for (date in activeDates) {
            for (slot in normalizedSlots) {
                occurrences += LocalDateTime.of(date, slot)
                if (occurrences.size >= maxOccurrences) {
                    return occurrences
                }
            }
        }

        return occurrences
    }
}
