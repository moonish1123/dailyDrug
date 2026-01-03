# DailyDrug 프로젝트 구현 TODO 목록

## 📋 프로젝트 현황 요약

**분석 기준**: Clean Architecture 준수, 모듈화 완성, 핵심 기능 구현 완료
**남은 작업**: OCR 모듈 ML Kit 구현, Local LLM ExecuTorch 연동, UI/UX 개선

---

## 🎯 Phase 1: OCR 모듈 완성 (최우선순위)

**예상 작업량**: 3-4일 | **Context 크기**: ~150k tokens
**목표**: ML Kit 기반 약봉지 텍스트 인식 및 약물 정보 추출

### 1.1 ML Kit Text Recognition 기본 구현
- [ ] **의존성 설정**
  ```kotlin
  // ocrmodule/build.gradle.kts에 추가
  implementation("com.google.mlkit:text-recognition-korean:16.0.0")
  implementation("androidx.camera:camera-camera2:1.3.1")
  implementation("androidx.camera:camera-lifecycle:1.3.1")
  implementation("androidx.camera:camera-view:1.3.1")
  ```

- [ ] **OcrDataSourceImpl 완성**
  ```kotlin
  // ocrmodule/src/main/java/com/dailydrug/ocr/data/datasource/OcrDataSourceImpl.kt
  class OcrDataSourceImpl @Inject constructor(
      @ApplicationContext private val context: Context
  ) : OcrDataSource {

      override suspend fun extractText(imageUri: Uri): Flow<String> = flow {
          val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

          try {
              val image = InputImage.fromFilePath(context, imageUri)
              recognizer.process(image)
                  .addOnSuccessListener { visionText ->
                      emit(visionText.text)
                  }
                  .addOnFailureListener { e ->
                      throw OcrException.TextExtractionFailed(e)
                  }
          } catch (e: Exception) {
              throw OcrException.InvalidImageUri(e)
          }
      }
  }
  ```

- [ ] **OCR 에러 핸들링**
  ```kotlin
  // ocrmodule/src/main/java/com/dailydrug/ocr/domain/model/OcrError.kt
  sealed class OcrException(message: String, cause: Throwable? = null) : Exception(message, cause) {
      class TextExtractionFailed(cause: Throwable) : OcrException("텍스트 추출 실패", cause)
      class InvalidImageUri(cause: Throwable) : OcrException("잘못된 이미지 URI", cause)
      class NoTextFound : OcrException("인식된 텍스트 없음")
      class NotDrugBag(text: String) : OcrException("약봉투가 아님: $text")
  }
  ```

### 1.2 약물 정보 파싱 및 추출
- [ ] **DrugInfo 모델 확장**
  ```kotlin
  // ocrmodule/src/main/java/com/dailydrug/ocr/domain/model/DrugInfo.kt
  data class DrugInfo(
      val drugName: String,                    // 약 이름
      val dosage: String,                      // 복용량 (예: "1정", "5ml")
      val scheduleInfo: ScheduleInfo,          // 복용 스케줄 정보
      val description: String = "",            // 약 설명
      val manufacturer: String = "",           // 제조사
      val extractedAt: LocalDateTime = LocalDateTime.now()
  )

  data class ScheduleInfo(
      val times: List<LocalTime>,              // 복용 시간들
      val pattern: String,                     // 복용 패턴 (예: "매일", "5일 복용 1일 휴식")
      val duration: String = "",               // 복용 기간
      val instructions: String = ""            // 복용 지침
  )
  ```

