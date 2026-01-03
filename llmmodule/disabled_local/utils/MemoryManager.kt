package com.llmmodule.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced memory manager for LLM operations
 * Provides memory monitoring, optimization, and cleanup
 */
@Singleton
class MemoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val REQUIRED_MEMORY_MB = 2048L // 2GB minimum
        private const val OPTIMAL_MEMORY_MB = 3072L // 3GB optimal
        private const val WARNING_MEMORY_MB = 1024L // 1GB warning threshold
        private const val CRITICAL_MEMORY_MB = 512L // 512MB critical threshold
        private const val GC_INTERVAL_MS = 5000L // GC check interval
    }

    /**
     * Get available memory in bytes
     */
    fun getAvailableMemory(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                memoryInfo.availMem
            } else {
                // Fallback for older devices
                val runtime = Runtime.getRuntime()
                runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
            }
        } catch (e: Exception) {
            // Fallback to Java runtime method
            val runtime = Runtime.getRuntime()
            runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
        }
    }

    /**
     * Get total device memory in bytes
     */
    fun getTotalMemory(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.totalMem
        } catch (e: Exception) {
            // Fallback - cannot get total memory on older devices
            -1L
        }
    }

    /**
     * Get memory usage percentage (0.0 - 1.0)
     */
    fun getMemoryUsagePercentage(): Float {
        val totalMemory = getTotalMemory()
        if (totalMemory <= 0) return -1f

        val availableMemory = getAvailableMemory()
        val usedMemory = totalMemory - availableMemory

        return (usedMemory.toFloat() / totalMemory.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Get memory status
     */
    fun getMemoryStatus(): MemoryStatus {
        val availableMB = getAvailableMemory() / (1024 * 1024)
        val usagePercentage = getMemoryUsagePercentage()

        return when {
            availableMB <= CRITICAL_MEMORY_MB -> MemoryStatus.CRITICAL
            availableMB <= WARNING_MEMORY_MB -> MemoryStatus.WARNING
            availableMB <= REQUIRED_MEMORY_MB -> MemoryStatus.LOW
            availableMB >= OPTIMAL_MEMORY_MB -> MemoryStatus.OPTIMAL
            else -> MemoryStatus.ADEQUATE
        }
    }

    /**
     * Check if model should be unloaded due to memory pressure
     */
    fun shouldUnloadModel(availableMemory: Long? = null): Boolean {
        val available = availableMemory ?: getAvailableMemory()
        val availableMB = available / (1024 * 1024)

        return when (getMemoryStatus()) {
            MemoryStatus.CRITICAL -> true
            MemoryStatus.WARNING -> availableMB < WARNING_MEMORY_MB / 2
            MemoryStatus.LOW -> availableMB < CRITICAL_MEMORY_MB
            else -> false
        }
    }

    /**
     * Check if model can be loaded
     */
    fun canLoadModel(requiredMemoryMB: Long = REQUIRED_MEMORY_MB): Boolean {
        val availableMB = getAvailableMemory() / (1024 * 1024)
        return availableMB >= requiredMemoryMB && getMemoryStatus() != MemoryStatus.CRITICAL
    }

    /**
     * Optimize memory usage before LLM operations
     */
    suspend fun optimizeForLlm() = withContext(Dispatchers.IO) {
        try {
            // Force garbage collection
            System.gc()
            delay(100)

            // Additional memory cleanup
            Runtime.getRuntime().freeMemory()

            // Check if memory is still insufficient
            if (!canLoadModel()) {
                // Try more aggressive cleanup
                repeat(3) {
                    System.gc()
                    delay(100)
                }
            }

        } catch (e: Exception) {
            // Continue even if cleanup fails
        }
    }

    /**
     * Optimize memory usage during OCR operations
     */
    suspend fun optimizeForOcr() = withContext(Dispatchers.IO) {
        try {
            // Light cleanup for OCR
            System.gc()
            delay(50)
        } catch (e: Exception) {
            // Continue even if cleanup fails
        }
    }

    /**
     * Perform periodic memory maintenance
     */
    suspend fun performMaintenance() = withContext(Dispatchers.IO) {
        try {
            val memoryStatus = getMemoryStatus()

            when (memoryStatus) {
                MemoryStatus.CRITICAL -> {
                    // Aggressive cleanup
                    repeat(5) {
                        System.gc()
                        delay(200)
                    }
                }
                MemoryStatus.WARNING -> {
                    // Moderate cleanup
                    repeat(3) {
                        System.gc()
                        delay(100)
                    }
                }
                MemoryStatus.LOW -> {
                    // Light cleanup
                    System.gc()
                    delay(50)
                }
                else -> {
                    // No action needed
                }
            }

        } catch (e: Exception) {
            // Continue even if maintenance fails
        }
    }

    /**
     * Get memory optimization recommendations
     */
    fun getRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val status = getMemoryStatus()
        val availableMB = getAvailableMemory() / (1024 * 1024)

        when (status) {
            MemoryStatus.CRITICAL -> {
                recommendations.add("⚠️ 메모리가 매우 부족합니다. 모든 불필요한 앱을 종료하세요.")
                recommendations.add("⚠️ LLM 모델을 즉시 언로드해야 합니다.")
                recommendations.add("⚠️ 기기를 재시작하는 것이 좋습니다.")
            }
            MemoryStatus.WARNING -> {
                recommendations.add("⚠️ 메모리가 부족합니다. 백그라운드 앱을 정리하세요.")
                recommendations.add("⚠️ LLM 사용 중 다른 앱 사용을 피하세요.")
            }
            MemoryStatus.LOW -> {
                recommendations.add("ℹ️ 메모리가 제한적입니다. LLM 성능이 저하될 수 있습니다.")
                recommendations.add("ℹ️ 생성 토큰 수를 줄여보세요.")
            }
            MemoryStatus.ADEQUATE -> {
                recommendations.add("✅ 메모리 사용량이 적절합니다.")
                if (availableMB < OPTIMAL_MEMORY_MB) {
                    recommendations.add("ℹ️ 더 나은 성능을 위해 다른 앱을 종료하세요.")
                }
            }
            MemoryStatus.OPTIMAL -> {
                recommendations.add("✅ 메모리 상태가 최적입니다. 최고 성능을 발휘할 수 있습니다.")
            }
        }

        return recommendations
    }

    /**
     * Get memory statistics for debugging
     */
    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()

        val deviceTotalMemory = getTotalMemory()
        val availableMemory = getAvailableMemory()

        return MemoryStats(
            heapMaxMemory = maxMemory,
            heapTotalMemory = totalMemory,
            heapUsedMemory = usedMemory,
            heapFreeMemory = freeMemory,
            deviceTotalMemory = deviceTotalMemory,
            deviceAvailableMemory = availableMemory,
            memoryUsagePercentage = getMemoryUsagePercentage(),
            memoryStatus = getMemoryStatus()
        )
    }

    /**
     * Monitor memory usage and trigger cleanup when needed
     */
    suspend fun startMemoryMonitoring(): kotlinx.coroutines.flow.Flow<MemoryStatus> = kotlinx.coroutines.flow.flow {
        while (true) {
            val status = getMemoryStatus()
            emit(status)

            // Perform cleanup if memory is low
            if (status == MemoryStatus.WARNING || status == MemoryStatus.CRITICAL) {
                performMaintenance()
            }

            delay(GC_INTERVAL_MS)
        }
    }.flowOn(Dispatchers.IO)
}

