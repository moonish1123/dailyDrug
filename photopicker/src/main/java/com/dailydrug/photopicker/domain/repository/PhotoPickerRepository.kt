package com.dailydrug.photopicker.domain.repository

import android.net.Uri
import com.dailydrug.photopicker.domain.model.ImageSource
import com.dailydrug.photopicker.domain.model.PhotoPickerResult

/**
 * 사진 선택 리포지토리 인터페이스
 */
interface PhotoPickerRepository {
    /**
     * 이미지 소스에서 이미지를 선택합니다
     *
     * @param source 이미지 소스 (Camera 또는 Gallery)
     * @return PhotoPickerResult 선택 결과 (URI 포함)
     */
    suspend fun pickImage(source: ImageSource): PhotoPickerResult
}