- [ ] **약물 정보 파서 구현**
  ```kotlin
  // ocrmodule/src/main/java/com/dailydrug/ocr/data/parser/DrugInfoParser.kt
  class DrugInfoParser @Inject constructor() {

      fun parseDrugText(extractedText: String): DrugInfo {
          // 1. 약 이름 추출 (한국어 약 이름 패턴)
          val drugName = extractDrugName(extractedText)

          // 2. 복용량 추출
          val dosage = extractDosage(extractedText)

          // 3. 복용 스케줄 추출
          val scheduleInfo = extractScheduleInfo(extractedText)

          // 4. 설명 추출
          val description = extractDescription(extractedText)

          return DrugInfo(
              drugName = drugName,
              dosage = dosage,
              scheduleInfo = scheduleInfo,
              description = description
          )
      }

      private fun extractDrugName(text: String): String {
          // 약 이름 패턴: "OO정", "OO캡슐", "OO시럽" 등
          val drugNamePattern = Regex("""([가-힣a-zA-Z]+(?:정|캡슐|시럽|액|과립|연고|크림))""")
          return drugNamePattern.find(text)?.value?.trim() ?: ""
      }

      private fun extractDosage(text: String): String {
          // 복용량 패턴: "1정", "5ml", "2알" 등
          val dosagePattern = Regex("""(\d+[정알mlmgg개])(?:\s*(?:씩|마다))?""")
          return dosagePattern.find(text)?.value?.trim() ?: ""
      }

      private fun extractScheduleInfo(text: String): ScheduleInfo {
          val times = extractTimes(text)
          val pattern = extractPattern(text)
          val duration = extractDuration(text)

          return ScheduleInfo(times, pattern, duration)
      }

      private fun extractTimes(text: String): List<LocalTime> {
          val timePattern = Regex("""(오전|오후)?\s*(\d{1,2})\s*시\s*(\d{1,2})\s*분""")

          return timePattern.findAll(text).map { match ->
              val period = match.groupValues[1]
              val hour = match.groupValues[2].toInt()
              val minute = match.groupValues[3].toInt()

              val adjustedHour = when (period) {
                  "오전" -> if (hour == 12) 0 else hour
                  "오후" -> if (hour == 12) 12 else hour + 12
                  else -> hour
              }

              LocalTime.of(adjustedHour, minute)
          }.toList()
      }

      private fun extractPattern(text: String): String {
          when {
              text.contains("매일") -> return "매일 복용"
              text.contains("격일") -> return "격일 복용"
              text.contains(Regex("""(\d+)일\s*복용\s*(\d+)일\s*휴식""")) -> {
                  val match = Regex("""(\d+)일\s*복용\s*(\d+)일\s*휴식""").find(text)
                  return "${match?.groupValues?.get(1)}일 복용 ${match?.groupValues?.get(2)}일 휴식"
              }
              else -> return "매일 복용"
          }
      }
  }
  ```

### 1.3 카메라 통합 및 권한 관리
- [ ] **AndroidManifest.xml 권한 추가**
  ```xml
  <!-- ocrmodule/src/main/AndroidManifest.xml -->
  <uses-permission android:name="android.permission.CAMERA" />
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
  <uses-feature android:name="android.hardware.camera" android:required="false" />
  ```

- [ ] **카메라 유틸리티**
  ```kotlin
  // ocrmodule/src/main/java/com/dailydrug/ocr/utils/CameraUtils.kt
  object CameraUtils {

      fun hasCameraPermission(context: Context): Boolean {
          return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                 == PackageManager.PERMISSION_GRANTED
      }

      fun getCameraPermissionLauncher(activity: Activity): ActivityResultLauncher<String> {
          return activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
              if (!granted) {
                  Toast.makeText(activity, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
              }
          }
      }

      fun createImageFile(context: Context): File {
          val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREAN).format(Date())
          val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "OCR")
          if (!storageDir.exists()) storageDir.mkdirs()

          return File(storageDir, "drug_bag_${timestamp}.jpg")
      }
  }
  ```

### 1.4 OCR Repository 완성
- [ ] **OcrRepositoryImpl 구현**
  ```kotlin
  // ocrmodule/src/main/java/com/dailydrug/ocr/data/repository/OcrRepositoryImpl.kt
  class OcrRepositoryImpl @Inject constructor(
      private val ocrDataSource: OcrDataSource,
      private val drugInfoParser: DrugInfoParser
  ) : OcrRepository {

      override suspend fun analyzeDrugBag(imageUri: Uri): Flow<DrugInfo> = flow {
          // 1. 텍스트 추출
          val extractedText = ocrDataSource.extractText(imageUri).first()
              .takeIf { it.isNotBlank() }
                  ?: throw OcrException.NoTextFound()

          // 2. 약봉투 여부 검증
          if (!isLikelyDrugBag(extractedText)) {
              throw OcrException.NotDrugBag(extractedText.take(100))
          }

          // 3. 약물 정보 파싱
          val drugInfo = drugInfoParser.parseDrugText(extractedText)
          emit(drugInfo)
      }

      private fun isLikelyDrugBag(text: String): Boolean {
          val drugKeywords = listOf("복용", "용법", "1일", "mg", "정", "캡슐", "시럽", "효능", "효과", "부작용")
          val lowercaseText = text.lowercase()

          return drugKeywords.any { keyword -> lowercaseText.contains(keyword) }
      }
  }
  ```

