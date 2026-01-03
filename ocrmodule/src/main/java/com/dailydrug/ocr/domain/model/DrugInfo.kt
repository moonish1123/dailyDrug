package com.dailydrug.ocr.domain.model

import java.time.LocalDateTime
import java.time.LocalTime

data class DrugInfo(
    val drugName: String,                    // 약 이름
    val dosage: String,                      // 복용량 (예: "1정", "5ml")
    val scheduleInfo: ScheduleInfo,          // 복용 스케줄 정보
    val description: String = "",            // 약 설명
    val manufacturer: String = "",           // 제조사
    val rawText: String = "",                // 원본 인식 텍스트
    val extractedAt: LocalDateTime = LocalDateTime.now()
)

data class ScheduleInfo(
    val times: List<LocalTime>,              // 복용 시간들
    val pattern: String,                     // 복용 패턴 (예: "매일", "5일 복용 1일 휴식")
    val duration: String = "",               // 복용 기간
    val instructions: String = ""            // 복용 지침
)
