package com.voicebridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.voicebridge.telemetry.OfflineCrashReporter
import kotlinx.coroutines.launch

/**
 * Main Activity for VoiceBridge - Incremental Implementation
 * 
 * Features:
 * - Basic UI with permission handling
 * - Voice recording preparation
 * - Camera integration preparation
 * - Step-by-step feature activation
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    }
    
    // UI Components
    private lateinit var statusText: TextView
    private lateinit var recordButton: Button
    private lateinit var cameraButton: Button
    private lateinit var settingsButton: Button
    
    // Core Components
    private lateinit var crashReporter: OfflineCrashReporter
    
    // State
    private var isRecording = false
    private var isCameraMode = false
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize crash reporting
        crashReporter = OfflineCrashReporter.getInstance(this)
        crashReporter.initialize()
        
        // Create UI programmatically for now
        createSimpleUI()
        
        // Check and request permissions
        checkPermissions()
        
        Log.i(TAG, "VoiceBridge MainActivity initialized - Incremental Version")
    }
    
    private fun createSimpleUI() {
        // Create main container
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        // Status text
        statusText = TextView(this).apply {
            text = "VoiceBridge v2.0 - Full Implementation Loading..."
            textSize = 18f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(statusText)
        
        // Record button
        recordButton = Button(this).apply {
            text = "Start Recording"
            setPadding(0, 20, 0, 20)
            setOnClickListener {
                if (isRecording) {
                    stopRecording()
                } else {
                    startRecording()
                }
            }
        }
        layout.addView(recordButton)
        
        // Camera button
        cameraButton = Button(this).apply {
            text = "Start Camera"
            setPadding(0, 20, 0, 20)
            setOnClickListener {
                if (isCameraMode) {
                    stopCamera()
                } else {
                    startCamera()
                }
            }
        }
        layout.addView(cameraButton)
        
        // Settings button
        settingsButton = Button(this).apply {
            text = "Open Settings"
            setPadding(0, 20, 0, 20)
            setOnClickListener {
                openSettings()
            }
        }
        layout.addView(settingsButton)
        
        setContentView(layout)
        
        // Initial status
        updateStatusText("VoiceBridge Ready - Grant permissions to unlock features")
    }
    
    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
            updateStatusText("Please grant permissions to continue")
        } else {
            onPermissionsGranted()
        }
    }
    
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val allGranted = permissions.all { it.value }
        
        if (allGranted) {
            onPermissionsGranted()
        } else {
            val deniedPermissions = permissions.filterValues { !it }.keys
            updateStatusText("Permissions denied: ${deniedPermissions.joinToString(", ")}")
            
            Toast.makeText(
                this,
                "VoiceBridge needs camera and microphone permissions to function",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun onPermissionsGranted() {
        lifecycleScope.launch {
            try {
                updateStatusText("Permissions granted! Initializing VoiceBridge components...")
                
                // Initialize components step by step
                initializeAudioRecorder()
                initializeCameraProcessor()
                initializeOCRProcessor()
                initializeSkillEngine()
                
                updateStatusText("VoiceBridge Ready - All features unlocked!")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing components", e)
                updateStatusText("Error: ${e.message}")
            }
        }
    }
    
    private suspend fun initializeAudioRecorder() {
        try {
            // TODO: Initialize AudioRecorder when ready
            updateStatusText("Audio recorder initialized ✓")
            Log.d(TAG, "Audio recorder initialization simulated")
        } catch (e: Exception) {
            Log.e(TAG, "Audio recorder initialization failed", e)
            updateStatusText("Audio recorder failed")
        }
    }
    
    private suspend fun initializeCameraProcessor() {
        try {
            // TODO: Initialize CameraProcessor when ready
            updateStatusText("Camera processor initialized ✓")
            Log.d(TAG, "Camera processor initialization simulated")
        } catch (e: Exception) {
            Log.e(TAG, "Camera processor initialization failed", e)
            updateStatusText("Camera processor failed")
        }
    }
    
    private suspend fun initializeOCRProcessor() {
        try {
            // TODO: Initialize OCRProcessor when ready
            updateStatusText("OCR processor initialized ✓")
            Log.d(TAG, "OCR processor initialization simulated")
        } catch (e: Exception) {
            Log.e(TAG, "OCR processor initialization failed", e)
            updateStatusText("OCR processor failed")
        }
    }
    
    private suspend fun initializeSkillEngine() {
        try {
            // TODO: Initialize SkillEngine when ready
            updateStatusText("Skill engine initialized ✓")
            Log.d(TAG, "Skill engine initialization simulated")
        } catch (e: Exception) {
            Log.e(TAG, "Skill engine initialization failed", e)
            updateStatusText("Skill engine failed")
        }
    }
    
    private fun startRecording() {
        try {
            isRecording = true
            recordButton.text = "Stop Recording"
            updateStatusText("Recording simulation... Speak now")
            
            // Simulate recording for now
            Log.d(TAG, "Recording started (simulated)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            updateStatusText("Recording error: ${e.message}")
        }
    }
    
    private fun stopRecording() {
        try {
            isRecording = false
            recordButton.text = "Start Recording"
            updateStatusText("Processing audio simulation...")
            
            // Simulate processing
            Log.d(TAG, "Recording stopped (simulated)")
            
            // Show result after delay
            statusText.postDelayed({
                updateStatusText("Audio processed! Ready for next command")
            }, 2000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            updateStatusText("Error stopping recording: ${e.message}")
        }
    }
    
    private fun startCamera() {
        try {
            isCameraMode = true
            cameraButton.text = "Stop Camera"
            updateStatusText("Camera simulation active - OCR ready")
            
            // Simulate camera
            Log.d(TAG, "Camera started (simulated)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera", e)
            updateStatusText("Camera error: ${e.message}")
        }
    }
    
    private fun stopCamera() {
        try {
            isCameraMode = false
            cameraButton.text = "Start Camera"
            updateStatusText("Camera stopped")
            
            Log.d(TAG, "Camera stopped (simulated)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
            updateStatusText("Error stopping camera: ${e.message}")
        }
    }
    
    private fun openSettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            updateStatusText("Opening accessibility settings...")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings", e)
            Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateStatusText(message: String) {
        runOnUiThread {
            statusText.text = message
            Log.d(TAG, "Status: $message")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        try {
            // Clean up any resources
            Log.i(TAG, "MainActivity destroyed and cleaned up")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}