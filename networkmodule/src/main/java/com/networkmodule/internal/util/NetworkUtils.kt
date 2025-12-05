package com.networkmodule.internal.util

import com.networkmodule.api.CacheConfig
import com.networkmodule.api.CertificatePinningConfig
import java.io.File
import okhttp3.Cache
import okhttp3.CertificatePinner

internal object NetworkUtils {

    fun buildCache(cacheDir: File, cacheConfig: CacheConfig?): Cache? {
        if (cacheConfig == null) return null
        val maxBytes = cacheConfig.maxSizeMb.toLong() * 1024L * 1024L
        if (maxBytes <= 0) return null
        val directory = File(cacheDir, "network_module_cache").apply { mkdirs() }
        return Cache(directory, maxBytes)
    }

    fun buildCertificatePinner(config: CertificatePinningConfig?): CertificatePinner? {
        if (config == null || config.pins.isEmpty()) return null
        return CertificatePinner.Builder().apply {
            config.pins.forEach { pin -> add(config.hostname, pin) }
        }.build()
    }
}
