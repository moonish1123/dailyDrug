package com.dailydrug.ocr.data.parser

import com.dailydrug.ocr.domain.model.OcrException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalTime

class DrugInfoParserTest {

    private lateinit var parser: DrugInfoParser

    @BeforeEach
    fun setUp() {
        parser = DrugInfoParser()
    }

    @Test
    fun `약물 이름 정확히 추출`() {
        val text = """
            타이레놀정 500mg
            1일 3회, 1회 1정씩 복용합니다
            식후 30분에 복용해주세요
        """.trimIndent()

        val drugInfo = parser.parseDrugText(text)

        assertEquals("타이레놀정", drugInfo.drugName)
    }

    @Test
    fun `다양한 약물 이름 패턴 추출`() {
        val testCases = mapOf(
            "콜대프정 100mg" to "콜대프정",
            "게보린주사" to "게보린주사",
            "알레기정 10mg" to "알레기정",
            "부스코판캡슐" to "부스코판캡슐",
            "메디퀵시럽" to "메디퀵시럽"
        )

        testCases.forEach { (input, expected) ->
            val drugInfo = parser.parseDrugText(input)
            assertEquals(expected, drugInfo.drugName, "Failed for: $input")
        }
    }

    @Test
    fun `복용량 패턴 추출`() {
        val testCases = mapOf(
            "1일 3회 1정씩 복용" to "1정(3회)",
            "1회 2정씩 1일 2회" to "2정(2회)",
            "5ml 1일 3회" to "5ml(3회)",
            "매일 1알씩" to "1알",
            "2시간마다 1정" to "1정"
        )

        testCases.forEach { (input, expected) ->
            val drugInfo = parser.parseDrugText(input)
            assertEquals(expected, drugInfo.dosage, "Failed for: $input")
        }
    }

    @Test
    fun `복용 시간 패턴 추출`() {
        val text = """
            오전 8시, 오후 2시, 저녁 8시에 복용합니다
            아침 7시 30분, 점심 12시 30분, 저녁 7시 30분
        """.trimIndent()

        val drugInfo = parser.parseDrugText(text)

        assertEquals(6, drugInfo.scheduleInfo.times.size)
        assertTrue(drugInfo.scheduleInfo.times.contains(LocalTime.of(8, 0)))
        assertTrue(drugInfo.scheduleInfo.times.contains(LocalTime.of(14, 0)))
        assertTrue(drugInfo.scheduleInfo.times.contains(LocalTime.of(20, 0)))
        assertTrue(drugInfo.scheduleInfo.times.contains(LocalTime.of(7, 30)))
    }

    @Test
    fun `시간대 키워드 추출`() {
        val text = """
            아침, 점심, 저녁으로 나누어 복용
            식전 30분에 드세요
            취침전에 복용
        """.trimIndent()

        val drugInfo = parser.parseDrugText(text)

        assertTrue(drugInfo.scheduleInfo.times.contains(LocalTime.of(8, 0))) // 아침
        assertTrue(drugInfo.scheduleInfo.times.contains(LocalTime.of(12, 0))) // 점심
        assertTrue(drugInfo.scheduleInfo.times.contains(LocalTime.of(18, 0))) // 저녁
        assertTrue(drugInfo.scheduleInfo.times.contains(LocalTime.of(22, 0))) // 취침전
    }

    @Test
    fun `복용 패턴 추출`() {
        val testCases = mapOf(
            "매일 3회 복용" to "매일 복용",
            "5일 복용 1일 휴식" to "5일 복용 1일 휴식",
            "격일로 복용" to "격일 복용",
            "주1회 복용" to "주 1회 복용",
            "통증 발생 시" to "통증 발생 시 복용"
        )

        testCases.forEach { (input, expected) ->
            val drugInfo = parser.parseDrugText(input)
            assertEquals(expected, drugInfo.scheduleInfo.pattern, "Failed for: $input")
        }
    }

    @Test
    fun `복용 기간 추출`() {
        val testCases = mapOf(
            "7일간 복용" to "7일간 복용",
            "14일분량 처방" to "2주간 복용",
            "3주간 투약" to "3주간 복용",
            "21일간 복용" to "3주간 복용"
        )

        testCases.forEach { (input, expected) ->
            val drugInfo = parser.parseDrugText(input)
            assertEquals(expected, drugInfo.scheduleInfo.duration, "Failed for: $input")
        }
    }

    @Test
    fun `복용 지침 추출`() {
        val text = """
            식전 30분 복용
            충분한 물과 함께 드세요
            취침전에 복용
        """.trimIndent()

        val drugInfo = parser.parseDrugText(text)

        assertTrue(drugInfo.scheduleInfo.instructions.contains("식전 30분 복용"))
        assertTrue(drugInfo.scheduleInfo.instructions.contains("충분한 물과 함께 복용"))
        assertTrue(drugInfo.scheduleInfo.instructions.contains("취침 전 복용"))
    }

    @Test
    fun `설명 텍스트 추출`() {
        val text = """
            타이레놀정

            효능효과: 감기로 인한 두통, 발열, 근육통

            부작용: 드물게 피부발진, 소화불량

            주의사항: 간기능 장애 환자는 주의해야 합니다

            보관법: 직사광선을 피해 실온 보관
        """.trimIndent()

        val drugInfo = parser.parseDrugText(text)

        assertTrue(drugInfo.description.contains("감기로 인한 두통, 발열, 근육통"))
        assertTrue(drugInfo.description.contains("피부발진, 소화불량"))
        assertTrue(drugInfo.description.contains("간기능 장애 환자는 주의"))
    }