### 1.5 OCR 모듈 테스트
- [ ] **단위 테스트**
  ```kotlin
  // ocrmodule/src/test/java/com/dailydrug/ocr/data/parser/DrugInfoParserTest.kt
  @Test
  fun `약물 이름 정확히 추출`() {
      val parser = DrugInfoParser()
      val text = "타이레놀정 500mg - 1일 3회, 1회 1정씩 복용합니다"

      val drugInfo = parser.parseDrugText(text)

      assertEquals("타이레놀정", drugInfo.drugName)
      assertEquals("1정", drugInfo.dosage)
  }

  @Test
  fun `복용 시간 패턴 추출`() {
      val parser = DrugInfoParser()
      val text = "오전 8시, 오후 2시, 저녁 8시에 복용합니다"

      val drugInfo = parser.parseDrugText(text)

      assertEquals(3, drugInfo.scheduleInfo.times.size)
      assertEquals(LocalTime.of(8, 0), drugInfo.scheduleInfo.times[0])
      assertEquals(LocalTime.of(14, 0), drugInfo.scheduleInfo.times[1])
      assertEquals(LocalTime.of(20, 0), drugInfo.scheduleInfo.times[2])
  }
  ```

---

## 🧠 Phase 2: Local LLM ExecuTorch 연동 (중간순위)

**예상 작업량**: 5-7일 | **Context 크기**: ~180k tokens
**목표**: ExecuTorch 기반 오프라인 LLM 실행 환경 구축

### 2.1 ExecuTorch 기본 설정
- [ ] **의존성 설정**
  ```kotlin
  // llmmodule/build.gradle.kts에 추가
  implementation("org.pytorch:torch-android-lite:0.15.0")
  implementation("org.pytorch:executorch-android:0.3.0")
  implementation("org.pytorch:executorch-llm:0.3.0")
  ```

- [ ] **모델 애셋 준비**
  ```kotlin
  // llmmodule/src/main/assets/models/ 디렉터리 생성
  // 필요한 파일들:
  // - llama-7b-4bit-q8.pte (ExecuTorch 변환 모델)
  // - tokenizer.model (SentencePiece 토크나이저)
  // - adapter_config.json (LoRA 설정)
  ```

- [ ] **모델 애셋 매니저**
  ```kotlin
  // llmmodule/src/main/java/com/llmmodule/data/asset/ModelAssetManager.kt
  class ModelAssetManager @Inject constructor(
      @ApplicationContext private val context: Context
  ) {
      private val modelPath = "models/llama-7b-4bit-q8.pte"
      private val tokenizerPath = "models/tokenizer.model"

      fun getModelFile(): File {
          return copyAssetToInternalStorage(modelPath)
      }

      fun getTokenizerFile(): File {
          return copyAssetToInternalStorage(tokenizerPath)
      }

      private fun copyAssetToInternalStorage(assetPath: String): File {
          val assetManager = context.assets
          val fileName = File(assetPath).name
          val outFile = File(context.filesDir, fileName)

          if (!outFile.exists()) {
              assetManager.open(assetPath).use { input ->
                  FileOutputStream(outFile).use { output ->
                      input.copyTo(output)
                  }
              }
          }

          return outFile
      }
  }
  ```

### 2.2 ExecuTorch 러너 구현
- [ ] **ExecutorRunner 래퍼**
  ```kotlin
  // llmmodule/src/main/java/com/llmmodule/data/local/ExecutorRunner.kt
  class ExecutorRunner @Inject constructor(
      private val assetManager: ModelAssetManager
  ) {
      private var module: Module? = null
      private var tokenizer: Tokenizer? = null

      suspend fun initialize() = withContext(Dispatchers.IO) {
          try {
              val modelFile = assetManager.getModelFile()
              val tokenizerFile = assetManager.getTokenizerFile()

              module = Module.load(modelFile.absolutePath)
              tokenizer = Tokenizer.fromFile(tokenizerFile.absolutePath)

              // 스레드 수 설정
              module?.setNumThreads(Runtime.getRuntime().availableProcessors())

          } catch (e: Exception) {
              throw LocalLlmException.ModelLoadFailed(e)
          }
      }

      suspend fun generate(prompt: String, maxTokens: Int = 512): Flow<String> = flow {
          ensureInitialized()

          try {
              val tokens = tokenizer?.encode(prompt) ?: emptyList()
              reset()
              feedPrompt(tokens)

              repeat(maxTokens) { i ->
                  val token = nextToken() ?: break
                  val text = tokenizer?.decode(listOf(token)) ?: ""

                  if (text == "<|end_of_text|>") break
                  emit(text)

                  // UI 응답성을 위해 약간의 지연
                  delay(50)
              }

          } catch (e: Exception) {
              throw LocalLlmException.GenerationFailed(e)
          }
      }

      private fun reset() {
          // KV 캐시 초기화 등
          module?.forward(torch.zeros(intArrayOf(1, 1)))
      }

      private fun feedPrompt(tokens: List<Long>) {
          // 프롬프트 토큰들 입력
      }

      private fun nextToken(): Long? {
          // 다음 토큰 생성
          return null // 실제 구현 필요
      }

      private fun ensureInitialized() {
          if (module == null || tokenizer == null) {
              throw LocalLlmException.NotInitialized()
          }
      }
  }
  ```

