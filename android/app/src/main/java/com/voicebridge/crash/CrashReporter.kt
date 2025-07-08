package com.voicebridge.crash

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.security.MessageDigest

/**
 * Privacy-focused offline crash reporting system
 * Stores crash reports locally without transmitting any data
 */
class CrashReporter private constructor(private val context: Context) {
    companion object {
        private const val TAG = "CrashReporter"
        private const val PREFS_NAME = "voicebridge_crashes"
        private const val CRASH_DIR = "crash_reports"
        private const val MAX_CRASH_FILES = 20
        
        @Volatile
        private var INSTANCE: CrashReporter? = null
        
        fun getInstance(context: Context): CrashReporter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CrashReporter(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        fun initialize(context: Context) {
            val reporter = getInstance(context)
            Thread.setDefaultUncaughtExceptionHandler(reporter.crashHandler)
        }
    }
    
    @Serializable
    data class CrashReport(
        val timestamp: String,
        val appVersion: String,
        val androidVersion: String,
        val deviceModel: String,
        val crashId: String,
        val exceptionType: String,
        val exceptionMessage: String,
        val stackTrace: String,
        val threadName: String,
        val availableMemory: Long,
        val totalMemory: Long,
        val freeStorage: Long
    )
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val crashDir = File(context.filesDir, CRASH_DIR)
    private val json = Json { prettyPrint = true }
    
    init {
        // Ensure crash directory exists
        if (!crashDir.exists()) {
            crashDir.mkdirs()
        }
        
        // Clean up old crash files
        cleanupOldCrashes()
    }
    
    private val crashHandler = Thread.UncaughtExceptionHandler { thread, exception ->
        try {
            reportCrash(exception, thread)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report crash", e)
        }
        
        // Call the default handler
        Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, exception)
    }
    
    private fun reportCrash(exception: Throwable, thread: Thread) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timestamp = Clock.System.now()
                val crashId = generateCrashId(exception, timestamp)
                
                val crashReport = CrashReport(
                    timestamp = timestamp.toString(),
                    appVersion = getAppVersion(),
                    androidVersion = Build.VERSION.RELEASE,
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    crashId = crashId,
                    exceptionType = exception.javaClass.simpleName,
                    exceptionMessage = exception.message ?: "No message",
                    stackTrace = getStackTrace(exception),
                    threadName = thread.name,
                    availableMemory = getAvailableMemory(),
                    totalMemory = getTotalMemory(),
                    freeStorage = getFreeStorage()
                )
                
                saveCrashReport(crashReport)
                updateCrashStats()
                
                Log.i(TAG, "Crash report saved: $crashId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate crash report", e)
            }
        }
    }
    
    private fun generateCrashId(exception: Throwable, timestamp: Instant): String {
        val input = "${exception.javaClass.name}${exception.message}${timestamp}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }
    
    private fun getStackTrace(exception: Throwable): String {
        val stringWriter = StringWriter()
        exception.printStackTrace(PrintWriter(stringWriter))
        return stringWriter.toString()
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getAvailableMemory(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
        } catch (e: Exception) {
            -1L
        }
    }
    
    private fun getTotalMemory(): Long {
        return try {
            Runtime.getRuntime().maxMemory()
        } catch (e: Exception) {
            -1L
        }
    }
    
    private fun getFreeStorage(): Long {
        return try {
            context.filesDir.freeSpace
        } catch (e: Exception) {
            -1L
        }
    }
    
    private fun saveCrashReport(report: CrashReport) {
        val filename = "crash_${report.crashId}.json"
        val file = File(crashDir, filename)
        
        try {
            val jsonString = json.encodeToString(report)
            file.writeText(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash report to file", e)
        }
    }
    
    private fun updateCrashStats() {
        val crashCount = prefs.getInt("crash_count", 0) + 1
        val lastCrashTime = Clock.System.now().toString()
        
        prefs.edit()
            .putInt("crash_count", crashCount)
            .putString("last_crash", lastCrashTime)
            .apply()
    }
    
    private fun cleanupOldCrashes() {
        try {
            val crashFiles = crashDir.listFiles()?.sortedByDescending { it.lastModified() }
            if (crashFiles != null && crashFiles.size > MAX_CRASH_FILES) {
                crashFiles.drop(MAX_CRASH_FILES).forEach { file ->
                    try {
                        file.delete()
                        Log.d(TAG, "Deleted old crash file: ${file.name}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete crash file: ${file.name}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old crashes", e)
        }
    }
    
    /**
     * Get crash statistics for debugging/analytics
     */
    fun getCrashStats(): CrashStats {
        val crashCount = prefs.getInt("crash_count", 0)
        val lastCrash = prefs.getString("last_crash", null)
        val reportCount = crashDir.listFiles()?.size ?: 0
        
        return CrashStats(crashCount, lastCrash, reportCount)
    }
    
    /**
     * Get all crash reports for debugging
     */
    fun getAllCrashReports(): List<CrashReport> {
        val reports = mutableListOf<CrashReport>()
        
        try {
            crashDir.listFiles()?.forEach { file ->
                if (file.extension == "json") {
                    try {
                        val jsonString = file.readText()
                        val report = json.decodeFromString<CrashReport>(jsonString)
                        reports.add(report)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse crash report: ${file.name}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read crash reports", e)
        }
        
        return reports.sortedByDescending { it.timestamp }
    }
    
    /**
     * Clear all crash reports
     */
    fun clearCrashReports() {
        try {
            crashDir.listFiles()?.forEach { file ->
                file.delete()
            }
            prefs.edit().clear().apply()
            Log.i(TAG, "All crash reports cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash reports", e)
        }
    }
    
    data class CrashStats(
        val totalCrashes: Int,
        val lastCrashTime: String?,
        val reportCount: Int
    )
}