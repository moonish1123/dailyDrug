# LlmModule - Claude/GPT/Local 통합 가이드

## 개요

DailyDrug 앱은 `llmmodule` 라이브러리를 통해 Claude, GPT, Local LLM 세 가지 제공자를 단일 인터페이스로 호출합니다.  
핵심 목표는 **제공자 추상화**와 **교체 가능한 구조**이며, 앱 레이어는 `LlmRepository`만 의존하도록 설계되어 있습니다.

### 지원 제공자
- `claude` – Anthropic Messages API (`claude-3-5-sonnet-latest`)
- `gpt` – OpenAI Chat Completions API (`gpt-3.5-turbo`)
- `local` – 오프라인 시뮬레이션 응답 (테스트/데모용)

API 키는 `"provider/실제키"` 형태로 저장하며, `ParseApiKeyUseCase`와 `LlmProvider.parseApiKey`가 이를 분리합니다.

---

## 아키텍처

```
┌───────────────────────────────┐
│ DailyDrug App (ViewModel 등) │
└───────────────┬───────────────┘
                │
┌───────────────▼───────────────┐
│ LlmRepository (interface)     │
│ LlmRequest / LlmResponse 등   │
└───────────────┬───────────────┘
                │ 멀티 바인딩
┌───────────────▼───────────────┐
│ LlmService 구현 세트          │
│ ├─ ClaudeLlmService           │
│ ├─ GptLlmService              │
│ └─ LocalLlmService            │
└───────────────┬───────────────┘
                │
┌───────────────▼───────────────┐
│ NetworkModule (Retrofit/OkHttp)│
└───────────────────────────────┘
```

`LlmRepositoryImpl`은 설정으로부터 활성 제공자를 찾고, 일치하는 `LlmService`로 요청을 위임합니다.

---

## 핵심 소스 코드

| 계층 | 파일 | 설명 |
|------|------|------|
| Domain | `llmmodule/src/main/java/com/llmmodule/domain/model/LlmProvider.kt` | `claude/gpt/local` 식별자와 API 키 파싱 |
| Domain | `.../domain/repository/LlmRepository.kt` | 앱이 의존하는 추상 인터페이스 |
| Data | `.../data/repository/LlmRepositoryImpl.kt` | 활성 제공자 라우팅, 에러 변환 |
| Data | `.../data/provider/claude/ClaudeLlmService.kt` | Anthropic Messages 호출 |
| Data | `.../data/provider/gpt/GptLlmService.kt` | OpenAI Chat Completions 호출 |
| Data | `.../data/provider/local/LocalLlmService.kt` | 오프라인 응답 샘플 |
| DI | `.../di/LlmModule.kt` | Hilt 멀티바인딩으로 서비스 셋 주입 |

---

## Claude 구현 요약

- **엔드포인트**: `POST https://api.anthropic.com/v1/messages`
- **헤더**: `x-api-key`, `anthropic-version=2023-06-01`
- **모델**: `claude-3-5-sonnet-latest`
- **요청** (`ClaudeMessagesRequest`):
  - `system`: 시스템 지시문(`LlmRequest.systemInstructions` 합쳐서 전달)
  - `messages`: 사용자 프롬프트를 텍스트 블록 한 개로 구성
  - `max_tokens`: 요청값이 없으면 1024 기본값 사용
- **응답 처리**:
  - `content` 배열에서 `type == "text"` 항목을 찾아 메시지 본문으로 사용
  - `usage`를 `LlmResponse.TokenUsage`로 변환
- **에러 처리**:
  - HTTP 에러 시 본문 문자열 또는 상태코드 기반 메시지 생성
  - `IOException` → `LlmError.Network`, 기타 → `LlmError.Unknown`

---

## GPT 구현 요약

- **엔드포인트**: `POST https://api.openai.com/v1/chat/completions`
- **모델**: `gpt-3.5-turbo`
- **스트리밍 제거**: 단순 응답만 지원하도록 `stream` 관련 코드와 모델 필드를 제거했습니다.
- **요청**: 시스템 지시문/사용자 메시지를 `messages` 배열로 전달, `max_tokens`/`temperature`만 옵션으로 허용

---

## LocalLlmService

- 네트워크 없이 `[LOCAL] 프롬프트` 형식의 고정 응답을 돌려 주어 디버그 및 데모 환경에서 빠르게 확인할 수 있습니다.
- API 키가 없어도 동작하며, 실제 네트워크 호출은 수행하지 않습니다.

---

## Hilt 구성 (`llmmodule/src/main/java/com/llmmodule/di/LlmModule.kt`)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object LlmProviderModule {
    @Provides @Singleton @IntoSet
    fun provideClaudeService(factory: NetworkClientFactory): LlmService =
        ClaudeLlmService(factory)

    @Provides @Singleton @IntoSet
    fun provideGptService(factory: NetworkClientFactory): LlmService =
        GptLlmService(factory)

    @Provides @Singleton @IntoSet
    fun provideLocalService(): LlmService = LocalLlmService()
}
```

`LlmBindingsModule`는 선택적 `LlmConfiguration`을 받아 `LlmRepositoryImpl`을 생성합니다. 앱은 `LlmConfiguration` 구현을 제공해 활성 제공자와 API 키 문자열을 전달해야 합니다.

---

## 사용 예시

```kotlin
val request = LlmRequest(
    prompt = "복약 스케줄을 요약해줘",
    systemInstructions = listOf("간결하게 답변하세요."),
    temperature = 0.7
)

llmRepository.generateText(request).collect { result ->
    when (result) {
        is LlmResult.Success -> showText(result.data.text)
        is LlmResult.Error -> handleError(result.error)
    }
}
```

---

## 통합 테스트

- `llmmodule/src/test/java/com/llmmodule/integration/LlmApiIntegrationTest.kt`
  - `ANTHROPIC_API_KEY`와 `OPENAI_API_KEY`가 `local.properties`에 존재할 때 실 API 호출
- `llmmodule/src/test/java/com/llmmodule/integration/KoreanHistorySummaryTest.kt`
  - 동일 키가 있을 때 Claude/GPT 응답 비교 출력
- 키가 없으면 `Assume` 덕분에 자동으로 테스트가 건너뜁니다.

---

## 향후 개선 아이디어

1. **함수 호출/JSON 모드**: Claude와 GPT 양쪽 모두 구조화 응답 지원
2. **멀티모달**: 이미지 입력이 필요한 경우를 대비한 확장
3. **프롬프트 템플릿**: 반복 프롬프트를 위한 템플릿 레이어
4. **비용 추적**: 토큰 사용량 기반 비용 로깅
5. **폴백 체인**: 특정 제공자가 실패할 때 다른 제공자로 자동 전환

---

## 적용 요약

- Gemini 전용 코드는 삭제되었으며, 동일한 추상화로 Claude가 교체됩니다.
- GPT는 비스트리밍 경로만 유지하여 구현 복잡도를 낮췄습니다.
- 문서 파일은 `llm_module.md`로 정리되어 최신 구조와 사용법을 다룹니다.
