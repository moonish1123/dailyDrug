package com.dailydrug.photopicker.presentation.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydrug.photopicker.permission.PhotoPickerPermissions
import com.permissionmodule.domain.model.PermissionStatus
import com.permissionmodule.domain.usecase.CheckPermissionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing camera capture screen state and permission.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val checkPermissionUseCase: CheckPermissionUseCase,
    @ApplicationContext val context: Context
) : ViewModel() {

    private val _cameraPermissionState = MutableStateFlow<PermissionStatus>(PermissionStatus.Granted)
    val cameraPermissionState: StateFlow<PermissionStatus> = _cameraPermissionState.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    /**
     * Check camera permission status.
     */
    fun checkCameraPermission() {
        viewModelScope.launch {
            _cameraPermissionState.value = checkPermissionUseCase(PhotoPickerPermissions.CAMERA)
        }
    }

    /**
     * Handle camera permission result.
     */
    fun onPermissionResult(isGranted: Boolean) {
        _cameraPermissionState.value = if (isGranted) {
            PermissionStatus.Granted
        } else {
            PermissionStatus.Denied(canRequestAgain = true)
        }
    }

    /**
     * Handle camera error.
     */
    fun onCameraError(error: Throwable) {
        _error.value = error
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }
}
