package com.dailydrug.photopicker.presentation.camera

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.permissionmodule.domain.model.PermissionStatus

/**
 * Full-screen camera capture screen with gallery button and capture button.
 *
 * @param onBack Callback when back button is pressed
 * @param onGallerySelected Callback when an image is selected from gallery
 * @param onImageCaptured Callback when an image is captured via camera
 * @param viewModel CameraViewModel for managing camera state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCaptureScreen(
    onBack: () -> Unit,
    onGallerySelected: (Uri) -> Unit,
    onImageCaptured: (android.net.Uri) -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraPermissionState by viewModel.cameraPermissionState.collectAsStateWithLifecycle()
    var showPermissionRequest by remember { mutableStateOf(false) }
    var captureTrigger by remember { mutableIntStateOf(0) }

    // Gallery launcher for picking images
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onGallerySelected(it) }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    // Check camera permission on launch
    LaunchedEffect(Unit) {
        viewModel.checkCameraPermission()
    }

    // Handle permission state
    LaunchedEffect(cameraPermissionState) {
        when (cameraPermissionState) {
            is PermissionStatus.Denied -> {
                showPermissionRequest = true
            }
            else -> {
                showPermissionRequest = false
            }
        }
    }

    // Request permission if needed
    LaunchedEffect(showPermissionRequest) {
        if (showPermissionRequest) {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = "갤러리"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues)
        ) {
            // Camera preview
            CameraPreview(
                captureTrigger = captureTrigger,
                onImageCaptured = { file ->
                    // Convert file to URI and pass to callback
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        viewModel.context,
                        "${viewModel.context.packageName}.fileprovider",
                        file
                    )
                    onImageCaptured(uri)
                },
                onError = { error ->
                    viewModel.onCameraError(error)
                }
            )

            // Capture button overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = { captureTrigger++ },
                    modifier = Modifier.size(72.dp),
                    containerColor = Color.White
                ) {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp)
                        ) {
                            // Inner circle for capture button visual
                        }
                    }
                }
            }
        }
    }
}
