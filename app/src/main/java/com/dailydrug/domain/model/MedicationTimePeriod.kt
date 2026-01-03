package com.dailydrug.domain.model

import java.time.LocalTime

/**
 * 약 복용 시간대 카테고리
 * 시간대별로 약을 그룹화하여 UI에 표시
 */
enum class MedicationTimePeriod(
    val displayName: String,
    val iconEmoji: String,
    val timeRange: String
) {
    MORNING(
        displayName = "오전약",
        iconEmoji = "🌅",
        timeRange = "06:00 ~ 10:59"
    ),
    LUNCH(
        displayName = "점심약",
        iconEmoji = "☀️",
        timeRange = "11:00 ~ 13:59"
    ),
    AFTERNOON(
        displayName = "오후약",
        iconEmoji = "🌤️",
        timeRange = "14:00 ~ 17:59"
    ),
    DINNER(
        displayName = "저녁약",
        iconEmoji = "🌆",
        timeRange = "18:00 ~ 20:59"
    ),
    NIGHT(
        displayName = "밤약",
        iconEmoji = "🌙",
        timeRange = "21:00 ~ 23:59"
    );

    companion object {
        /**
         * 시간에 해당하는 시간대 반환
         */
        fun fromTime(time: LocalTime): MedicationTimePeriod {
            val hour = time.hour
            return when (hour) {
                in 6..10 -> MORNING      // 06:00 ~ 10:59
                in 11..13 -> LUNCH        // 11:00 ~ 13:59
                in 14..17 -> AFTERNOON    // 14:00 ~ 17:59
                in 18..20 -> DINNER       // 18:00 ~ 20:59
                in 21..23 -> NIGHT        // 21:00 ~ 23:59
                else -> MORNING           // 0~5시는 새벽이라 오전으로 처리
            }
        }

        /**
         * 정렬 순서 (오전 -> 점심 -> 오후 -> 저녁 -> 밤)
         */
        fun sortedValues(): List<MedicationTimePeriod> {
            return values().toList()
        }
    }
}
