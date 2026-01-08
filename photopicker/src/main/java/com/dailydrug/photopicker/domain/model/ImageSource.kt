package com.dailydrug.photopicker.domain.model

/**
 * 이미지 소스를 나타내는 sealed class
 *
 * @property Camera 카메라로 촬영
 * @property Gallery 갤러리에서 선택
 */
sealed class ImageSource {
    data object Camera : ImageSource()
    data object Gallery : ImageSource()
}

/**
 * 이미지 선택 결과
 *
 * @property uri 선택된 이미지의 URI
 * @property success 성공 여부
 */
data class PhotoPickerResult(
    val uri: android.net.Uri?,
    val success: Boolean
)