    @Test
    fun `제조사 추출`() {
        val testCases = mapOf(
            "제조: 일동제약" to "일동제약",
            "종근당약국" to "종근당약국",
            "대웅제약" to "대웅제약",
            "제조자: 유한양행" to "유한양행"
        )

        testCases.forEach { (input, expected) ->
            val drugInfo = parser.parseDrugText(input)
            assertEquals(expected, drugInfo.manufacturer, "Failed for: $input")
        }
    }

    @Test
    fun `빈 텍스트 처리`() {
        assertThrows(OcrException.NoTextFound::class.java) {
            parser.parseDrugText("")
        }

        assertThrows(OcrException.NoTextFound::class.java) {
            parser.parseDrugText("   ")
        }
    }

    @Test
    fun `약봉투 여부 검증`() {
        val validDrugText = """
            타이레놀정 500mg
            1일 3회, 1회 1정씩 복용
            식후 30분에 복용
            효능: 두통, 발열
            부작용: 드물게 위장장애
        """.trimIndent()

        assertTrue(parser.isLikelyDrugBag(validDrugText))
    }

    @Test
    fun `약봉투 아님 검증`() {
        val invalidTexts = listOf(
            "오늘 날씨가 좋습니다",
            "점심 메뉴: 김치찌개",
            "회의 일정: 오후 2시",
            "연락처: 010-1234-5678"
        )

        invalidTexts.forEach { text ->
            assertFalse(parser.isLikelyDrugBag(text), "Should not be drug bag: $text")
        }
    }

    @Test
    fun `실제 약봉투 텍스트 복합 테스트`() {
        val realDrugBagText = """
            서울대학교병원 약국

            홍길동님 (성별: 남/나이: 45세)

            처방일: 2024년 1월 15일

            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            [약물 1]
            약품명: 타이레놀정 (Acetaminophen)
            용량: 500mg
            분량: 30정

            용법:
            • 1회 1정씩
            • 1일 3회
            • 식후 30분 복용
            • 오전 8시, 오후 2시, 저녁 8시

            복용기간: 10일간 복용

            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            효능효과:
            감기로 인한 두통, 발열, 신경통, 근육통

            부작용:
            드물게 피부발진, 두드러기, 위장장애가 나타날 수 있습니다

            주의사항:
            • 간기능 장애 환자는 의사와 상담 후 복용
            • 정해진 용량을 초과하지 마세요
            • 3일 이상 복용해도 증상 개선이 없으면 의사와 상담

            보관법:
            직사광선을 피해 실온에서 보관하고 어린이 손이 닿지 않는 곳에 보관하세요

            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            제조: 한미약품 (주)
            주소: 경기도 성남시 분당구

            ☎ 문의: 1577-1234

            약사: 김약사 (면허번호: 제12345호)
        """.trimIndent()

        val drugInfo = parser.parseDrugText(realDrugBagText)

        assertEquals("타이레놀정", drugInfo.drugName)
        assertEquals("1정(3회)", drugInfo.dosage)
        assertEquals(3, drugInfo.scheduleInfo.times.size)
        assertTrue(drugInfo.scheduleInfo.times.contains(LocalTime.of(8, 0)))
        assertTrue(drugInfo.scheduleInfo.times.contains(LocalTime.of(14, 0)))
        assertTrue(drugInfo.scheduleInfo.times.contains(LocalTime.of(20, 0)))
        assertEquals("매일 복용", drugInfo.scheduleInfo.pattern)
        assertEquals("10일간 복용", drugInfo.scheduleInfo.duration)
        assertEquals("한미약품 (주)", drugInfo.manufacturer)
        assertTrue(drugInfo.scheduleInfo.instructions.contains("식후 30분 복용"))
        assertTrue(drugInfo.description.contains("감기로 인한 두통, 발열"))
        assertTrue(drugInfo.description.contains("피부발진, 두드러기, 위장장애"))
    }

    @Test
    fun `다양한 시간 형식 파싱`() {
        val testCases = mapOf(
            "8:30" to LocalTime.of(8, 30),
            "14:00" to LocalTime.of(14, 0),
            "오전 9시 15분" to LocalTime.of(9, 15),
            "오후 3시" to LocalTime.of(15, 0),
            "밤 11시" to LocalTime.of(23, 0)
        )

        testCases.forEach { (input, expected) ->
            val text = "복용시간: $input"
            val drugInfo = parser.parseDrugText(text)
            assertTrue(drugInfo.scheduleInfo.times.contains(expected), "Failed for: $input")
        }
    }

    @Test
    fun `복잡한 복용 용법 파싱`() {
        val text = """
            아무게정 200mg

            처방량: 1회 2정, 1일 3회

            복용법:
            - 아침 식전 30분: 2정
            - 점심 식후 1시간: 2정
            - 저녁 취침전: 2정

            주기: 5일 복용 후 2일 휴식
            총 복용기간: 3주간
        """.trimIndent()

        val drugInfo = parser.parseDrugText(text)

        assertEquals("아무게정", drugInfo.drugName)
        assertEquals("2정(3회)", drugInfo.dosage)
        assertEquals("5일 복용 2일 휴식", drugInfo.scheduleInfo.pattern)
        assertEquals("3주간 복용", drugInfo.scheduleInfo.duration)
        assertTrue(drugInfo.scheduleInfo.instructions.contains("식전 30분 복용"))
        assertTrue(drugInfo.scheduleInfo.instructions.contains("식후 1시간 복용"))
        assertTrue(drugInfo.scheduleInfo.instructions.contains("취침 전 복용"))
    }
}