- [ ] **토크나이저 래퍼**
  ```kotlin
  // llmmodule/src/main/java/com/llmmodule/data/local/Tokenizer.kt
  class Tokenizer private constructor(private val nativeHandle: Long) {

      companion object {
          external fun fromFile(modelPath: String): Tokenizer
          external fun encode(text: String, nativeHandle: Long): LongArray
          external fun decode(tokens: LongArray, nativeHandle: String): String
          external fun destroy(nativeHandle: Long)
      }

      fun encode(text: String): List<Long> {
          return encode(text, nativeHandle).toList()
      }

      fun decode(tokens: List<Long>): String {
          return decode(tokens.toLongArray(), nativeHandle)
      }

      fun destroy() {
          destroy(nativeHandle)
      }
  }
  ```

### 2.3 Local LLM 서비스 구현
- [ ] **LocalLlmService 완성**
  ```kotlin
  // llmmodule/src/main/java/com/llmmodule/data/provider/local/LocalLlmService.kt
  class LocalLlmService @Inject constructor(
      private val executorRunner: ExecutorRunner
  ) : LlmService {

      override suspend fun generateText(request: LlmRequest): Flow<String> = flow {
          try {
              // 약물 복용 코칭을 위한 프롬프트 구성
              val enhancedPrompt = buildMedicationPrompt(request.prompt)

              executorRunner.generate(enhancedPrompt, request.maxTokens)
                  .collect { token ->
                      emit(token)
                  }

          } catch (e: Exception) {
              throw LlmError.Provider("Local LLM generation failed", e)
          }
      }

      private fun buildMedicationPrompt(userInput: String): String {
          return """
          당신은 약물 복용 코칭 전문 AI입니다. 사용자의 질문에 친절하고 정확하게 답변해주세요.

          지침:
          - 의학적 조언은 제공하지 말고, 일반적인 정보만 제공하세요
          - 부작용이나 심각한 증상이 있다면 즉시 의사와 상담하라고 알려주세요
          - 정확하고 이해하기 쉬운 언어를 사용하세요
          - 한국어로 답변하세요

          사용자 질문: $userInput

          답변:
          """.trimIndent()
      }
  }
  ```

### 2.4 LLM 설정 관리
- [ ] **LlmConfiguration 확장**
  ```kotlin
  // llmmodule/src/main/java/com/llmmodule/domain/config/LlmConfiguration.kt
  data class LocalLlmConfig(
      val modelPath: String,
      val tokenizerPath: String,
      val maxTokens: Int = 512,
      val temperature: Float = 0.7f,
      val topK: Int = 40,
      val useLora: Boolean = false,
      val loraPath: String? = null
  )

  @Singleton
  class LlmConfigurationManager @Inject constructor(
      @ApplicationContext private val context: Context
  ) {
      private val prefs = context.getSharedPreferences("llm_config", Context.MODE_PRIVATE)

      fun getLocalLlmConfig(): LocalLlmConfig {
          return LocalLlmConfig(
              modelPath = prefs.getString("model_path", "models/llama-7b-4bit-q8.pte")!!,
              tokenizerPath = prefs.getString("tokenizer_path", "models/tokenizer.model")!!,
              maxTokens = prefs.getInt("max_tokens", 512),
              temperature = prefs.getFloat("temperature", 0.7f),
              topK = prefs.getInt("top_k", 40),
              useLora = prefs.getBoolean("use_lora", false),
              loraPath = prefs.getString("lora_path", null)
          )
      }

      fun saveLocalLlmConfig(config: LocalLlmConfig) {
          prefs.edit().apply {
              putString("model_path", config.modelPath)
              putString("tokenizer_path", config.tokenizerPath)
              putInt("max_tokens", config.maxTokens)
              putFloat("temperature", config.temperature)
              putInt("top_k", config.topK)
              putBoolean("use_lora", config.useLora)
              putString("lora_path", config.loraPath)
              apply()
          }
      }
  }
  ```

