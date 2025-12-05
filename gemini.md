# gemini.md

이 파일은 Google Deepmind의 Gemini가 이 프로젝트(DailyDrug)를 효과적으로 수행하기 위한 가이드라인과 컨텍스트를 담고 있습니다.

## 1. 핵심 원칙 (Core Principles)

### 1.1 기본 행동 수칙
- **언어**: 모든 대답은 **한국어**로 합니다.
- **검증**: 모든 대답과 코드는 출력 전 반드시 검증 과정을 거칩니다.
- **심층 분석 (Deep Review)**: 작업 완료 후, 수행한 내용을 단순히 나열하는 것을 넘어, **Flow를 1 depth 더 깊게 탐색**하여 분석하고 검토합니다. 놓친 엣지 케이스나 사이드 이펙트가 없는지 철저히 확인합니다.
- **MCP 활용**: `context7`, `serena`와 같은 MCP가 있다면 적극적으로 활용하여 컨텍스트를 풍부하게 유지합니다. (현재 환경에서 가용한 도구를 최대한 활용하여 깊이 있는 문맥 파악을 지향합니다.)

### 1.2 SuperGemini Framework Components
사용자의 메모리에 정의된 프레임워크 컴포넌트를 준수합니다.
- `@AGENTS.md`: 프로젝트별 구체적인 가이드라인
- `@FLAGS.md`, `@PRINCIPLES.md`, `@RULES.md`: 핵심 프레임워크 (참조)
- 행동 모드: Brainstorming, Introspection, Task Management, Token Efficiency 등을 상황에 맞게 적용합니다.

---

## 2. 프로젝트 개요 (Project Overview)

**DailyDrug (매일 약먹기)**
- **목표**: 사용자가 약 복용을 잊지 않도록 돕는 안드로이드 알림 앱.
- **핵심 가치**: "약 먹을 때까지 끝까지 알림" (미복용 시 1시간마다 재알림).
- **디자인**: 블루 계열의 모던하고 시원한 Material3 디자인.

---

## 3. 기술 스택 및 아키텍처 (Tech Stack & Architecture)

### 3.1 기술 스택
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: Clean Architecture (Presentation - Domain - Data) + MVVM
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Local DB**: Room
- **Background**: WorkManager (스케줄링), AlarmManager (정확한 시간 알림)

### 3.2 모듈 구조
- `app`: 메인 애플리케이션 모듈 (UI, DI, DB, Worker 등 포함)
- `llmmodule`: LLM 관련 기능 (현재 구조상 존재, 역할 확인 필요)
- `networkmodule`: 네트워크 통신 모듈

### 3.3 Clean Architecture 계층
1.  **Presentation Layer** (`app/src/main/java/com/dailydrug/presentation`)
    *   UI (Compose), ViewModel, StateHolder
2.  **Domain Layer** (`app/src/main/java/com/dailydrug/domain`)
    *   **순수 Kotlin 모듈**. Android 의존성 없음.
    *   UseCase, Repository Interface, Domain Model
3.  **Data Layer** (`app/src/main/java/com/dailydrug/data`)
    *   Repository Implementation, Room DB/DAO, DataSource, Worker

---

## 4. 개발 워크플로우 (Development Workflow)

### 4.1 구현 단계 (Phases)
`PROJECT_SPEC.md`에 정의된 단계를 따릅니다.
- **Phase 0**: 프로젝트 설정 (완료)
- **Phase 1**: 데이터 레이어 (Room, Repository) (완료)
- **Phase 2**: 도메인 레이어 (UseCase) (완료)
- **Phase 3**: 알림 시스템 (WorkManager, AlarmManager) (기능 구현됨)
- **Phase 4**: UI 레이어 (Compose, ViewModel) (완료)
- **Phase 5**: 통합 및 테스트 (진행 중/예정)

### 4.2 커밋 및 PR 가이드
- **커밋 메시지**: `[Phase N] 작업 내용` (예: `[Phase 4] 메인 화면 UI 구현`)
- **PR**: 작업 범위, 테스트 내용, 관련 Phase 링크 포함. UI 변경 시 스크린샷/영상 필수.

### 4.3 테스트 전략
- **Unit Test**: UseCase, ViewModel, 알고리즘 (JUnit5, Kotest)
- **UI Test**: Compose Test
- **명령어**:
    *   전체 테스트: `./gradlew test`
    *   특정 테스트: `./gradlew test --tests "fully.qualified.TestName"`

---

## 5. 현재 상태 및 우선순위 (Current Status)

### 5.1 완료된 작업 (`AGENTS.md` 기준)
- Phase 0 ~ 4 완료.
- 홈/스케줄/상세 화면 구현 완료.
- Room DB, UseCase, Repository 연결 완료.
- 알림 시스템 기본 구현 완료 (NotificationHelper, AlarmReceiver, Worker).

### 5.2 진행 예정 작업 (Next Steps)
- **UI 레이어 통합**: ViewModel과 UI의 완전한 연동.
- **상태 지속성**: UI 상태 저장 및 복원.
- **End-to-End 흐름 완성**: 스케줄 생성 -> 알림 -> 복용 기록 -> 이력 확인의 전체 사이클 검증.
- **테스트 보강**: 주요 로직에 대한 테스트 커버리지 확보.

---

## 6. 심층 분석 체크리스트 (Deep Review Checklist)

작업 완료 후 다음 항목을 스스로 점검합니다.

1.  **Flow Analysis**: 변경된 코드가 전체 데이터 흐름(Data Flow)에 어떤 영향을 미치는가?
    *   *예: DB 스키마 변경이 마이그레이션, 기존 데이터, UI 표시, 백그라운드 워커에 미치는 영향.*
2.  **Edge Cases**:
    *   날짜 변경 시점 (자정)
    *   네트워크/DB 에러 상황
    *   앱이 백그라운드/종료된 상태에서의 동작 (특히 알림)
3.  **Consistency**:
    *   `PROJECT_SPEC.md`의 요구사항과 일치하는가?
    *   Clean Architecture 원칙을 위배하지 않았는가? (예: Domain에서 Android 의존성 사용)
4.  **User Experience**:
    *   UI 반응성, 에러 메시지, 로딩 상태가 적절한가?

---

참고 파일:
- [AGENTS.md](AGENTS.md)
- [PROJECT_SPEC.md](PROJECT_SPEC.md)
- [CLAUDE.md](CLAUDE.md)
