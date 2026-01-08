package com.dailydrug.photopicker.data.repository

import android.content.Context
import android.net.Uri
import com.dailydrug.photopicker.domain.model.ImageSource
import com.dailydrug.photopicker.domain.model.PhotoPickerResult
import com.dailydrug.photopicker.domain.repository.PhotoPickerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사진 선택 리포지토리 구현
 */
@Singleton
class PhotoPickerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PhotoPickerRepository {

    private val _cameraResult = MutableStateFlow<Uri?>(null)
    private val _galleryResult = MutableStateFlow<Uri?>(null)

    val cameraResult: StateFlow<Uri?> = _cameraResult.asStateFlow()
    val galleryResult: StateFlow<Uri?> = _galleryResult.asStateFlow()

    /**
     * 카메라 캡처 결과를 설정합니다 (Compose에서 호출)
     */
    fun setCameraResult(uri: Uri?) {
        _cameraResult.value = uri
    }

    /**
     * 갤러리 선택 결과를 설정합니다 (Compose에서 호출)
     */
    fun setGalleryResult(uri: Uri?) {
        _galleryResult.value = uri
    }

    override suspend fun pickImage(source: ImageSource): PhotoPickerResult {
        return when (source) {
            is ImageSource.Camera -> {
                val uri = _cameraResult.value
                PhotoPickerResult(uri = uri, success = uri != null)
            }
            is ImageSource.Gallery -> {
                val uri = _galleryResult.value
                PhotoPickerResult(uri = uri, success = uri != null)
            }
        }
    }
}
