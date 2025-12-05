package com.dailydrug.domain.model

/**
 * Combination of a medicine and one of its schedules used for edit flows.
 */
data class ScheduleDetail(
    val medicine: Medicine,
    val schedule: MedicationSchedule
)
