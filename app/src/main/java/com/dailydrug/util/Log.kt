package com.dailydrug.util

import android.util.Log as AndroidLog

/**
 * 간단한 Log 래퍼
 * import android.util.Log 대신 import com.dailydrug.util.Log로 사용
 */
object Log {
    const val VERBOSE = AndroidLog.VERBOSE
    const val DEBUG = AndroidLog.DEBUG
    const val INFO = AndroidLog.INFO
    const val WARN = AndroidLog.WARN
    const val ERROR = AndroidLog.ERROR
    const val ASSERT = AndroidLog.ASSERT

    fun v(tag: String, msg: String) = AndroidLog.v(tag, msg)
    fun v(tag: String, msg: String, tr: Throwable) = AndroidLog.v(tag, msg, tr)

    fun d(tag: String, msg: String) = AndroidLog.d(tag, msg)
    fun d(tag: String, msg: String, tr: Throwable) = AndroidLog.d(tag, msg, tr)

    fun i(tag: String, msg: String) = AndroidLog.i(tag, msg)
    fun i(tag: String, msg: String, tr: Throwable) = AndroidLog.i(tag, msg, tr)

    fun w(tag: String, msg: String) = AndroidLog.w(tag, msg)
    fun w(tag: String, msg: String, tr: Throwable) = AndroidLog.w(tag, msg, tr)
    fun w(tag: String, tr: Throwable) = AndroidLog.w(tag, tr)

    fun e(tag: String, msg: String) = AndroidLog.e(tag, msg)
    fun e(tag: String, msg: String, tr: Throwable) = AndroidLog.e(tag, msg, tr)

    fun wtf(tag: String, msg: String) = AndroidLog.wtf(tag, msg)
    fun wtf(tag: String, msg: String, tr: Throwable) = AndroidLog.wtf(tag, msg, tr)
    fun wtf(tag: String, tr: Throwable) = AndroidLog.wtf(tag, tr)
}

/**
 * 태그가 자동으로 생성되는 확장 함수
 * 사용법: logd("메시지"), loge("에러", exception)
 */
inline fun <reified T> T.logd(msg: String) = Log.d(T::class.java.simpleName, msg)
inline fun <reified T> T.logv(msg: String) = Log.v(T::class.java.simpleName, msg)
inline fun <reified T> T.logi(msg: String) = Log.i(T::class.java.simpleName, msg)
inline fun <reified T> T.logw(msg: String) = Log.w(T::class.java.simpleName, msg)
inline fun <reified T> T.loge(msg: String, tr: Throwable? = null) {
    if (tr != null) {
        Log.e(T::class.java.simpleName, msg, tr)
    } else {
        Log.e(T::class.java.simpleName, msg)
    }
}
