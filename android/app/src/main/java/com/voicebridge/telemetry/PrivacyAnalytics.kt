package com.voicebridge.telemetry

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Privacy-Preserving Analytics for VoiceBridge
 * 
 * Features:
 * - All data stored locally
 * - No automatic transmission
 * - User controls all data sharing
 * - Anonymized metrics only
 * - GDPR compliant
 * - No user identification
 */
class PrivacyAnalytics private constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "PrivacyAnalytics"
        private const val ANALYTICS_FILE = "analytics.json"
        private const val PREFS_NAME = "voicebridge_analytics"
        private const val PREF_ANALYTICS_ENABLED = "analytics_enabled"
        private const val PREF_FIRST_LAUNCH = "first_launch_time"
        private const val MAX_EVENTS_PER_SESSION = 1000
        
        @Volatile
        private var INSTANCE: PrivacyAnalytics? = null
        
        fun getInstance(context: Context): PrivacyAnalytics {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PrivacyAnalytics(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val analyticsFile = File(context.filesDir, ANALYTICS_FILE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Session tracking
    private var sessionStartTime: Long = 0
    private var sessionId: String = ""
    private val sessionEvents = mutableListOf<AnalyticsEvent>()
    
    data class AnalyticsEvent(
        val type: String,
        val category: String,
        val action: String,
        val label: String? = null,
        val value: Int? = null,
        val timestamp: String,
        val sessionId: String,
        val properties: Map<String, Any> = emptyMap()
    )
    
    data class SessionSummary(
        val sessionId: String,
        val startTime: String,
        val duration: Long,
        val eventCount: Int,
        val features: Set<String>
    )
    
    fun initialize() {
        // Set first launch time if not set
        if (!prefs.contains(PREF_FIRST_LAUNCH)) {
            prefs.edit().putLong(PREF_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
        
        // Start new session
        startSession()
        
        Log.i(TAG, "Privacy analytics initialized (enabled: ${isAnalyticsEnabled()})")
    }
    
    private fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        sessionId = UUID.randomUUID().toString().substring(0, 8) // Short session ID
        sessionEvents.clear()
        
        if (isAnalyticsEnabled()) {
            trackEvent(
                category = "app",
                action = "session_start",
                properties = mapOf(
                    "app_version" to getAppVersion(),
                    "android_version" to android.os.Build.VERSION.RELEASE
                )
            )
        }
    }
    
    fun endSession() {
        if (!isAnalyticsEnabled()) return
        
        val duration = System.currentTimeMillis() - sessionStartTime
        
        trackEvent(
            category = "app",
            action = "session_end",
            value = (duration / 1000).toInt(), // Duration in seconds
            properties = mapOf(
                "session_duration" to duration,
                "events_count" to sessionEvents.size
            )
        )
        
        // Save session data
        saveSessionData()
    }
    
    /**
     * Track feature usage
     */
    fun trackFeatureUsage(feature: String, action: String, properties: Map<String, Any> = emptyMap()) {
        if (!isAnalyticsEnabled()) return
        
        trackEvent(
            category = "feature",
            action = action,
            label = feature,
            properties = properties
        )
    }
    
    /**
     * Track voice recognition events
     */
    fun trackVoiceEvent(action: String, success: Boolean, duration: Long? = null) {
        if (!isAnalyticsEnabled()) return
        
        val properties = mutableMapOf<String, Any>(
            "success" to success
        )
        duration?.let { properties["duration_ms"] = it }
        
        trackEvent(
            category = "voice",
            action = action,
            value = if (success) 1 else 0,
            properties = properties
        )
    }
    
    /**
     * Track OCR events
     */
    fun trackOCREvent(action: String, success: Boolean, confidence: Float? = null) {
        if (!isAnalyticsEnabled()) return
        
        val properties = mutableMapOf<String, Any>(
            "success" to success
        )
        confidence?.let { properties["confidence"] = it }
        
        trackEvent(
            category = "ocr",
            action = action,
            value = if (success) 1 else 0,
            properties = properties
        )
    }
    
    /**
     * Track form filling events
     */
    fun trackFormEvent(action: String, fieldCount: Int? = null, success: Boolean? = null) {
        if (!isAnalyticsEnabled()) return
        
        val properties = mutableMapOf<String, Any>()
        fieldCount?.let { properties["field_count"] = it }
        success?.let { properties["success"] = it }
        
        trackEvent(
            category = "form",
            action = action,
            value = fieldCount,
            properties = properties
        )
    }
    
    /**
     * Track accessibility events
     */
    fun trackAccessibilityEvent(action: String, feature: String) {
        if (!isAnalyticsEnabled()) return
        
        trackEvent(
            category = "accessibility",
            action = action,
            label = feature
        )
    }
    
    /**
     * Track performance metrics
     */
    fun trackPerformance(operation: String, duration: Long, success: Boolean) {
        if (!isAnalyticsEnabled()) return
        
        trackEvent(
            category = "performance",
            action = operation,
            value = duration.toInt(),
            properties = mapOf(
                "duration_ms" to duration,
                "success" to success
            )
        )
    }
    
    /**
     * Track errors (anonymized)
     */
    fun trackError(category: String, error: String, fatal: Boolean = false) {
        if (!isAnalyticsEnabled()) return
        
        // Anonymize error message - only keep error type
        val errorType = error.split(":").firstOrNull()?.trim() ?: "unknown"
        
        trackEvent(
            category = "error",
            action = category,
            label = errorType,
            value = if (fatal) 1 else 0,
            properties = mapOf(
                "fatal" to fatal,
                "error_type" to errorType
            )
        )
    }
    
    private fun trackEvent(
        category: String,
        action: String,
        label: String? = null,
        value: Int? = null,
        properties: Map<String, Any> = emptyMap()
    ) {
        if (!isAnalyticsEnabled() || sessionEvents.size >= MAX_EVENTS_PER_SESSION) return
        
        val event = AnalyticsEvent(
            type = "event",
            category = category,
            action = action,
            label = label,
            value = value,
            timestamp = dateFormat.format(Date()),
            sessionId = sessionId,
            properties = properties
        )
        
        sessionEvents.add(event)
        Log.d(TAG, "Tracked event: $category/$action")
    }
    
    private fun saveSessionData() {
        scope.launch {
            try {
                val sessionSummary = SessionSummary(
                    sessionId = sessionId,
                    startTime = dateFormat.format(Date(sessionStartTime)),
                    duration = System.currentTimeMillis() - sessionStartTime,
                    eventCount = sessionEvents.size,
                    features = sessionEvents.map { it.category }.toSet()
                )
                
                // Load existing data
                val existingData = loadAnalyticsData()
                
                // Add new session
                existingData.put("last_session", sessionToJson(sessionSummary))
                
                // Add to sessions array
                val sessions = existingData.optJSONArray("sessions") ?: JSONArray()
                sessions.put(sessionToJson(sessionSummary))
                
                // Keep only last 30 sessions
                if (sessions.length() > 30) {
                    val newSessions = JSONArray()
                    for (i in (sessions.length() - 30) until sessions.length()) {
                        newSessions.put(sessions.get(i))
                    }
                    existingData.put("sessions", newSessions)
                } else {
                    existingData.put("sessions", sessions)
                }
                
                // Update aggregated stats
                updateAggregatedStats(existingData)
                
                // Save to file
                analyticsFile.writeText(existingData.toString(2))
                
                Log.d(TAG, "Session data saved")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving session data", e)
            }
        }
    }
    
    private fun sessionToJson(session: SessionSummary): JSONObject {
        return JSONObject().apply {
            put("id", session.sessionId)
            put("start_time", session.startTime)
            put("duration", session.duration)
            put("event_count", session.eventCount)
            put("features", JSONArray(session.features.toList()))
        }
    }
    
    private fun updateAggregatedStats(data: JSONObject) {
        val stats = data.optJSONObject("stats") ?: JSONObject()
        
        // Total sessions
        stats.put("total_sessions", (stats.optInt("total_sessions", 0) + 1))
        
        // Total duration
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        stats.put("total_duration", (stats.optLong("total_duration", 0) + sessionDuration))
        
        // Feature usage
        val features = stats.optJSONObject("features") ?: JSONObject()
        sessionEvents.groupBy { it.category }.forEach { (category, events) ->
            features.put(category, (features.optInt(category, 0) + events.size))
        }
        stats.put("features", features)
        
        // Last updated
        stats.put("last_updated", dateFormat.format(Date()))
        
        data.put("stats", stats)
    }
    
    private fun loadAnalyticsData(): JSONObject {
        return try {
            if (analyticsFile.exists()) {
                JSONObject(analyticsFile.readText())
            } else {
                JSONObject().apply {
                    put("version", 1)
                    put("created", dateFormat.format(Date()))
                    put("privacy_notice", "All data stored locally. No automatic transmission.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading analytics data", e)
            JSONObject()
        }
    }
    
    /**
     * Get analytics summary for user review
     */
    fun getAnalyticsSummary(): String {
        return try {
            val data = loadAnalyticsData()
            val stats = data.optJSONObject("stats") ?: JSONObject()
            
            buildString {
                appendLine("VoiceBridge Usage Analytics")
                appendLine("==========================")
                appendLine()
                appendLine("Total Sessions: ${stats.optInt("total_sessions", 0)}")
                appendLine("Total Usage Time: ${formatDuration(stats.optLong("total_duration", 0))}")
                appendLine("First Launch: ${formatDate(prefs.getLong(PREF_FIRST_LAUNCH, 0))}")
                appendLine()
                appendLine("Feature Usage:")
                val features = stats.optJSONObject("features") ?: JSONObject()
                features.keys().forEach { key ->
                    appendLine("- ${key.capitalize()}: ${features.optInt(key, 0)} events")
                }
                appendLine()
                appendLine("Privacy Notice:")
                appendLine("All data is stored locally on your device.")
                appendLine("No data is automatically transmitted.")
                appendLine("You can clear this data at any time.")
            }
        } catch (e: Exception) {
            "Error loading analytics summary"
        }
    }
    
    /**
     * Clear all analytics data
     */
    fun clearAnalyticsData() {
        scope.launch {
            try {
                if (analyticsFile.exists()) {
                    analyticsFile.delete()
                }
                sessionEvents.clear()
                Log.i(TAG, "Analytics data cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing analytics data", e)
            }
        }
    }
    
    /**
     * Export analytics data for user
     */
    fun exportAnalyticsData(): String? {
        return try {
            if (analyticsFile.exists()) {
                analyticsFile.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting analytics data", e)
            null
        }
    }
    
    /**
     * Check if analytics is enabled
     */
    fun isAnalyticsEnabled(): Boolean {
        return prefs.getBoolean(PREF_ANALYTICS_ENABLED, true) // Default enabled
    }
    
    /**
     * Enable/disable analytics
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_ANALYTICS_ENABLED, enabled).apply()
        Log.i(TAG, "Analytics ${if (enabled) "enabled" else "disabled"}")
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun formatDuration(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }
    
    private fun formatDate(timestamp: Long): String {
        return if (timestamp > 0) {
            SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timestamp))
        } else {
            "Unknown"
        }
    }
}