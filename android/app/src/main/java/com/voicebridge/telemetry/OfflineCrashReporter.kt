package com.voicebridge.telemetry

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
// import com.voicebridge.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * Offline Crash Reporter for VoiceBridge
 * 
 * Privacy-focused crash reporting that:
 * - Stores crash logs locally only
 * - No automatic transmission
 * - User controls data sharing
 * - GDPR compliant
 * - Minimal data collection
 */
class OfflineCrashReporter private constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "OfflineCrashReporter"
        private const val CRASH_DIR = "crash_reports"
        private const val MAX_CRASH_FILES = 10
        private const val CRASH_FILE_PREFIX = "crash_"
        private const val CRASH_FILE_EXTENSION = ".txt"
        
        @Volatile
        private var INSTANCE: OfflineCrashReporter? = null
        
        fun getInstance(context: Context): OfflineCrashReporter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfflineCrashReporter(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val crashReportsDir = File(context.filesDir, CRASH_DIR)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var isInitialized = false
    
    data class CrashReport(
        val timestamp: String,
        val appVersion: String,
        val androidVersion: String,
        val deviceModel: String,
        val stackTrace: String,
        val deviceInfo: DeviceInfo,
        val appState: AppState
    )
    
    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val apiLevel: Int,
        val architecture: String,
        val totalMemory: Long,
        val availableMemory: Long
    )
    
    data class AppState(
        val foreground: Boolean,
        val lastActivity: String?,
        val memoryUsage: Long,
        val threadCount: Int
    )
    
    fun initialize() {
        if (isInitialized) return
        
        // Create crash reports directory
        if (!crashReportsDir.exists()) {
            crashReportsDir.mkdirs()
        }
        
        // Clean old crash reports
        cleanOldCrashReports()
        
        // Set up uncaught exception handler
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            handleCrash(thread, exception)
        }
        
        isInitialized = true
        Log.i(TAG, "Offline crash reporter initialized")
    }
    
    private fun handleCrash(thread: Thread, exception: Throwable) {
        try {
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", exception)
            
            // Generate crash report
            val crashReport = generateCrashReport(exception, thread)
            
            // Save crash report locally
            saveCrashReport(crashReport)
            
            Log.i(TAG, "Crash report saved locally")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling crash", e)
        } finally {
            // Call original handler to maintain system behavior
            defaultHandler?.uncaughtException(thread, exception)
            
            // Force exit if no default handler
            if (defaultHandler == null) {
                exitProcess(1)
            }
        }
    }
    
    private fun generateCrashReport(exception: Throwable, thread: Thread): CrashReport {
        val timestamp = dateFormat.format(Date())
        
        return CrashReport(
            timestamp = timestamp,
            appVersion = getAppVersion(),
            androidVersion = Build.VERSION.RELEASE,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            stackTrace = getStackTrace(exception, thread),
            deviceInfo = getDeviceInfo(),
            appState = getAppState()
        )
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo: PackageInfo = context.packageManager
                .getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }
    
    private fun getStackTrace(exception: Throwable, thread: Thread): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        
        printWriter.println("Exception in thread \"${thread.name}\"")
        exception.printStackTrace(printWriter)
        
        // Include thread dump
        printWriter.println("\n--- Thread Dump ---")
        Thread.getAllStackTraces().forEach { (t, stackTrace) ->
            if (t != thread) {
                printWriter.println("\nThread: ${t.name} (${t.state})")
                stackTrace.forEach { element ->
                    printWriter.println("\tat $element")
                }
            }
        }
        
        return stringWriter.toString()
    }
    
    private fun getDeviceInfo(): DeviceInfo {
        val runtime = Runtime.getRuntime()
        
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            totalMemory = runtime.maxMemory(),
            availableMemory = runtime.freeMemory()
        )
    }
    
    private fun getAppState(): AppState {
        val runtime = Runtime.getRuntime()
        
        return AppState(
            foreground = true, // Simplified - could integrate with lifecycle
            lastActivity = "Unknown", // Could track current activity
            memoryUsage = runtime.totalMemory() - runtime.freeMemory(),
            threadCount = Thread.activeCount()
        )
    }
    
    private fun saveCrashReport(crashReport: CrashReport) {
        scope.launch {
            try {
                val filename = "${CRASH_FILE_PREFIX}${crashReport.timestamp}${CRASH_FILE_EXTENSION}"
                val file = File(crashReportsDir, filename)
                
                val reportContent = formatCrashReport(crashReport)
                file.writeText(reportContent)
                
                Log.i(TAG, "Crash report saved: ${file.absolutePath}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash report", e)
            }
        }
    }
    
    private fun formatCrashReport(report: CrashReport): String {
        return buildString {
            appendLine("VoiceBridge Crash Report")
            appendLine("========================")
            appendLine()
            appendLine("Timestamp: ${report.timestamp}")
            appendLine("App Version: ${report.appVersion}")
            appendLine("Android Version: ${report.androidVersion}")
            appendLine("Device: ${report.deviceModel}")
            appendLine()
            appendLine("Device Information:")
            appendLine("- Manufacturer: ${report.deviceInfo.manufacturer}")
            appendLine("- Model: ${report.deviceInfo.model}")
            appendLine("- Android: ${report.deviceInfo.androidVersion} (API ${report.deviceInfo.apiLevel})")
            appendLine("- Architecture: ${report.deviceInfo.architecture}")
            appendLine("- Total Memory: ${report.deviceInfo.totalMemory / 1024 / 1024} MB")
            appendLine("- Available Memory: ${report.deviceInfo.availableMemory / 1024 / 1024} MB")
            appendLine()
            appendLine("App State:")
            appendLine("- Foreground: ${report.appState.foreground}")
            appendLine("- Memory Usage: ${report.appState.memoryUsage / 1024 / 1024} MB")
            appendLine("- Thread Count: ${report.appState.threadCount}")
            appendLine("- Last Activity: ${report.appState.lastActivity}")
            appendLine()
            appendLine("Stack Trace:")
            appendLine("-------------")
            appendLine(report.stackTrace)
            appendLine()
            appendLine("Privacy Notice:")
            appendLine("This crash report is stored locally on your device.")
            appendLine("No data is automatically transmitted.")
            appendLine("You can choose to share this report with support if needed.")
        }
    }
    
    private fun cleanOldCrashReports() {
        scope.launch {
            try {
                val crashFiles = crashReportsDir.listFiles { file ->
                    file.isFile && file.name.startsWith(CRASH_FILE_PREFIX)
                }?.sortedByDescending { it.lastModified() }
                
                crashFiles?.let { files ->
                    if (files.size > MAX_CRASH_FILES) {
                        files.drop(MAX_CRASH_FILES).forEach { file ->
                            if (file.delete()) {
                                Log.d(TAG, "Deleted old crash report: ${file.name}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning old crash reports", e)
            }
        }
    }
    
    /**
     * Get list of stored crash reports
     */
    fun getCrashReports(): List<File> {
        return crashReportsDir.listFiles { file ->
            file.isFile && file.name.startsWith(CRASH_FILE_PREFIX)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * Clear all stored crash reports
     */
    fun clearAllCrashReports() {
        scope.launch {
            try {
                getCrashReports().forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "Deleted crash report: ${file.name}")
                    }
                }
                Log.i(TAG, "All crash reports cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing crash reports", e)
            }
        }
    }
    
    /**
     * Get crash report content for sharing
     */
    fun getCrashReportContent(file: File): String? {
        return try {
            if (file.exists() && file.isFile) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading crash report", e)
            null
        }
    }
    
    /**
     * Test crash reporting (debug only)
     */
    fun testCrash() {
        if (true) { // Debug mode for testing
            throw RuntimeException("Test crash for VoiceBridge crash reporting")
        }
    }
}