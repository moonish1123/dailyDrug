package com.llmmodule.utils

import android.content.Context
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.lang.Runtime.getRuntime

class MemoryManagerTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var memoryManager: MemoryManager

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        memoryManager = MemoryManager(mockContext)
    }

    @Test
    fun `Should get available memory in bytes`() {
        val memory = memoryManager.getAvailableMemory()

        assertTrue(memory > 0, "Available memory should be positive")
        assertTrue(memory < getRuntime().maxMemory(), "Available memory should be less than max memory")
    }

    @Test
    fun `Should get memory usage percentage`() {
        val percentage = memoryManager.getMemoryUsagePercentage()

        assertTrue(percentage >= 0.0f, "Usage percentage should not be negative")
        assertTrue(percentage <= 1.0f, "Usage percentage should not exceed 1.0")
    }

    @Test
    fun `Should determine memory status correctly`() {
        val status = memoryManager.getMemoryStatus()

        assertNotNull(status, "Memory status should not be null")

        val availableMB = memoryManager.getAvailableMemory() / (1024 * 1024)
        when {
            availableMB <= 512 -> assertEquals(MemoryStatus.CRITICAL, status)
            availableMB <= 1024 -> assertEquals(MemoryStatus.WARNING, status)
            availableMB <= 2048 -> assertEquals(MemoryStatus.LOW, status)
            availableMB >= 3072 -> assertEquals(MemoryStatus.OPTIMAL, status)
            else -> assertEquals(MemoryStatus.ADEQUATE, status)
        }
    }

    @Test
    fun `Should check if model can be loaded`() {
        val canLoad = memoryManager.canLoadModel(2048L)

        val availableMB = memoryManager.getAvailableMemory() / (1024 * 1024)
        assertEquals(availableMB >= 2048, canLoad,
            "Should be able to load model if enough memory is available")
    }

    @Test
    fun `Should check if model should be unloaded`() {
        val shouldUnload = memoryManager.shouldUnloadModel()

        val status = memoryManager.getMemoryStatus()
        val expectedUnload = status == MemoryStatus.CRITICAL ||
                           (status == MemoryStatus.WARNING &&
                            memoryManager.getAvailableMemory() / (1024 * 1024) < 256)

        assertEquals(expectedUnload, shouldUnload,
            "Should unload model in critical memory conditions")
    }

    @Test
    fun `Should provide memory recommendations`() {
        val recommendations = memoryManager.getRecommendations()

        assertNotNull(recommendations, "Recommendations should not be null")
        assertTrue(recommendations.isNotEmpty(), "Should provide at least one recommendation")

        val allRecommendations = recommendations.joinToString(" ")
        assertTrue(allRecommendations.contains("✅") || allRecommendations.contains("⚠️") || allRecommendations.contains("ℹ️"),
            "Recommendations should include status indicators")
    }

    @Test
    fun `Should generate memory statistics`() {
        val stats = memoryManager.getMemoryStats()

        assertNotNull(stats, "Memory stats should not be null")
        assertTrue(stats.heapMaxMemory > 0, "Heap max memory should be positive")
        assertTrue(stats.heapTotalMemory > 0, "Heap total memory should be positive")
        assertTrue(stats.heapUsedMemory >= 0, "Heap used memory should be non-negative")
        assertTrue(stats.heapFreeMemory >= 0, "Heap free memory should be non-negative")
        assertTrue(stats.heapMaxMemory >= stats.heapTotalMemory,
            "Max memory should be >= total memory")
        assertTrue(stats.memoryUsagePercentage >= 0.0f, "Usage percentage should be non-negative")
        assertTrue(stats.memoryUsagePercentage <= 1.0f, "Usage percentage should not exceed 1.0")
    }

    @Test
    fun `Should calculate memory in MB correctly`() {
        val stats = memoryManager.getMemoryStats()

        assertTrue(stats.heapMaxMemoryMB > 0, "Heap max memory in MB should be positive")
        assertTrue(stats.heapUsedMemoryMB >= 0, "Heap used memory in MB should be non-negative")
        assertTrue(stats.heapFreeMemoryMB >= 0, "Heap free memory in MB should be non-negative")
        assertTrue(stats.memoryUsagePercent >= 0, "Memory usage percent should be non-negative")
        assertTrue(stats.memoryUsagePercent <= 100, "Memory usage percent should not exceed 100")
    }

    @Test
    fun `Should handle edge cases gracefully`() {
        // Test with very high memory requirement
        val canLoadHighRequirement = memoryManager.canLoadModel(Long.MAX_VALUE)
        assertFalse(canLoadHighRequirement, "Should not be able to load extremely large model")

        // Test with zero memory requirement
        val canLoadZeroRequirement = memoryManager.canLoadModel(0L)
        assertTrue(canLoadZeroRequirement, "Should be able to load zero-size model")

        // Test with negative memory requirement
        val canLoadNegativeRequirement = memoryManager.canLoadModel(-1L)
        assertFalse(canLoadNegativeRequirement, "Should not be able to load negative-size model")
    }

    @Test
    fun `Should provide consistent memory status and recommendations`() {
        val status = memoryManager.getMemoryStatus()
        val recommendations = memoryManager.getRecommendations()

        val recommendationsText = recommendations.joinToString(" ")

        when (status) {
            MemoryStatus.CRITICAL -> {
                assertTrue(recommendationsText.contains("⚠️ 메모리가 매우 부족합니다"))
                assertTrue(recommendationsText.contains("즉시 언로드해야"))
            }
            MemoryStatus.WARNING -> {
                assertTrue(recommendationsText.contains("⚠️ 메모리가 부족합니다"))
            }
            MemoryStatus.OPTIMAL -> {
                assertTrue(recommendationsText.contains("✅ 메모리 상태가 최적입니다"))
            }
            else -> {
                // Other statuses should still provide some recommendation
                assertTrue(recommendations.isNotEmpty())
            }
        }
    }
}