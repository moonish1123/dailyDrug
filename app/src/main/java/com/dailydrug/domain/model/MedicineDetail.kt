package com.dailydrug.domain.model

data class MedicineDetail(
    val medicine: Medicine,
    val schedules: List<MedicationSchedule>,
    val records: List<MedicationRecord>
) {
    val totalDoses: Int get() = records.size
    val takenCount: Int get() = records.count { it.status == MedicationStatus.TAKEN }
    val skippedCount: Int get() = records.count { it.status == MedicationStatus.SKIPPED }
    val adherenceRate: Double get() = if (totalDoses == 0) 0.0 else takenCount.toDouble() / totalDoses
}
