package com.dailydrug.ocr.data.parser

import com.dailydrug.ocr.domain.model.DrugInfo
import com.dailydrug.ocr.domain.model.OcrException
import com.dailydrug.ocr.domain.model.ScheduleInfo
import java.time.LocalTime
import java.util.regex.Pattern
import javax.inject.Inject

class DrugInfoParser @Inject constructor() {

    fun parseDrugText(extractedText: String): DrugInfo {
        if (extractedText.isBlank()) {
            throw OcrException.NoTextFound()
        }

        // 1. 약 이름 추출
        val drugName = extractDrugName(extractedText)

        // 2. 복용량 추출
        val dosage = extractDosage(extractedText)

        // 3. 복용 스케줄 추출
        val scheduleInfo = extractScheduleInfo(extractedText)

        // 4. 설명 추출
        val description = extractDescription(extractedText)

        // 5. 제조사 추출
        val manufacturer = extractManufacturer(extractedText)

        return DrugInfo(
            drugName = drugName,
            dosage = dosage,
            scheduleInfo = scheduleInfo,
            description = description,
            manufacturer = manufacturer,
            rawText = extractedText
        )
    }

    private fun extractDrugName(text: String): String {
        // 약 이름 패턴: "OO정", "OO캡슐", "OO시럽", "OO액", "OO과립", "OO연고", "OO크림"
        val drugNamePattern = Pattern.compile("""([가-힣a-zA-Z0-9]+(?:정|캡슐|시럽|액|과립|연고|크림|주사|점안|점비|점이|냉각|스프레이|패치|로션))""")

        val matcher = drugNamePattern.matcher(text)
        while (matcher.find()) {
            val candidate = matcher.group(1)
            // 제조사나 약국 이름 필터링
            if (!isPharmacyName(candidate) && candidate.length >= 2) {
                return candidate.trim()
            }
        }

        // 일반적인 패턴으로 대체 시도
        val fallbackPattern = Pattern.compile("""([가-힣]{2,10}(?:약|제|정|캡슐|시럽|액))""")
        val fallbackMatcher = fallbackPattern.matcher(text)
        if (fallbackMatcher.find()) {
            return fallbackMatcher.group(1).trim()
        }

        return "알 수 없는 약물"
    }

    private fun extractDosage(text: String): String {
        // 복용량 패턴: "1정", "5ml", "2알", "1회 2정", "1일 3회 1정" 등
        val dosagePatterns = listOf(
            Pattern.compile("""(\d+(?:\.\d+)?)[정알mlmgg개](?:씩|마다)?"""),
            Pattern.compile("""(\d+)회\s*(\d+)[정알mlmgg개]"""),
            Pattern.compile("""1일\s*(\d+)회\s*(\d+)[정알mlmgg개]"""),
            Pattern.compile("""(\d+(?:\.\d+)?)\s*times?\s*(?:a\s*)?day"""),
            Pattern.compile("""(?:매\s*)?(\d+)\s*시간마다\s*(\d+)[정알mlmgg개]""")
        )

        for (pattern in dosagePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return when (matcher.groupCount()) {
                    2 -> "${matcher.group(2)}(${matcher.group(1)}회)"
                    1 -> matcher.group(1)
                    else -> "1정"
                }.trim()
            }
        }

