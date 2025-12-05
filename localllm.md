# Local LLM (ExecuTorch 기반) 적용 가이드

## 1. 레퍼런스 프로젝트 요약

- **LlamaDemo (Android)** – [`meta-pytorch/executorch-examples`](https://github.com/meta-pytorch/executorch-examples/tree/main/llm/android/LlamaDemo)  
  - Android Studio 기반 샘플이며, ExecuTorch로 변환한 LLaMA 모델을 단말 내에서 추론한다.  
  - 주요 구성:  
    - `app/src/main/java/.../inference` 패키지: ExecuTorch 런타임을 감싸는 `ExecutorRunner`, 토크나이저, LoRA 어댑터 로딩.  
    - `assets/models/`: `.pte` 모델, 토크나이저 사전, LoRA 가중치.  
    - `gradle/libs.versions.toml`, `build.gradle`: ExecuTorch AAR, PyTorch-Android, SentencePiece 등을 포함.  
    - UI는 Compose + ViewModel로 프롬프트/응답을 표시하며, 추론은 `Dispatchers.IO` 코루틴에서 실행.

- **ExecuTorch Android 문서** – [Docs: Using ExecuTorch on Android](https://docs.pytorch.org/executorch/main/using-executorch-android.html)  
  - ExecuTorch의 AAR 추가, `experimental.torch.compile`로 모델을 `.pte`로 내보내는 절차, JNI/Java API 사용법을 설명.  
  - 모델 로딩 시 `LiteModuleLoader.load(assetFilePath)` 패턴, 입력/출력 텐서 버퍼 인터페이스, 스레드 풀 구성 등을 다룬다.

- **ExecuTorch README** – [github.com/pytorch/executorch](https://github.com/pytorch/executorch?tab=readme-ov-file)  
  - ExecuTorch 개요, `torch.export` → `etrecord` 변환 → 대상 플랫폼(예: Android) 배포 흐름 제시.  
  - AOT( ahead-of-time ) 컴파일·최적화, 러타임 모듈 구조(`libexecutorch`, `etdump`) 및 지원 백엔드(OpenCL, Vulkan 등)에 대해 정리.

## 2. 전체 적용 흐름

### 2.1 모델 준비
1. **PyTorch 모델 내보내기**  
   ```python
   import torch
   from torch._export import capture_pre_autograd_graph

   model = ...  # LLaMA 변형 등
   example_inputs = (prompt_tensor,)
   exported = capture_pre_autograd_graph(model, example_inputs)
   exported.save("llama.et")
   ```
2. **ExecuTorch 컴파일**  
   ```python
   from executorch import to_edge

   edge_program = to_edge(exported, compile_config=...)
   edge_program.write("llama.pte")
   ```
   - LlamaDemo는 `compile_graph.py`에서 LoRA, KV 캐시 등 옵션을 설정한다.  
   - 생성된 `.pte`, 토크나이저(예: `tokenizer.model`), LoRA 가중치(`lora.bin`)를 Android `assets/models`에 배치.

### 2.2 Android 프로젝트 구성
1. **Gradle 설정 (`build.gradle`)**
   ```kotlin
   repositories {
       mavenCentral()
       google()
   }

   dependencies {
       implementation("org.pytorch:torch-android-lite:<ver>")
       implementation("org.pytorch:executorch-android:<ver>")
       implementation("org.pytorch:executorch-llm:<ver>") // LLaMA util이 필요할 경우
       implementation("io.reactivex.rxjava3:rxjava:3.x")  // 샘플에 따라 선택
   }
   ```
   - LlamaDemo는 `libs.versions.toml`에서 ExecuTorch AAR 버전을 관리한다.

2. **Assets 관리**  
   - `src/main/assets/models/llama.pte`, `tokenizer.model`, `adapter_config.json` 등을 복사.  
   - 빌드 시 50MB 이상 파일이 있다면 `packagingOptions.resources.excludes` 또는 `aaptOptions` 설정으로 압축 제외.

3. **네이티브 권한**  
   - ExecuTorch 자체는 추가 권한이 필요하지 않지만, 장치 저장공간 접근 또는 인터넷(모델 다운로드)을 원하면 `AndroidManifest.xml`에 정의.  
   - LlamaDemo는 로컬 자산만 사용하므로 기본 권한으로 충분하다.

### 2.3 런타임 통합
1. **모델 로딩**
   ```kotlin
   private val module by lazy {
       Module.load(assetFilePath("models/llama.pte"))
   }
   ```
   - `assetFilePath`는 `context.assets.openFd`로 임시 파일을 복사하는 유틸리티 사용.
   - ExecuTorch LLM SDK(`ExecutorRunner`)를 쓰면 KV 캐시, LoRA 어댑터 적용 등을 자동 처리.

2. **토크나이저**
   - SentencePiece 모델(`tokenizer.model`)을 로딩해 입력 텍스트를 토큰 ID 배열로 변환.  
   - LlamaDemo는 `Tokenizer` 클래스를 두어 `encode`, `decode`를 제공한다.

3. **추론 루프**
   ```kotlin
   fun generate(prompt: String): Flow<String> = flow {
       val tokens = tokenizer.encode(prompt)
       runner.reset()
       runner.feedPrompt(tokens)
       while (true) {
           val token = runner.nextToken() ?: break
           emit(tokenizer.decode(token))
       }
   }
   ```
   - `ExecutorRunner.nextToken()`은 내부에서 ExecuTorch `Module.forward()`를 호출.
   - 토큰 스트리밍은 `Flow`, `LiveData`, `StateFlow` 중 선택 가능.

4. **UI 바인딩**
   - Compose: `LaunchedEffect(prompt)`로 `viewModel.generate()` 수집 → `LazyColumn`에 프롬프트와 응답을 표시.  
   - 연산은 장시간 블로킹이므로 `Dispatchers.IO` 사용.

### 2.4 성능·최적화
- **양자화**: ExecuTorch `to_edge` 시 `quant_config` 적용(Q8, Q4 등)으로 모델 크기와 추론 속도 향상.  
- **LoRA**: LLaMA 기반 모델은 LoRA 어댑터를 합쳐서 파라미터 효율을 높임. LlamaDemo에서는 LoRA 상태를 자산으로 로딩해 런타임에 Merge.  
- **KV 캐시**: 긴 문장 생성을 위해 KV 캐시 메모리를 재사용한다. `ExecutorRunner`가 내부적으로 관리.
- **멀티 스레드**: `Module.forward()` 호출 전 `Module.setNumThreads(n)`으로 CPU 병렬화 조정.

## 3. DailyDrug 프로젝트 적용 제안

1. **모듈 구성**
   - `app/src/main/assets/models/` 디렉터리 생성 후 `.pte`, 토크나이저 파일 배치.
   - `app/build.gradle.kts`에 ExecuTorch AAR 의존성 추가, `packagingOptions` 설정 업데이트.

2. **도메인 계층**
   - `presentation/alarm` 등 기존 구조를 참고해 `presentation/llm` 패키지 신설.  
   - `LocalLlmManager` 인터페이스를 정의해 `prompt → Flow<String>` 형태의 API 제공.

3. **데이터 계층**
   - `data/llm/ExecuTorchLocalLlm` 구현체로 Asset 로더, Tokenizer, Runner 관리.  
   - Hilt 모듈에서 싱글톤으로 주입 (`@InstallIn(SingletonComponent::class)`).

4. **프레젠테이션**
   - 사용 시나리오(예: 복약 가이드 생성)별 ViewModel에 `LocalLlmManager` 주입.  
   - Compose 화면에서 `collectAsStateWithLifecycle()`로 스트리밍 텍스트를 렌더.  
   - 긴 작업 진행 표시와 취소 버튼 제공.

5. **빌드/배포 체크리스트**
   - APK 용량: 모델 크기(수백 MB) → Play Store 100MB 제한 고려, 필요시 `app bundle` + 동적 다운로드 설계.  
   - 기기 호환성: ExecuTorch는 arm64-v8a, armeabi-v7a, x86_64 사전 컴파일 라이브러리를 제공. `android.defaultConfig.ndk.abiFilters`로 타깃 ABI 지정.
   - 메모리/성능: LLaMA 7B는 모바일 메모리 요구가 큼 → 4bit 양자화 + LoRA 사용, 프롬프트 길이 제한 UI 제공.

## 4. 문제 해결 팁

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `AssetFileDescriptor` EOF 에러 | 대형 모델 파일이 AAPT에 의해 압축됨 | `android.packagingOptions.resources.excludes += "models/**"` |
| `UnsatisfiedLinkError` | ABI 미스매치 | `ndk.abiFilters`에 `arm64-v8a`만 남기고 재빌드 |
| 느린 초기 로드 | 모델 복사 비용 | 첫 실행 시 `filesDir`로 복사 후 캐싱 |
| OutOfMemoryError | KV 캐시/배치 과다 | 토큰 길이 제한, `runner.setMaxCacheTokens` 설정 |
| 추론 지연 | 스레드 수 제한 | `module.setNumThreads(Runtime.getRuntime().availableProcessors())` |

## 5. 마무리

- ExecuTorch는 PyTorch 모델을 카메라·오프라인 환경에 맞춘 경량 러타임으로 실행한다.  
- LlamaDemo와 공식 문서를 따라가면 LLaMA 계열 Local LLM을 Android 앱에 빠르게 접목할 수 있다.  
- DailyDrug에서 복약 코칭, FAQ 생성 등 온디바이스 텍스트 기능을 구현하려면 위 절차에 따라 모델 자산 구성, 런타임 통합, UI 스트리밍 요소를 순차적으로 도입하면 된다.

