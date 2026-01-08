package com.dailydrug.presentation.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

/**
 * OCR 텍스트 추출 다이얼로그
 */
@Composable
fun OcrTextDialog(
    onDismiss: () -> Unit,
    viewModel: OcrTestViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf<OcrTestUiState>(OcrTestUiState.Idle) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            uiState = OcrTestUiState.Processing
        } else {
            uiState = OcrTestUiState.Error("이미지 캡처에 실패했습니다")
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uiState = OcrTestUiState.ImageCaptured(uri)
        } else {
            uiState = OcrTestUiState.Error("이미지 선택에 실패했습니다")
        }
    }

    // Handle image captured
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is OcrTestUiState.ImageCaptured -> {
                val uri = (state as OcrTestUiState.ImageCaptured).uri
                try {
                    val bitmap = ImageUtils.uriToBitmapOrThrow(context, uri)
                    viewModel.extractTextFromBitmap(bitmap) { result ->
                        uiState = result
                    }
                } catch (e: Exception) {
                    uiState = OcrTestUiState.Error("이미지 처리 오류: ${e.message}")
                }
            }
            else -> {}
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
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = uiState) {
                    is OcrTestUiState.Idle -> {
                        IdleContent(
                            onCameraClick = {
                                // Create temp file for camera
                                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.KOREA).format(java.util.Date())
                                val storageDir = android.os.Environment.getExternalStoragePublicDirectory(
                                    android.os.Environment.DIRECTORY_PICTURES
                                )
                                val imageFile = java.io.File(storageDir, "IMG_$timeStamp.jpg")
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    imageFile
                                )
                                cameraLauncher.launch(uri)
                            },
                            onGalleryClick = {
                                galleryLauncher.launch("image/*")
                            }
                        )
                    }
                    is OcrTestUiState.Processing -> {
                        LoadingContent("텍스트를 추출하는 중...")
                    }
                    is OcrTestUiState.ImageCaptured -> {
                        // Handled by LaunchedEffect
                        LoadingContent("텍스트를 추출하는 중...")
                    }
                    is OcrTestUiState.Success -> {
                        val text = (state as OcrTestUiState.Success).extractedText
                        SuccessContent(
                            extractedText = text,
                            onCopy = {
                                scope.launch {
                                    copyToClipboard(context, text)
                                }
                            }
                        )
                    }
                    is OcrTestUiState.Error -> {
                        val message = (state as OcrTestUiState.Error).message
                        ErrorContent(
                            message = message,
                            onRetry = { uiState = OcrTestUiState.Idle }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "이미지를 선택하여 텍스트를 추출하세요",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
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
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
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

        Button(
            onClick = onCopy,
            modifier = Modifier.fillMaxWidth()
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

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
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
    data class ImageCaptured(val uri: Uri) : OcrTestUiState()
    data class Success(val extractedText: String) : OcrTestUiState()
    data class Error(val message: String) : OcrTestUiState()
}