        return "1정" // 기본값
    }

    private fun extractScheduleInfo(text: String): ScheduleInfo {
        val times = extractTimes(text)
        val pattern = extractPattern(text)
        val duration = extractDuration(text)
        val instructions = extractInstructions(text)

        return ScheduleInfo(times, pattern, duration, instructions)
    }

    private fun extractTimes(text: String): List<LocalTime> {
        val timePatterns = listOf(
            Pattern.compile("""(오전|오후|아침|점심|저녁|밤|새벽)?\s*(\d{1,2})\s*시\s*(\d{1,2})\s*분"""),
            Pattern.compile("""(\d{1,2}):(\d{1,2})"""),
            Pattern.compile("""(오전|오후)?\s*(\d{1,2})\s*시"""),
            Pattern.compile("""(아침|점심|저녁|식전|식후|취침전|기상후)""")
        )

        val times = mutableSetOf<LocalTime>()

        for (pattern in timePatterns) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                try {
                    val time = when {
                        matcher.groupCount() >= 3 && matcher.group(1) != null -> {
                            // 오전/오후 + 시 + 분
                            val period = matcher.group(1) ?: ""
                            val hour = matcher.group(2)?.toIntOrNull() ?: 0
                            val minute = matcher.group(3)?.toIntOrNull() ?: 0
                            adjustHourForPeriod(period, hour).let { LocalTime.of(it, minute) }
                        }
                        matcher.groupCount() >= 2 && matcher.group(1)?.contains(":") == true -> {
                            // HH:MM 형식
                            val parts = matcher.group(1)?.split(":") ?: emptyList()
                            if (parts.size >= 2) {
                                LocalTime.of(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
                            } else continue
                        }
                        matcher.groupCount() >= 2 -> {
                            // HH:MM 형식
                            LocalTime.of(
                                matcher.group(1)?.toIntOrNull() ?: 0,
                                matcher.group(2)?.toIntOrNull() ?: 0
                            )
                        }
                        matcher.groupCount() == 1 -> {
                            // 특정 시간대 키워드
                            when (matcher.group(1) ?: "") {
                                "아침", "기상후" -> LocalTime.of(8, 0)
                                "점심" -> LocalTime.of(12, 0)
                                "저녁" -> LocalTime.of(18, 0)
                                "밤", "취침전" -> LocalTime.of(22, 0)
                                "새벽" -> LocalTime.of(6, 0)
                                "식전" -> LocalTime.of(11, 30)
                                "식후" -> LocalTime.of(13, 0)
                                else -> continue
                            }
                        }
                        else -> continue
                    }
                    times.add(time)
                } catch (e: Exception) {
                    // 파싱 오류는 무시하고 다음 패턴으로
                }
            }
        }

        return times.sorted().take(5) // 최대 5개 시간까지
    }

    private fun extractPattern(text: String): String {
        val patternMap = mapOf(
            "매일" to "매일 복용",
            "격일" to "격일 복용",
            "주1회" to "주 1회 복용",
            "주2회" to "주 2회 복용",
            "주3회" to "주 3회 복용",
            "필요시" to "증상 발생 시 복용",
            "통증시" to "통증 발생 시 복용"
        )

        for ((keyword, pattern) in patternMap) {
            if (text.contains(keyword)) {
                return pattern
            }
        }

        // "5일 복용 1일 휴식" 패턴
        val cyclePattern = Pattern.compile("""(\d+)일\s*복용\s*(\d+)일\s*휴식""")
        val cycleMatcher = cyclePattern.matcher(text)
        if (cycleMatcher.find()) {
            return "${cycleMatcher.group(1)}일 복용 ${cycleMatcher.group(2)}일 휴식"
        }

        return "매일 복용" // 기본값
    }

    private fun extractDuration(text: String): String {
        val durationPatterns = listOf(
            Pattern.compile("""(\d+)[주일간]\s*복용"""),
            Pattern.compile("""(\d+)[주간]\s*투약"""),
            Pattern.compile("""(\d+)[일간]\s*복용"""),
            Pattern.compile("""(\d+)일\s*분량"""),
            Pattern.compile("""(\d+)일\s*처방""")
        )

        for (pattern in durationPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val days = matcher.group(1)
                return when {
                    days.toInt() >= 7 -> "${days.toInt() / 7}주간 복용"
                    else -> "${days}일간 복용"
                }
            }
        }

        return ""
    }

    private fun extractInstructions(text: String): String {
        val instructions = mutableListOf<String>()

        val instructionPatterns = mapOf(
            "식전" to "식전 30분 복용",
            "식후" to "식후 30분 복용",
            "공복" to "공복 복용",
            "취침전" to "취침 전 복용",
            "기상후" to "기상 후 즉시 복용",
            "충분한 물과 함께" to "충분한 물과 함께 복용",
            "물과 함께" to "물과 함께 복용"
        )

        for ((keyword, instruction) in instructionPatterns) {
            if (text.contains(keyword)) {
                instructions.add(instruction)
            }
        }

        return instructions.distinct().joinToString(", ")
    }

    private fun extractDescription(text: String): String {
        // 부작용, 효능, 주의사항 등 설명 관련 텍스트 추출
        val descriptionSections = mutableListOf<String>()

        val descriptionPatterns = listOf(
            Pattern.compile("""[효능효과][^:]*(?:[:]|\n)([^☎\n]*)"""),
            Pattern.compile("""[부작용][^:]*(?:[:]|\n)([^☎\n]*)"""),
            Pattern.compile("""[주의사항][^:]*(?:[:]|\n)([^☎\n]*)"""),
            Pattern.compile("""[용법용량][^:]*(?:[:]|\n)([^☎\n]*)""")
        )

        for (pattern in descriptionPatterns) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val section = matcher.group(1)?.trim()
                if (!section.isNullOrBlank() && section.length > 5) {
                    descriptionSections.add(section)
                }
            }
        }

        return descriptionSections.joinToString(" / ").take(200) // 최대 200자
    }

    private fun extractManufacturer(text: String): String {
        // 제조사/약국 이름 패턴
        val pharmacyPatterns = listOf(
            Pattern.compile("""([가-힣]+약국|[가-힣]+병원|[가-힣]+의원)"""),
            Pattern.compile("""([가-힣]+제약|[가-힣]+파마|[가-힣]+바이오)"""),
            Pattern.compile("""제조[가-힣]*\s*([가-힣]+)"""),
            Pattern.compile("""제조자[:\s]*([가-힣]+)""")
        )

        for (pattern in pharmacyPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)?.trim() ?: ""
            }
        }

        return ""
    }

    private fun adjustHourForPeriod(period: String, hour: Int): Int {
        return when (period) {
            "오전", "아침" -> if (hour == 12) 0 else hour
            "오후", "점심", "저녁" -> if (hour == 12) 12 else hour + 12
            "밤" -> if (hour < 12) hour + 12 else hour
            else -> hour
        }
    }

    private fun isPharmacyName(name: String): Boolean {
        val pharmacyKeywords = listOf("약국", "병원", "의원", "제약", "파마", "바이오")
        return pharmacyKeywords.any { keyword ->
            name.contains(keyword) || name.endsWith(keyword)
        }
    }

    fun isLikelyDrugBag(text: String): Boolean {
        val drugKeywords = listOf(
            "복용", "용법", "1일", "mg", "정", "캡슐", "시럽", "효능", "효과", "부작용",
            "투약", "용량", "식전", "식후", "주의", "보관", "유효기간", "성분", "제조"
        )

        val lowercaseText = text.lowercase()
        val foundKeywords = drugKeywords.count { keyword -> lowercaseText.contains(keyword) }

        // 3개 이상 약물 관련 키워드가 있으면 약봉투로 판단
        return foundKeywords >= 3
    }
}