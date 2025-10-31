# 매일 약먹기 (DailyDrug) - 프로젝트 사양서

## 1. 프로젝트 개요

### 1.1 프로젝트 목표
매일 복용해야 하는 약을 잊지 않고 복용할 수 있도록 돕는 알림 앱 개발

### 1.2 핵심 기능
- 투약 스케줄 등록 및 관리
- 복용 시간 알림 (미복용 시 1시간마다 재알림)
- 연속 투약 패턴 설정 (예: 5일 복용 + 1일 휴식)
- 복용 여부 기록 및 이력 관리

### 1.3 UX 목표
- 블루 계열의 시원하고 모던한 디자인
- 심플하고 직관적인 인터페이스
- 복용 상태에 따른 명확한 시각적 피드백 (미복용: 빨강, 복용 완료: 파랑)

---

## 2. 기술 스택

### 2.1 개발 환경
- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build Tool**: Gradle with Kotlin DSL

### 2.2 아키텍처 및 라이브러리

#### Clean Architecture Layers
```
Presentation Layer (UI + ViewModel)
    ↓
Domain Layer (UseCase)
    ↓
Data Layer (Repository + DataSource)
```

#### Core Libraries
- **UI**: Jetpack Compose + Material3
- **Navigation**: Compose Navigation
- **Database**: Room
- **Async**: Coroutines + Flow
- **DI**: Hilt
- **Notification**: WorkManager + AlarmManager
- **Network** (Optional): OkHttp + Retrofit
- **Testing**: JUnit5, Kotest, Compose UI Test

---

## 3. 데이터 모델 설계

### 3.1 Entity Definitions

#### Medicine (약 정보)
```kotlin
@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // 약 이름
    val dosage: String,                  // 복용량 (예: "1정", "5ml")
    val color: Int,                      // 약 식별 색상
    val memo: String = "",               // 메모
    val createdAt: Long = System.currentTimeMillis()
)
```

#### MedicationSchedule (투약 스케줄)
```kotlin
@Entity(tableName = "medication_schedules")
data class MedicationSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicineId: Long,                // Medicine FK
    val startDate: LocalDate,            // 시작일
    val endDate: LocalDate?,             // 종료일 (null: 무기한)
    val timeSlots: List<LocalTime>,      // 복용 시간들 (JSON 저장)
    val takeDays: Int,                   // 연속 복용 일수
    val restDays: Int,                   // 휴식 일수 (0: 매일)
    val isActive: Boolean = true
)
```

#### MedicationRecord (복용 기록)
```kotlin
@Entity(tableName = "medication_records")
data class MedicationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,                // MedicationSchedule FK
    val scheduledDateTime: LocalDateTime, // 예정 복용 시간
    val takenDateTime: LocalDateTime?,   // 실제 복용 시간 (null: 미복용)
    val isTaken: Boolean = false,
    val skipped: Boolean = false,        // 건너뜀 여부
    val note: String = ""
)
```

### 3.2 Type Converters
- LocalDate ↔ Long
- LocalTime ↔ String
- LocalDateTime ↔ Long
- List<LocalTime> ↔ JSON String

---

## 4. 화면 구조

### 4.1 Navigation Graph
```
MainActivity (NavHost)
├── MainScreen (Home)              // 오늘의 복용 목록
├── ScheduleInputScreen           // 스케줄 등록/수정
├── MedicineDetailScreen          // 약 상세/이력
└── SettingsScreen               // 설정
```

### 4.2 Screen Specifications

#### MainScreen (홈 화면)
**목적**: 오늘 복용해야 할 약 목록 표시 및 복용 기록

**구성요소**:
- TopAppBar: 제목 "매일 약먹기", 설정 아이콘
- 날짜 선택기: 오늘 날짜 (스와이프로 전환 가능)
- 복용 리스트:
  - 미복용: 빨간색 배경, 터치 유도 애니메이션
  - 복용 완료: 파란색 배경, 체크 마크
  - 각 아이템: 약 이름, 복용량, 예정 시간, 복용 버튼
- FAB: "+" 버튼 → ScheduleInputScreen 이동

**상태 관리**:
```kotlin
data class MainUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val todayMedications: List<TodayMedication> = emptyList(),
    val isLoading: Boolean = false
)

data class TodayMedication(
    val recordId: Long,
    val medicineName: String,
    val dosage: String,
    val scheduledTime: LocalTime,
    val isTaken: Boolean,
    val takenTime: LocalDateTime?,
    val color: Int
)
```