### 2.5 메모리 관리 및 최적화
- [ ] **메모리 관리**
  ```kotlin
  // llmmodule/src/main/java/com/llmmodule/utils/MemoryManager.kt
  class MemoryManager @Inject constructor() {

      fun getAvailableMemory(): Long {
          val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
          val memoryInfo = ActivityManager.MemoryInfo()
          activityManager.getMemoryInfo(memoryInfo)
          return memoryInfo.availMem
      }

      fun shouldUnloadModel(): Boolean {
          val availableMemory = getAvailableMemory()
          val requiredMemory = 2 * 1024 * 1024 * 1024L // 2GB 예상
          return availableMemory < requiredMemory
      }

      suspend fun optimizeMemoryUsage() {
          // 필요 시 모델 언로드 및 재로드
          if (shouldUnloadModel()) {
              delay(100) // 잠시 대기
              // 모델 언로드 로직
          }
      }
  }
  ```

---

## 🎨 Phase 3: UI/UX 통합 및 개선 (중간순위)

**예상 작업량**: 2-3일 | **Context 크기**: ~120k tokens
**목표**: OCR 및 LLM 기능 UI 연동, 사용자 경험 개선

### 3.1 OCR 카메라 화면
- [ ] **CameraCaptureScreen 구현**
  ```kotlin
  // app/src/main/java/com/dailydrug/presentation/ocr/CameraCaptureScreen.kt
  @Composable
  fun CameraCaptureScreen(
      onImageCaptured: (Uri) -> Unit,
      onDismiss: () -> Unit,
      viewModel: CameraViewModel = hiltViewModel()
  ) {
      val context = LocalContext.current
      val lifecycleOwner = LocalLifecycleOwner.current

      LaunchedEffect(Unit) {
          viewModel.checkCameraPermission(context)
      }

      Box(modifier = Modifier.fillMaxSize()) {
          CameraPreview(
              modifier = Modifier.fillMaxSize(),
              lifecycleOwner = lifecycleOwner,
              onImageCaptured = onImageCaptured
          )

          // 카메라 제어 버튼
          Row(
              modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .padding(16.dp),
              horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
              IconButton(
                  onClick = { viewModel.captureImage(context) }
              ) {
                  Icon(Icons.Default.Camera, contentDescription = "촬영")
              }

              IconButton(
                  onClick = onDismiss
              ) {
                  Icon(Icons.Default.Close, contentDescription = "닫기")
              }
          }

          // 권한 요청 다이얼로그
          if (viewModel.showPermissionDialog) {
              PermissionDialog(
                  onRequestPermission = { viewModel.requestCameraPermission(context) },
                  onDismiss = { viewModel.showPermissionDialog = false }
              )
          }
      }
  }
  ```

- [ ] **CameraViewModel**
  ```kotlin
  // app/src/main/java/com/dailydrug/presentation/ocr/CameraViewModel.kt
  @HiltViewModel
  class CameraViewModel @Inject constructor(
      private val analyzeDrugBagUseCase: AnalyzeDrugBagUseCase
  ) : ViewModel() {

      var showPermissionDialog by mutableStateOf(false)
          private set

      fun checkCameraPermission(context: Context) {
          if (!CameraUtils.hasCameraPermission(context)) {
              showPermissionDialog = true
          }
      }

      fun requestCameraPermission(activity: Activity) {
          val launcher = CameraUtils.getCameraPermissionLauncher(activity)
          launcher.launch(Manifest.permission.CAMERA)
      }

      fun captureImage(context: Context) {
          val imageFile = CameraUtils.createImageFile(context)
          val imageUri = FileProvider.getUriForFile(
              context,
              "${context.packageName}.fileprovider",
              imageFile
          )
          // 이미지 캡처 및 처리 로직
      }
  }
  ```

### 3.2 약물 정보 자동 입력 화면
- [ ] **OcrResultScreen**
  ```kotlin
  // app/src/main/java/com/dailydrug/presentation/ocr/OcrResultScreen.kt
  @Composable
  fun OcrResultScreen(
      drugInfo: DrugInfo,
      onConfirm: (CreateScheduleParams) -> Unit,
      onEdit: () -> Unit,
      onRetake: () -> Unit
  ) {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
          // 인식된 정보 표시
          Card(modifier = Modifier.fillMaxWidth()) {
              Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                  Text("약 이름", style = MaterialTheme.typography.labelLarge)
                  Text(drugInfo.drugName, style = MaterialTheme.typography.bodyLarge)

                  Text("복용량", style = MaterialTheme.typography.labelLarge)
                  Text(drugInfo.dosage, style = MaterialTheme.typography.bodyLarge)

                  Text("복용 시간", style = MaterialTheme.typography.labelLarge)
                  drugInfo.scheduleInfo.times.forEach { time ->
                      Text(time.toString(), style = MaterialTheme.typography.bodyMedium)
                  }
              }
          }

          // 약물 설명
          if (drugInfo.description.isNotBlank()) {
              Card(modifier = Modifier.fillMaxWidth()) {
                  Column(modifier = Modifier.padding(16.dp)) {
                      Text("설명", style = MaterialTheme.typography.labelLarge)
                      Text(drugInfo.description, style = MaterialTheme.typography.bodyMedium)
                  }
              }
          }

          // 버튼들
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
              OutlinedButton(
                  modifier = Modifier.weight(1f),
                  onClick = onRetake
              ) {
                  Text("재촬영")
              }

              OutlinedButton(
                  modifier = Modifier.weight(1f),
                  onClick = onEdit
              ) {
                  Text("수정")
              }

              Button(
                  modifier = Modifier.weight(1f),
                  onClick = {
                      val params = convertToScheduleParams(drugInfo)
                      onConfirm(params)
                  }
              ) {
                  Text("확인")
              }
          }
      }
  }

  private fun convertToScheduleParams(drugInfo: DrugInfo): CreateScheduleParams {
      return CreateScheduleParams(
          name = drugInfo.drugName,
          dosage = drugInfo.dosage,
          timeSlots = drugInfo.scheduleInfo.times,
          takeDays = extractTakeDays(drugInfo.scheduleInfo.pattern),
          restDays = extractRestDays(drugInfo.scheduleInfo.pattern),
          memo = drugInfo.description
      )
  }
  ```

