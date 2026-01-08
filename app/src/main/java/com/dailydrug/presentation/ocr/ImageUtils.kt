package com.dailydrug.presentation.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 이미지 유틸리티 클래스
 */
object ImageUtils {

    /**
     * URI를 Bitmap으로 변환합니다
     *
     * @param context Context
     * @param uri 이미지 URI
     * @return Bitmap 또는 null (변환 실패 시)
     */
    suspend fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * URI를 Bitmap으로 변환합니다 (예외를 던지는 버전)
     *
     * @param context Context
     * @param uri 이미지 URI
     * @return Bitmap
     * @throws Exception 변환 실패 시
     */
    suspend fun uriToBitmapOrThrow(context: Context, uri: Uri): Bitmap = suspendCancellableCoroutine { continuation ->
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            continuation.resume(bitmap)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}
