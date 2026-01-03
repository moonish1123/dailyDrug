package com.dailydrug.domain.model

/**
 * 약 복용 시간대별 그룹
 */
data class MedicationTimeGroup(
    val period: MedicationTimePeriod,
    val doses: List<ScheduledDose>
) {
    /**
     * 그룹 내 모든 약이 복용 완료되었는지 확인
     */
    val isAllTaken: Boolean
        get() = doses.all { it.status == MedicationStatus.TAKEN }

    /**
     * 그룹 내 미복용 약 개수
     */
    val pendingCount: Int
        get() = doses.count { it.status == MedicationStatus.PENDING }
}

/**
 * ScheduledDose 리스트를 시간대별로 그룹화
 */
fun List<ScheduledDose>.groupByTimePeriod(): List<MedicationTimeGroup> {
    return MedicationTimePeriod.sortedValues()
        .mapNotNull { period ->
            val periodDoses = filter { it.timePeriod == period }
            if (periodDoses.isNotEmpty()) {
                MedicationTimeGroup(period, periodDoses.sortedBy { it.scheduledDateTime })
            } else {
                null
            }
        }
}