#### ScheduleInputScreen (스케줄 입력)
**목적**: 새 약 투약 스케줄 등록

**구성요소**:
- 약 정보 입력: 이름, 복용량, 색상 선택
- 기간 설정: 시작일, 종료일 (선택)
- 복용 시간: 복수 시간 추가 가능
- 복용 패턴 선택:
  - 매일 복용
  - 커스텀: N일 복용 + M일 휴식
  - 프리셋: "1일 복용 1일 휴식", "5일 복용 1일 휴식"
- 저장/취소 버튼

#### MedicineDetailScreen (약 상세)
**목적**: 특정 약의 복용 이력 조회

**구성요소**:
- 약 정보 표시
- 스케줄 정보
- 복용 이력 캘린더 뷰
- 통계: 복용률, 연속 복용 일수

---

## 5. 알림 시스템 설계

### 5.1 권한 요구사항
```kotlin
// Android 13+
Manifest.permission.POST_NOTIFICATIONS
Manifest.permission.SCHEDULE_EXACT_ALARM

// Runtime Permission Request
- POST_NOTIFICATIONS: 런타임 요청 필수
- SCHEDULE_EXACT_ALARM: 설정 화면으로 유도
```

### 5.2 알림 전략

#### WorkManager + AlarmManager 혼합 사용
- **WorkManager**: 일일 스케줄 생성 (매일 자정)
- **AlarmManager**: 정확한 시간 알림 트리거

#### Notification Channel 설정
```kotlin
NotificationChannel(
    id = "medication_reminder",
    name = "복용 알림",
    importance = NotificationManager.IMPORTANCE_HIGH
).apply {
    enableVibration(true)
    enableLights(true)
    setSound(defaultSoundUri, audioAttributes)
}
```

---

## 추가 구현 스펙
- **월별 기록 선 생성 로직**: 스케줄 저장 시점과 매월 1일 자정에 다음 달 말일까지 `medication_records`를 자동 확장해 복용 일정이 끊기지 않도록 한다.
- **홈 화면 ‘오늘’ 버튼**: 메인 화면 날짜 헤더에 ‘오늘’ 버튼을 배치해 날짜 탐색 후 현재 날짜로 즉시 복귀할 수 있게 한다.
- **알림 액션 확장**: 알림 카드에 `오늘 약 먹음` 액션을 추가하고, 사용자가 복용 완료로 처리하지 않을 경우 기존 알림을 해제한 뒤 재스케줄링하여 반복 알림을 보낸다.

### 5.3 재알림 로직
```kotlin
// 미복용 시 1시간마다 재알림
- 최초 알림 시간: 예정 시간
- 재알림 간격: 1시간
- 재알림 횟수: 무제한 (복용 또는 다음날까지)
- 알림 취소 조건: 복용 기록, 건너뜀, 다음날 00:00
```

### 5.4 Notification Action
```kotlin
// Full-screen Intent (고우선순위)
- 앱 실행 → 해당 약 복용 기록 화면
- Quick Action: "복용 완료", "1시간 후 알림"
```

---

## 6. Clean Architecture 구현

### 6.1 Package Structure
```
com.dailydrug
├── data
│   ├── local
│   │   ├── dao
│   │   │   ├── MedicineDao.kt
│   │   │   ├── MedicationScheduleDao.kt
│   │   │   └── MedicationRecordDao.kt
│   │   ├── entity (위 3.1 참조)
│   │   ├── converter
│   │   │   └── DateTimeConverters.kt
│   │   └── database
│   │       └── AppDatabase.kt
│   ├── repository
│   │   └── MedicationRepositoryImpl.kt
│   └── worker
│       ├── DailyScheduleWorker.kt
│       └── MedicationReminderWorker.kt
├── domain
│   ├── model (Clean domain models)
│   ├── repository
│   │   └── MedicationRepository.kt (interface)
│   └── usecase
│       ├── GetTodayMedicationsUseCase.kt
│       ├── RecordMedicationUseCase.kt
│       ├── CreateScheduleUseCase.kt
│       ├── CalculateSchedulePatternsUseCase.kt
│       └── ScheduleNotificationUseCase.kt
├── presentation
│   ├── main
│   │   ├── MainScreen.kt
│   │   ├── MainViewModel.kt
│   │   └── MainUiState.kt
│   ├── schedule
│   │   ├── ScheduleInputScreen.kt
│   │   └── ScheduleInputViewModel.kt
│   ├── detail
│   │   ├── MedicineDetailScreen.kt
│   │   └── MedicineDetailViewModel.kt
│   ├── component (공통 Composable)
│   └── theme (Material3 Theme)
├── di
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   └── UseCaseModule.kt
└── MainActivity.kt
```

