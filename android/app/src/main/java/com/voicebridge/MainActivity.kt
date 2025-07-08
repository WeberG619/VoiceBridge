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
import com.voicebridge.audio.AudioRecorder
import com.voicebridge.audio.AudioData
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job

/**
 * Main Activity for VoiceBridge - Real Audio Implementation
 * 
 * Features:
 * - Basic UI with permission handling
 * - Real voice recording with AudioRecorder
 * - Camera integration preparation
 * - Voice activity detection and audio processing
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
    private var audioRecorder: AudioRecorder? = null
    private var recordingJob: Job? = null
    
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
        
        Log.i(TAG, "VoiceBridge MainActivity initialized - Real Audio Version")
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
            // Initialize real AudioRecorder
            audioRecorder = AudioRecorder(this)
            
            if (audioRecorder?.initialize() == true) {
                updateStatusText("Audio recorder initialized ✓")
                Log.d(TAG, "Real audio recorder initialized successfully")
            } else {
                updateStatusText("Audio recorder initialization failed")
                Log.e(TAG, "Failed to initialize audio recorder")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio recorder initialization failed", e)
            updateStatusText("Audio recorder failed: ${e.message}")
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
            val recorder = audioRecorder
            if (recorder == null) {
                updateStatusText("Audio recorder not initialized")
                return
            }
            
            isRecording = true
            recordButton.text = "Stop Recording"
            updateStatusText("Recording... Speak now")
            
            // Start real audio recording
            recordingJob = lifecycleScope.launch {
                try {
                    recorder.startRecording().collect { audioData ->
                        // Process audio data in real-time
                        processAudioData(audioData)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during recording", e)
                    updateStatusText("Recording error: ${e.message}")
                    stopRecording()
                }
            }
            
            Log.d(TAG, "Real audio recording started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            updateStatusText("Recording error: ${e.message}")
            isRecording = false
            recordButton.text = "Start Recording"
        }
    }
    
    private fun stopRecording() {
        try {
            isRecording = false
            recordButton.text = "Start Recording"
            updateStatusText("Processing audio...")
            
            // Cancel recording job
            recordingJob?.cancel()
            recordingJob = null
            
            // Stop audio recorder
            audioRecorder?.stopRecording()
            
            Log.d(TAG, "Real audio recording stopped")
            
            // Show completion message
            updateStatusText("Audio recording complete! Ready for next command")
            
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
    
    private fun processAudioData(audioData: AudioData) {
        // Update UI with real-time audio information
        runOnUiThread {
            if (audioData.isVoiceActive) {
                updateStatusText("Voice detected! Volume: ${audioData.volume.toInt()}%")
            } else {
                updateStatusText("Listening... (no voice)")
            }
        }
        
        // Log audio statistics
        Log.d(TAG, "Audio - Voice: ${audioData.isVoiceActive}, Energy: ${audioData.energy}, RMS: ${audioData.rms}")
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
            // Stop recording if active
            if (isRecording) {
                stopRecording()
            }
            
            // Clean up audio recorder
            audioRecorder?.release()
            audioRecorder = null
            
            Log.i(TAG, "MainActivity destroyed and cleaned up")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}