### 3.3 LLM 채팅 화면
- [ ] **LlmChatScreen**
  ```kotlin
  // app/src/main/java/com/dailydrug/presentation/llm/LlmChatScreen.kt
  @Composable
  fun LlmChatScreen(
      viewModel: LlmChatViewModel = hiltViewModel()
  ) {
      val uiState by viewModel.uiState.collectAsState()
      val listState = rememberLazyListState()

      LaunchedEffect(uiState.messages) {
          if (uiState.messages.isNotEmpty()) {
              listState.animateScrollToItem(uiState.messages.size - 1)
          }
      }

      Column(modifier = Modifier.fillMaxSize()) {
          // 헤더
          TopAppBar(
              title = { Text("약물 복용 코칭") },
              navigationIcon = {
                  IconButton(onClick = { /* 뒤로 가기 */ }) {
                      Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                  }
              }
          )

          // 메시지 목록
          LazyColumn(
              modifier = Modifier.weight(1f),
              state = listState,
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
              items(uiState.messages) { message ->
                  MessageBubble(message = message)
              }

              if (uiState.isLoading) {
                  item {
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.Start
                      ) {
                          CircularProgressIndicator(modifier = Modifier.size(24.dp))
                          Spacer(modifier = Modifier.width(8.dp))
                          Text("답변 생성 중...")
                      }
                  }
              }
          }

          // 입력창
          MessageInput(
              value = uiState.currentInput,
              onValueChange = viewModel::updateInput,
              onSend = viewModel::sendMessage,
              isLoading = uiState.isLoading
          )
      }
  }

  @Composable
  private fun MessageBubble(message: LlmMessage) {
      val isUser = message.sender == LlmMessage.Sender.USER
      val alignment = if (isUser) Arrangement.End else Arrangement.Start
      val color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
      val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = alignment
      ) {
          Box(
              modifier = Modifier
                  .background(
                      color = color,
                      shape = RoundedCornerShape(16.dp)
                  )
                  .padding(12.dp)
                  .widthIn(max = 280.dp)
          ) {
              Text(
                  text = message.content,
                  color = textColor,
                  style = MaterialTheme.typography.bodyMedium
              )
          }
      }
  }
  ```

### 3.4 ScheduleInputScreen 개선
- [ ] **OCR 기능 연동**
  ```kotlin
  // app/src/main/java/com/dailydrug/presentation/schedule/ScheduleInputScreen.kt 개선

  // FAB에 OCR 기능 추가
  @Composable
  private fun ScheduleInputFloatingActions(
      onOcrCapture: () -> Unit,
      onManualInput: () -> Unit
  ) {
      Box(modifier = Modifier.fillMaxSize()) {
          // 기존 FAB
          FloatingActionButton(
              modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(16.dp),
              onClick = onManualInput
          ) {
              Icon(Icons.Default.Add, contentDescription = "수동 입력")
          }

          // OCR FAB
          FloatingActionButton(
              modifier = Modifier
                  .align(Alignment.BottomStart)
                  .padding(16.dp),
              containerColor = MaterialTheme.colorScheme.secondary,
              onClick = onOcrCapture
          ) {
              Icon(Icons.Default.CameraAlt, contentDescription = "카메라로 입력")
          }
      }
  }
  ```

---

## 🧪 Phase 4: 통합 테스트 및 최적화 (마지막 단계)