/**
 * Memory status enum
 */
enum class MemoryStatus {
    CRITICAL,    // < 512MB available
    WARNING,     // < 1GB available
    LOW,         // < 2GB available
    ADEQUATE,    // 2-3GB available
    OPTIMAL      // > 3GB available
}

/**
 * Memory statistics data class
 */
data class MemoryStats(
    val heapMaxMemory: Long,        // Max heap size
    val heapTotalMemory: Long,      // Current heap allocation
    val heapUsedMemory: Long,       // Used heap memory
    val heapFreeMemory: Long,       // Free heap memory
    val deviceTotalMemory: Long,    // Total device memory (-1 if unknown)
    val deviceAvailableMemory: Long, // Available device memory
    val memoryUsagePercentage: Float, // Memory usage (0-1)
    val memoryStatus: MemoryStatus   // Current memory status
) {
    val heapMaxMemoryMB: Double get() = heapMaxMemory / (1024.0 * 1024.0)
    val heapUsedMemoryMB: Double get() = heapUsedMemory / (1024.0 * 1024.0)
    val heapFreeMemoryMB: Double get() = heapFreeMemory / (1024.0 * 1024.0)
    val deviceTotalMemoryMB: Double get() = deviceTotalMemory / (1024.0 * 1024.0)
    val deviceAvailableMemoryMB: Double get() = deviceAvailableMemory / (1024.0 * 1024.0)
    val memoryUsagePercent: Int get() = (memoryUsagePercentage * 100).toInt()
}