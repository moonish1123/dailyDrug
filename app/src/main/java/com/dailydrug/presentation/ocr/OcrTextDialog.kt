package com.dailydrug.presentation.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailydrug.ocr.domain.model.OcrLanguage
import com.dailydrug.permission.DailyDrugPermissions
import com.permissionmodule.domain.model.PermissionStatus
import com.permissionmodule.domain.usecase.CheckPermissionUseCase
import kotlinx.coroutines.launch

/**
 * OCR 텍스트 추출 다이얼로그
 *
 * UI 구성:
 * - 카메라 | 갤러리 버튼 (상단)
 * - 언어 선택 드롭다운
 * - 분석 | 취소 버튼
 * - 결과 표시 영역
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrTextDialog(
    onDismiss: () -> Unit,
    viewModel: OcrTestViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // UI 상태
    var uiState by remember { mutableStateOf<OcrTestUiState>(OcrTestUiState.Idle) }
    var selectedLanguage by remember { mutableStateOf(OcrLanguage.KOREAN) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var languageTextFieldSize by remember { mutableStateOf(Size.Zero) }

    // 초기 카메라 권한 상태 확인
    var cameraPermissionGranted by remember {
        mutableStateOf(false)
    }

    // 권한 상태 확인
    LaunchedEffect(Unit) {
        viewModel.checkPermissionUseCase(DailyDrugPermissions.CAMERA).let { status ->
            cameraPermissionGranted = status is PermissionStatus.Granted
        }
    }

    // 카메라 권한 요청 런처
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
    }

    // 카메라 런처
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // 권한 확인 후 URI 저장
            capturedImageUri?.let { uri ->
                uiState = OcrTestUiState.ImageSelected(uri)
            }
        } else {
            uiState = OcrTestUiState.Error("이미지 캡처에 실패했습니다")
        }
    }

    // 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uiState = OcrTestUiState.ImageSelected(uri)
        } else {
            uiState = OcrTestUiState.Error("이미지 선택에 실패했습니다")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OCR 텍스트 추출",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    TextButton(onClick = onDismiss) {
                        Text("닫기")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // 메인 컨텐츠
                when (val state = uiState) {
                    is OcrTestUiState.Idle, is OcrTestUiState.ImageSelected -> {
                        val hasImage = state is OcrTestUiState.ImageSelected || capturedImageUri != null
                        IdleContent(
                            selectedLanguage = selectedLanguage,
                            onLanguageChange = { selectedLanguage = it },
                            showLanguageMenu = showLanguageMenu,
                            onShowLanguageMenuChange = { showLanguageMenu = it },
                            languageTextFieldSize = languageTextFieldSize,
                            onLanguageTextFieldSizeChange = { languageTextFieldSize = it },
                            onCameraClick = {
                                if (cameraPermissionGranted) {
                                    // 권한이 있는 경우 카메라 실행
                                    val timeStamp = java.text.SimpleDateFormat(
                                        "yyyyMMdd_HHmmss",
                                        java.util.Locale.KOREA
                                    ).format(java.util.Date())
                                    val storageDir = android.os.Environment.getExternalStoragePublicDirectory(
                                        android.os.Environment.DIRECTORY_PICTURES
                                    )
                                    val imageFile = java.io.File(storageDir, "IMG_$timeStamp.jpg")
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        imageFile
                                    )
                                    capturedImageUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    // 권한이 없는 경우 권한 요청
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            onGalleryClick = {
                                galleryLauncher.launch("image/*")
                            },
                            onAnalyzeClick = {
                                capturedImageUri?.let { uri ->
                                    scope.launch {
                                        uiState = OcrTestUiState.Processing
                                        try {
                                            val bitmap = ImageUtils.uriToBitmapOrThrow(context, uri)
                                            viewModel.extractTextFromBitmap(bitmap, selectedLanguage) { result ->
                                                uiState = result
                                            }
                                        } catch (e: Exception) {
                                            uiState = OcrTestUiState.Error("이미지 처리 오류: ${e.message}")
                                        }
                                    }
                                }
                            },
                            hasImage = hasImage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    is OcrTestUiState.Processing -> {
                        LoadingContent("텍스트를 추출하는 중...", Modifier.weight(1f))
                    }
                    is OcrTestUiState.Success -> {
                        val text = (state as OcrTestUiState.Success).extractedText
                        SuccessContent(
                            extractedText = text,
                            onCopy = {
                                scope.launch {
                                    copyToClipboard(context, text)
                                }
                            },
                            onNewImage = {
                                uiState = OcrTestUiState.Idle
                                capturedImageUri = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    is OcrTestUiState.Error -> {
                        val message = (state as OcrTestUiState.Error).message
                        ErrorContent(
                            message = message,
                            onRetry = {
                                uiState = OcrTestUiState.Idle
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdleContent(
    selectedLanguage: OcrLanguage,
    onLanguageChange: (OcrLanguage) -> Unit,
    showLanguageMenu: Boolean,
    onShowLanguageMenuChange: (Boolean) -> Unit,
    languageTextFieldSize: Size,
    onLanguageTextFieldSizeChange: (Size) -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    hasImage: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // 안내 메시지
        Text(
            text = if (hasImage) "이미지가 선택되었습니다" else "이미지를 선택하여 텍스트를 추출하세요",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 카메라 | 갤러리 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = onCameraClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("카메라")
            }

            Button(
                onClick = onGalleryClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("갤러리")
            }
        }

        // 언어 선택 드롭다운
        ExposedDropdownMenuBox(
            expanded = showLanguageMenu,
            onExpandedChange = onShowLanguageMenuChange
        ) {
            OutlinedTextField(
                value = selectedLanguage.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("언어 선택") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = "언어 선택",
                        modifier = Modifier.clickable {
                            onShowLanguageMenuChange(!showLanguageMenu)
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .onGloballyPositioned { coordinates ->
                        onLanguageTextFieldSizeChange(coordinates.size.toSize())
                    }
            )

            ExposedDropdownMenu(
                expanded = showLanguageMenu,
                onDismissRequest = { onShowLanguageMenuChange(false) },
                modifier = Modifier.exposedDropdownSize(true)
            ) {
                OcrLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.displayName) },
                        onClick = {
                            onLanguageChange(language)
                            onShowLanguageMenuChange(false)
                        }
                    )
                }
            }
        }

        // 분석 | 취소 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = onAnalyzeClick,
                enabled = hasImage,
                modifier = Modifier.weight(1f)
            ) {
                Text("분석")
            }

            OutlinedButton(
                onClick = {},
                modifier = Modifier.weight(1f)
            ) {
                Text("취소")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun LoadingContent(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessContent(
    extractedText: String,
    onCopy: () -> Unit,
    onNewImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "추출된 텍스트",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    Text(
                        text = extractedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNewImage,
                modifier = Modifier.weight(1f)
            ) {
                Text("새 이미지")
            }

            Button(
                onClick = onCopy,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("복사하기")
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onRetry) {
            Text("다시 시도")
        }
    }
}

/**
 * 클립보드에 텍스트 복사
 */
private suspend fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("OCR Text", text)
    clipboard.setPrimaryClip(clip)
}

/**
 * OCR 테스트 UI 상태
 */
sealed class OcrTestUiState {
    data object Idle : OcrTestUiState()
    data object Processing : OcrTestUiState()
    data class ImageSelected(val uri: Uri) : OcrTestUiState()
    data class Success(val extractedText: String) : OcrTestUiState()
    data class Error(val message: String) : OcrTestUiState()
}
