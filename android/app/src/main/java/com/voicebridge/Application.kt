package com.voicebridge

import android.app.Application
import android.util.Log
import com.voicebridge.crash.CrashReporter
import com.voicebridge.telemetry.PrivacyAnalytics

/**
 * VoiceBridge Application Class
 * 
 * Initializes application-level components:
 * - Crash reporting
 * - Privacy analytics
 * - Global error handling
 */
class VoiceBridgeApplication : Application() {
    
    companion object {
        private const val TAG = "VoiceBridgeApp"
    }
    
    private lateinit var crashReporter: CrashReporter
    private lateinit var analytics: PrivacyAnalytics
    
    override fun onCreate() {
        super.onCreate()
        
        Log.i(TAG, "VoiceBridge application starting...")
        
        // Initialize crash reporting
        initializeCrashReporting()
        
        // Initialize analytics
        initializeAnalytics()
        
        Log.i(TAG, "VoiceBridge application initialized")
    }
    
    private fun initializeCrashReporting() {
        try {
            crashReporter = CrashReporter.getInstance(this)
            CrashReporter.initialize(this)
            Log.i(TAG, "Crash reporting initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize crash reporting", e)
        }
    }
    
    private fun initializeAnalytics() {
        try {
            analytics = PrivacyAnalytics.getInstance(this)
            analytics.initialize()
            
            // Track app launch
            analytics.trackFeatureUsage("app", "launch")
            
            Log.i(TAG, "Analytics initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize analytics", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // End analytics session
        try {
            analytics.endSession()
        } catch (e: Exception) {
            Log.e(TAG, "Error ending analytics session", e)
        }
    }
}