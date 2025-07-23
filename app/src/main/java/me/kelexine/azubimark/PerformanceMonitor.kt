package me.kelexine.azubimark

import android.content.Context
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

/**
 * Performance monitoring system to track app performance and identify bottlenecks
 */
object PerformanceMonitor {
    
    private const val TAG = "AzubiMark_Performance"
    private const val MAX_PERFORMANCE_LOGS = 100
    
    data class PerformanceMetric(
        val operation: String,
        val startTime: Long,
        val endTime: Long,
        val memoryBefore: Long,
        val memoryAfter: Long,
        val threadName: String,
        val additionalData: Map<String, Any> = emptyMap()
    ) {
        val duration: Long get() = endTime - startTime
        val memoryUsed: Long get() = memoryAfter - memoryBefore
        
        override fun toString(): String {
            return "[$operation] Duration: ${duration}ms, Memory: ${memoryUsed}KB, Thread: $threadName"
        }
    }
    
    private val performanceMetrics = ArrayList<PerformanceMetric>()
    private val activeOperations = ConcurrentHashMap<String, Long>()
    private val memorySnapshots = ConcurrentHashMap<String, Long>()
    
    /**
     * Start monitoring a performance operation
     */
    fun startOperation(operationName: String, additionalData: Map<String, Any> = emptyMap()) {
        if (!isPerformanceMonitoringEnabled()) return
        
        val startTime = SystemClock.elapsedRealtime()
        val memoryBefore = getCurrentMemoryUsage()
        val operationId = "${operationName}_${System.nanoTime()}"
        
        activeOperations[operationId] = startTime
        memorySnapshots[operationId] = memoryBefore
        
        Log.d(TAG, "Started operation: $operationName at ${getCurrentTimestamp()}")
    }
    
    /**
     * End monitoring a performance operation
     */
    fun endOperation(operationName: String, additionalData: Map<String, Any> = emptyMap()) {
        if (!isPerformanceMonitoringEnabled()) return
        
        val endTime = SystemClock.elapsedRealtime()
        val memoryAfter = getCurrentMemoryUsage()
        val threadName = Thread.currentThread().name
        
        // Find the matching operation
        val operationId = activeOperations.keys.find { it.startsWith(operationName) }
        
        if (operationId != null) {
            val startTime = activeOperations.remove(operationId) ?: endTime
            val memoryBefore = memorySnapshots.remove(operationId) ?: memoryAfter
            
            val metric = PerformanceMetric(
                operation = operationName,
                startTime = startTime,
                endTime = endTime,
                memoryBefore = memoryBefore,
                memoryAfter = memoryAfter,
                threadName = threadName,
                additionalData = additionalData
            )
            
            addMetric(metric)
            
            Log.d(TAG, "Completed operation: $metric")
            
            // Log warning for slow operations
            if (metric.duration > 1000) { // 1 second
                Log.w(TAG, "Slow operation detected: $operationName took ${metric.duration}ms")
            }
            
            // Log warning for high memory usage
            if (metric.memoryUsed > 10 * 1024) { // 10MB
                Log.w(TAG, "High memory usage detected: $operationName used ${metric.memoryUsed}KB")
            }
        } else {
            Log.w(TAG, "End operation called without matching start: $operationName")
        }
    }
    
    /**
     * Monitor a code block execution
     */
    inline fun <T> monitor(operationName: String, additionalData: Map<String, Any> = emptyMap(), operation: () -> T): T {
        startOperation(operationName, additionalData)
        try {
            return operation()
        } finally {
            endOperation(operationName, additionalData)
        }
    }
    
    /**
     * Monitor a suspend function execution
     */
    suspend inline fun <T> monitorSuspend(operationName: String, additionalData: Map<String, Any> = emptyMap(), noinline operation: suspend () -> T): T {
        startOperation(operationName, additionalData)
        try {
            return operation()
        } finally {
            endOperation(operationName, additionalData)
        }
    }
    
    /**
     * Add a performance metric
     */
    private fun addMetric(metric: PerformanceMetric) {
        synchronized(performanceMetrics) {
            performanceMetrics.add(metric)
            
            // Keep only the most recent metrics
            if (performanceMetrics.size > MAX_PERFORMANCE_LOGS) {
                performanceMetrics.removeAt(0)
            }
        }
    }
    