**예상 작업량**: 2-3일 | **Context 크기**: ~100k tokens
**목표**: End-to-End 테스트, 성능 최적화, 릴리즈 준비

### 4.1 통합 테스트
- [ ] **OCR to Schedule 통합 테스트**
  ```kotlin
  // app/src/test/java/com/dailydrug/integration/OcrToScheduleTest.kt
  @Test
  fun `OCR 인식에서 스케줄 생성까지 통합 테스트`() = runTest {
      // Given: 약봉지 이미지
      val imageUri = createTestDrugBagImageUri()

      // When: OCR 분석
      val drugInfo = analyzeDrugBagUseCase.invoke(imageUri).first()

      // And: 스케줄 생성
      val scheduleParams = CreateScheduleParams(
          name = drugInfo.drugName,
          dosage = drugInfo.dosage,
          timeSlots = drugInfo.scheduleInfo.times,
          takeDays = 7,
          restDays = 0
      )
      val scheduleId = createScheduleUseCase.invoke(scheduleParams)

      // Then: 스케줄이 정상적으로 생성됨
      assertThat(scheduleId).isGreaterThan(0)

      // And: 다음 날 복용 기록이 생성됨
      val nextDayRecords = getTodayMedicationsUseCase.invoke(LocalDate.now().plusDays(1))
      assertThat(nextDayRecords).isNotEmpty()
      assertThat(nextDayRecords.first().medicineName).isEqualTo(drugInfo.drugName)
  }
  ```

- [ ] **LLM 통합 테스트**
  ```kotlin
  // app/src/test/java/com/dailydrug/integration/LlmIntegrationTest.kt
  @Test
  fun `약물 복용 질문에 LLM 응답 테스트`() = runTest {
      // Given: 약물 복용 관련 질문
      val question = "타이레놀 복용 후 운전해도 될까요?"

      // When: LLM에 질문
      val response = generateTextUseCase.invoke(
          LlmRequest(
              prompt = question,
              provider = LlmProvider.LOCAL,
              maxTokens = 200
          )
      ).collectToList()

      // Then: 의미 있는 응답 반환
      val fullResponse = response.joinToString("")
      assertThat(fullResponse).isNotEmpty()
      assertThat(fullResponse).contains("운전")
      assertThat(fullResponse).contains("의사")
  }
  ```

### 4.2 성능 최적화
- [ ] **메모리 사용량 최적화**
  ```kotlin
  // app/src/main/java/com/dailydrug/utils/PerformanceOptimizer.kt
  class PerformanceOptimizer @Inject constructor(
      private val memoryManager: MemoryManager
  ) {

      suspend fun optimizeForOcr() {
          // OCR 실행 전 메모리 정리
          System.gc()
          delay(100)
      }

      suspend fun optimizeForLlm() {
          // LLM 실행 전 메모리 최적화
          if (memoryManager.shouldUnloadModel()) {
              memoryManager.optimizeMemoryUsage()
          }
      }

      fun measureExecutionTime(
          operation: String,
          block: suspend () -> Unit
      ): Long = measureTimeMillis {
          runBlocking { block() }
      }.also { elapsed ->
          Log.d("Performance", "$operation took ${elapsed}ms")
      }
  }
  ```

- [ ] **UI 렌더링 최적화**
  ```kotlin
  // LazyColumn 키 설정 및 최적화
  @Composable
  private fun MedicationList(
      medications: List<TodayMedication>,
      onRecordMedication: (Long) -> Unit
  ) {
      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
          items(
              items = medications,
              key = { medication -> medication.recordId }
          ) { medication ->
              // derivedStateOf로 불필요한 recomposition 방지
              val isOverdue by remember {
                  derivedStateOf {
                      LocalDateTime.now().isAfter(
                          LocalDateTime.of(medication.scheduledDate, medication.scheduledTime)
                              .plusMinutes(30)
                      )
                  }
              }

              MedicationItem(
                  medication = medication,
                  isOverdue = isOverdue,
                  onRecordMedication = onRecordMedication
              )
          }
      }
  }
  ```

### 4.3 에러 핸들링 개선
- [ ] **글로벌 에러 핸들러**
  ```kotlin
  // app/src/main/java/com/dailydrug/presentation/error/GlobalErrorHandler.kt
  @Singleton
  class GlobalErrorHandler @Inject constructor(
      private val notificationHelper: NotificationHelper
  ) {

      fun handleError(throwable: Throwable, context: Context) {
          when (throwable) {
              is OcrException.NoTextFound -> {
                  showToast(context, "인식된 텍스트가 없습니다. 다시 촬영해주세요.")
              }
              is OcrException.NotDrugBag -> {
                  showToast(context, "약봉투가 아닌 것 같습니다. 다시 촬영해주세요.")
              }
              is LocalLlmException.ModelLoadFailed -> {
                  showToast(context, "LLM 모델 로드에 실패했습니다. 앱을 재시작해주세요.")
              }
              is NetworkException -> {
                  showToast(context, "네트워크 연결을 확인해주세요.")
              }
              else -> {
                  showToast(context, "오류가 발생했습니다: ${throwable.message}")
                  Log.e("GlobalError", "Unhandled error", throwable)
              }
          }
      }
  }
  ```