### 6.2 의존성 주입 (Hilt)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase

    @Provides
    fun provideMedicineDao(db: AppDatabase): MedicineDao
}
```

---

## 7. 구현 단계 (Phases)

### Phase 0: 프로젝트 설정
**목표**: 프로젝트 초기 설정 및 기본 구조 생성

**Tasks**:
1. Android 프로젝트 생성 (Compose template)
2. build.gradle.kts 의존성 추가
   - Compose BOM
   - Hilt
   - Room
   - WorkManager
   - Navigation Compose
3. 패키지 구조 생성 (data/domain/presentation)
4. Hilt Application 클래스 생성
5. Material3 Theme 설정 (블루 계열 ColorScheme)

**완료 기준**:
- 앱 실행 가능
- Hilt DI 동작 확인
- 기본 Navigation 설정 완료

---

### Phase 1: 데이터 레이어
**목표**: Room Database 및 Repository 구현

**Tasks**:
1. Entity 클래스 작성 (Medicine, MedicationSchedule, MedicationRecord)
2. Type Converters 구현
3. DAO 인터페이스 작성
4. AppDatabase 클래스 구현
5. Repository 인터페이스 정의 (domain/repository)
6. RepositoryImpl 구현 (data/repository)
7. DatabaseModule 작성 (Hilt)

**완료 기준**:
- Unit Test: DAO CRUD 동작 확인
- Repository 의존성 주입 확인
- 데이터 읽기/쓰기 정상 동작

---

### Phase 2: 도메인 레이어
**목표**: UseCase 구현 및 비즈니스 로직 분리

**Tasks**:
1. Domain Model 정의 (Entity와 분리)
2. UseCase 구현:
   - GetTodayMedicationsUseCase
   - RecordMedicationUseCase
   - CreateScheduleUseCase
   - CalculateSchedulePatternsUseCase (복용 패턴 계산)
3. UseCaseModule 작성 (Hilt)

**핵심 로직**:
```kotlin
// CalculateSchedulePatternsUseCase 예시
fun shouldTakeMedicationOn(
    startDate: LocalDate,
    targetDate: LocalDate,
    takeDays: Int,
    restDays: Int
): Boolean {
    if (restDays == 0) return true // 매일 복용

    val daysSinceStart = ChronoUnit.DAYS.between(startDate, targetDate)
    val cycleLength = takeDays + restDays
    val dayInCycle = (daysSinceStart % cycleLength).toInt()

    return dayInCycle < takeDays
}
```

**완료 기준**:
- Unit Test: 각 UseCase 로직 검증
- 복용 패턴 계산 알고리즘 테스트 (다양한 케이스)

---

### Phase 3: 알림 시스템
**목표**: WorkManager + AlarmManager 기반 알림 구현

**Tasks**:
1. 권한 요청 로직 구현 (POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM)
2. NotificationHelper 클래스 작성
   - Channel 생성
   - Notification 빌더
   - Full-screen Intent 설정
3. DailyScheduleWorker 구현 (매일 자정 실행)
   - 다음날 복용 스케줄 생성
   - AlarmManager 알림 등록
4. MedicationAlarmReceiver 구현 (BroadcastReceiver)
   - 알림 표시
   - 재알림 스케줄링 (1시간 후)
5. ScheduleNotificationUseCase 구현
6. 알림 취소 로직 (복용 기록 시)

**완료 기준**:
- 정확한 시간에 알림 수신 확인
- 미복용 시 1시간마다 재알림 동작 확인
- 복용 기록 시 재알림 취소 확인
- 백그라운드 동작 테스트

---

### Phase 4: UI 레이어
**목표**: Compose UI 및 ViewModel 구현

**Tasks**:

#### 4.1 MainScreen
1. MainViewModel 구현
   - UiState 정의
   - getTodayMedications Flow 구독
   - recordMedication 이벤트 처리
2. MainScreen Composable 작성
   - 날짜 선택 헤더
   - 복용 리스트 (LazyColumn)
   - 복용 상태별 디자인 (미복용/완료)
   - 복용 버튼 클릭 → recordMedication
3. MedicationItem Composable (재사용 컴포넌트)

#### 4.2 ScheduleInputScreen
1. ScheduleInputViewModel 구현
   - 입력 상태 관리
   - 유효성 검증
   - createSchedule 이벤트
2. ScheduleInputScreen Composable
   - 약 정보 입력 폼
   - 시간 선택 (TimePicker)
   - 패턴 선택 UI
   - 저장 버튼

#### 4.3 MedicineDetailScreen
1. MedicineDetailViewModel 구현
   - 특정 약의 이력 로드
   - 통계 계산
2. MedicineDetailScreen Composable
   - 약 정보 카드
   - 캘린더 뷰 (복용 이력)
   - 통계 표시

#### 4.4 Navigation 설정
```kotlin
NavHost(navController, startDestination = "main") {
    composable("main") { MainScreen(navController) }
    composable("schedule/{scheduleId}") { ... }
    composable("detail/{medicineId}") { ... }
}
```

**완료 기준**:
- 모든 화면 정상 렌더링
- 화면 간 Navigation 동작
- ViewModel ↔ UI 상태 동기화 확인
- Compose UI Test 작성 및 통과

---

### Phase 5: 통합 및 테스트
**목표**: End-to-End 테스트 및 최종 검증

**Tasks**:
1. E2E 시나리오 테스트
   - 스케줄 등록 → 알림 수신 → 복용 기록 → 이력 확인
2. Edge Case 테스트
   - 자정 넘어가는 경우
   - 복용 패턴 변경 시 알림 재계산
   - 앱 종료 상태에서 알림
3. 성능 테스트
   - 대량 데이터 로드 (100개 이상 스케줄)
   - UI 렌더링 성능
4. 접근성 검증
   - TalkBack 동작 확인
   - 색상 대비 검증
5. 릴리즈 빌드 설정
   - ProGuard 규칙
   - 서명 설정

**완료 기준**:
- 모든 핵심 시나리오 정상 동작
- 크래시 0건 (7일 사용 테스트)
- 접근성 점수 기준 충족

---

## 8. 테스트 전략

### 8.1 Unit Test (JUnit5 + Kotest)
**대상**:
- UseCase 로직
- Repository 구현
- ViewModel 로직
- 복용 패턴 계산 알고리즘

**커버리지 목표**: 80% 이상

### 8.2 Integration Test
**대상**:
- Room DAO 동작 (In-memory DB)
- Repository ↔ DataSource 통합

### 8.3 UI Test (Compose Testing)
**대상**:
- 각 Screen의 렌더링
- 사용자 인터랙션 (버튼 클릭, 입력)
- Navigation 흐름

**예시**:
```kotlin
@Test
fun mainScreen_showsTodayMedications() {
    composeTestRule.setContent {
        MainScreen(navController = rememberNavController())
    }

    composeTestRule.onNodeWithText("약 이름").assertIsDisplayed()
    composeTestRule.onNodeWithText("복용").performClick()
}
```

### 8.4 E2E Test
**시나리오**:
1. 스케줄 등록 → DB 저장 확인
2. 알림 시간 도래 → 알림 표시 확인
3. 복용 기록 → UI 업데이트 및 재알림 취소 확인

---

## 9. 개발 가이드라인

### 9.1 코드 스타일
- Kotlin Official Style Guide 준수
- ktlint 사용 (자동 포맷팅)
- Compose 권장사항 따르기 (remember, derivedStateOf, LaunchedEffect 적절히 사용)

### 9.2 Git Workflow
- Feature 브랜치 전략
- 커밋 메시지: `[Phase N] 작업 내용` 형식
- PR 단위: Phase 별 또는 주요 기능 단위

### 9.3 성능 고려사항
- LazyColumn에서 key 파라미터 사용
- ViewModel에서 불필요한 recomposition 방지
- Room Query 최적화 (인덱스 설정)

---

## 10. 향후 확장 가능성

### 10.1 추가 기능 아이디어
- 복용 완료 시 보상 시스템 (스트릭, 배지)
- 약 재고 관리 및 재구매 알림
- 가족 구성원 약 관리 (멀티 프로필)
- 약 정보 API 연동 (의약품안전나라)
- 복용 통계 및 리포트 (주간/월간)

### 10.2 기술적 개선
- Jetpack Compose Multiplatform (iOS 지원)
- 클라우드 백업 (Firebase)
- Wear OS 지원 (워치 알림)

---

## 11. 참고 자료

### 11.1 공식 문서
- [Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [WorkManager Guide](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Notification Guide](https://developer.android.com/develop/ui/views/notifications)

### 11.2 아키텍처 가이드
- [Guide to App Architecture](https://developer.android.com/topic/architecture)
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

---

## 부록: build.gradle.kts 의존성 예시

```kotlin
dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Hilt
    val hiltVersion = "2.50"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```