    /**
     * Get current memory usage in KB
     */
    private fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024
    }
    
    /**
     * Get current timestamp
     */
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }
    
    /**
     * Check if performance monitoring is enabled
     */
    private fun isPerformanceMonitoringEnabled(): Boolean {
        // Enable in debug builds or when debug mode is enabled in settings
        return BuildConfig.DEBUG || isDebugModeEnabled()
    }
    
    /**
     * Check if debug mode is enabled in app settings
     */
    private fun isDebugModeEnabled(): Boolean {
        return try {
            val context = AzubiMarkApplication.instance
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.getBoolean("debug_mode", false)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get performance statistics
     */
    fun getPerformanceStats(): Map<String, Any> {
        synchronized(performanceMetrics) {
            if (performanceMetrics.isEmpty()) {
                return mapOf("message" to "No performance data available")
            }
            
            val operations = performanceMetrics.groupBy { it.operation }
            val stats = mutableMapOf<String, Any>()
            
            // Overall stats
            stats["totalOperations"] = performanceMetrics.size
            stats["averageDuration"] = performanceMetrics.map { it.duration }.average()
            stats["totalMemoryUsed"] = performanceMetrics.sumOf { it.memoryUsed }
            
            // Per-operation stats
            val operationStats = operations.mapValues { (_, metrics) ->
                mapOf(
                    "count" to metrics.size,
                    "averageDuration" to metrics.map { it.duration }.average(),
                    "maxDuration" to (metrics.maxOfOrNull { it.duration } ?: 0),
                    "minDuration" to (metrics.minOfOrNull { it.duration } ?: 0),
                    "totalMemoryUsed" to metrics.sumOf { it.memoryUsed }
                )
            }
            
            stats["operationStats"] = operationStats
            
            // Recent slow operations
            val slowOperations = performanceMetrics
                .filter { it.duration > 500 } // 500ms threshold
                .sortedByDescending { it.duration }
                .take(5)
                .map { "${it.operation}: ${it.duration}ms" }
                
            if (slowOperations.isNotEmpty()) {
                stats["slowOperations"] = slowOperations
            }
            
            return stats
        }
    }
    
    /**
     * Get detailed performance report
     */
    fun getDetailedReport(): String {
        synchronized(performanceMetrics) {
            if (performanceMetrics.isEmpty()) {
                return "No performance data available"
            }
            
            val report = StringBuilder()
            report.appendLine("=== AzubiMark Performance Report ===")
            report.appendLine("Generated at: ${getCurrentTimestamp()}")
            report.appendLine()
            
            val stats = getPerformanceStats()
            report.appendLine("Overall Statistics:")
            report.appendLine("  Total Operations: ${stats["totalOperations"]}")
            report.appendLine("  Average Duration: ${"%.2f".format(stats["averageDuration"])}ms")
            report.appendLine("  Total Memory Used: ${stats["totalMemoryUsed"]}KB")
            report.appendLine()
            
            // Operation details
            val operationStats = stats["operationStats"] as? Map<String, Map<String, Any>>
            if (operationStats != null) {
                report.appendLine("Operation Details:")
                operationStats.forEach { (operation, opStats) ->
                    report.appendLine("  $operation:")
                    report.appendLine("    Count: ${opStats["count"]}")
                    report.appendLine("    Avg Duration: ${"%.2f".format(opStats["averageDuration"])}ms")
                    report.appendLine("    Max Duration: ${opStats["maxDuration"]}ms")
                    report.appendLine("    Min Duration: ${opStats["minDuration"]}ms")
                    report.appendLine("    Memory Used: ${opStats["totalMemoryUsed"]}KB")
                    report.appendLine()
                }
            }
            
            // Recent metrics
            report.appendLine("Recent Operations (last 10):")
            performanceMetrics.takeLast(10).forEach { metric ->
                report.appendLine("  $metric")
            }
            
            return report.toString()
        }
    }
    
    /**
     * Export performance data for analysis
     */
    fun exportPerformanceData(context: Context) {
        if (!isPerformanceMonitoringEnabled()) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val report = getDetailedReport()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "azubimark_performance_$timestamp.txt"
                
                val file = java.io.File(context.getExternalFilesDir(null), fileName)
                file.writeText(report)
                
                Log.i(TAG, "Performance report exported to: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export performance data", e)
            }
        }
    }
    
    /**
     * Clear performance data
     */
    fun clearPerformanceData() {
        synchronized(performanceMetrics) {
            performanceMetrics.clear()
            activeOperations.clear()
            memorySnapshots.clear()
        }
        Log.d(TAG, "Performance data cleared")
    }
    
    /**
     * Check for performance issues
     */
    fun checkPerformanceIssues(): List<String> {
        val issues = mutableListOf<String>()
        
        synchronized(performanceMetrics) {
            // Check for consistently slow operations
            val operations = performanceMetrics.groupBy { it.operation }
            operations.forEach { (operation, metrics) ->
                val avgDuration = metrics.map { it.duration }.average()
                if (avgDuration > 1000) {
                    issues.add("Operation '$operation' averages ${avgDuration.toInt()}ms (slow)")
                }
                
                val avgMemory = metrics.map { it.memoryUsed }.average()
                if (avgMemory > 5 * 1024) { // 5MB
                    issues.add("Operation '$operation' uses ${avgMemory.toInt()}KB memory on average (high)")
                }
            }
            
            // Check for memory leaks (increasing memory usage over time)
            if (performanceMetrics.size >= 10) {
                val recent = performanceMetrics.takeLast(5).map { it.memoryAfter }.average()
                val older = performanceMetrics.takeLast(10).take(5).map { it.memoryAfter }.average()
                
                if (recent > older * 1.5) {
                    issues.add("Potential memory leak detected (memory usage increasing)")
                }
            }
        }
        
        return issues
    }
    
    /**
     * Log performance summary
     */
    fun logPerformanceSummary() {
        if (!isPerformanceMonitoringEnabled()) return
        
        val stats = getPerformanceStats()
        Log.i(TAG, "Performance Summary: ${stats["totalOperations"]} operations, " +
                   "avg ${String.format("%.1f", stats["averageDuration"])}ms, " +
                   "${stats["totalMemoryUsed"]}KB memory")
        
        val issues = checkPerformanceIssues()
        if (issues.isNotEmpty()) {
            Log.w(TAG, "Performance issues detected:")
            issues.forEach { issue ->
                Log.w(TAG, "  - $issue")
            }
        }
    }
}