### 4.4 배포 준비
- [ ] **ProGuard 규칙 추가**
  ```proguard
  # app/proguard-rules.pro
  # ExecuTorch
  -keep class org.pytorch.** { *; }
  -keep class com.facebook.jni.** { *; }

  # ML Kit
  -keep class com.google.mlkit.** { *; }

  # Room
  -keep class * extends androidx.room.RoomDatabase
  -dontwarn androidx.room.paging.**
  ```

- [ ] **APK 크기 최적화**
  ```kotlin
  // app/build.gradle.kts
  android {
      buildTypes {
          release {
              shrinkResources = true
              minifyEnabled = true
              proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

              // App Bundle 설정
              applicationVariants.all {
                  variant ->
                      variant.outputs.all {
                          outputFileName = "dailydrug-${variant.buildType.name}-${variant.versionName}.apk"
                      }
              }
          }
      }

      packagingOptions {
          resources {
              excludes += setOf(
                  "/META-INF/{AL2.0,LGPL2.1}",
                  "**/attach_hotspot_windows.dll",
                  "META-INF/DEPENDENCIES",
                  "META-INF/LICENSE",
                  "META-INF/LICENSE.txt",
                  "META-INF/NOTICE",
                  "META-INF/NOTICE.txt"
              )
          }
      }
  }
  ```

- [ ] **버전 관리**
  ```kotlin
  // app/build.gradle.kts
  android {
      defaultConfig {
          versionCode = 2
          versionName = "1.1.0"
      }
  }
  ```

---

## 📊 Phase 진행 순서 및 예상 기간

| Phase | 내용 | 예상 기간 | 의존 관계 |
|-------|------|-----------|-----------|
| **Phase 1** | OCR 모듈 완성 | 3-4일 | 없음 |
| **Phase 2** | Local LLM 연동 | 5-7일 | Phase 1 |
| **Phase 3** | UI/UX 통합 | 2-3일 | Phase 1, 2 |
| **Phase 4** | 통합 테스트 | 2-3일 | Phase 1, 2, 3 |
| **총계** | | **12-17일** | |

## 🔧 실행 방법

### Phase 시작 전 준비
```bash
# 1. 현재 상태 저장
git checkout -b phase-implementation
git add .
git commit -m "Start of implementation phases"

# 2. 필요한 의존성 설치
./gradlew clean build
```

### 각 Phase 실행
```bash
# Phase 1: OCR 모듈
./gradlew :ocrmodule:build
./gradlew testDebugUnitTest --tests "*ocr*"

# Phase 2: LLM 모듈
./gradlew :llmmodule:build
./gradlew testDebugUnitTest --tests "*llm*"

# Phase 3: 통합 빌드
./gradlew assembleDebug
./gradlew connectedAndroidTest

# Phase 4: 최종 테스트
./gradlew test
./gradlew assembleRelease
```

## 📋 체크리스트

### Phase 1 완료 기준
- [ ] ML Kit 텍스트 인식 동작
- [ ] 약물 정보 파싱 정확도 80% 이상
- [ ] 카메라 권한 및 캡처 기능
- [ ] OCR 결과 UI 표시
- [ ] 단위 테스트 통과

### Phase 2 완료 기준
- [ ] ExecuTorch 모델 로딩
- [ ] 텍스트 생성 속도 50 토큰/초 이상
- [ ] 메모리 사용량 2GB 이내
- [ ] LLM 응답 품질 검증
- [ ] 모델 언로드/재로드 기능

### Phase 3 완료 기준
- [ ] OCR → 스케줄 입력 자동 완성
- [ ] LLM 채팅 UI 반응성
- [ ] 오류 발생 시 사용자 친화적 메시지
- [ ] UI 테스트 통과

### Phase 4 완료 기준
- [ ] End-to-End 테스트 100% 통과
- [ ] APK 크기 150MB 이하 (Play Store 제한 고려)
- [ ] 크래시 리포트 0건 (24시간 테스트)
- [ ] 릴리즈 빌드 성공

이 TODO.md를 기준으로 각 Phase를 순차적으로 진행하여 DailyDrug 프로젝트를 완성할 수 있습니다.