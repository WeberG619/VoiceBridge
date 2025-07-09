package com.voicebridge

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.voicebridge.api.ClaudeAPI
import com.voicebridge.api.WhisperAPI
import com.voicebridge.api.GoogleVisionAPI
import com.voicebridge.config.APIConfig
import com.voicebridge.vision.VisionLLMService
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * REAL VOICEBRIDGE - One Button, Real APIs
 * 
 * Features:
 * - Claude API for natural conversation (user has this)
 * - Whisper API for accent-friendly speech recognition
 * - Google Vision API for free OCR (1000/month)
 * - One-button interface like ChatGPT voice mode
 */
class RealMainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "VoiceBridge"
        private const val PERMISSION_REQUEST_CODE = 100
    }
    
    // UI Components
    private lateinit var visionButton: ImageButton
    private lateinit var cameraPreview: PreviewView
    private lateinit var statusText: TextView
    private lateinit var container: LinearLayout
    private lateinit var setupButton: Button
    
    // Camera Components
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    
    // Vision State
    private var isVisionActive = false
    private var visionHandler = Handler(Looper.getMainLooper())
    private var visionRunnable: Runnable? = null
    
    // APIs
    private lateinit var claudeAPI: ClaudeAPI
    private lateinit var whisperAPI: WhisperAPI
    private lateinit var googleVisionAPI: GoogleVisionAPI
    private lateinit var visionLLMService: VisionLLMService
    private var textToSpeech: TextToSpeech? = null
    
    // State
    private var conversationHistory = mutableListOf<String>()
    private var isSetup = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize secure API key storage
        APIConfig.initialize(this)
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        createUI()
        checkAPIKeys()
        checkPermissions()
        
        // Camera will be initialized after permissions are granted
    }
    
    private fun createUI() {
        // Main container with beautiful gradient
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#0f0f23"), // Dark
                    Color.parseColor("#1a1a2e")  // Slightly lighter
                )
            )
        }
        
        // App title - Enhanced with modern styling
        val titleText = TextView(this).apply {
            text = "🎤 VoiceBridge AI"
            textSize = 36f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 60, 0, 10)
            // Add text shadow for depth
            setShadowLayer(8f, 0f, 4f, Color.parseColor("#000000"))
        }
        container.addView(titleText)
        
        val subtitleText = TextView(this).apply {
            text = "🌟 One Button • See Everything • Understand Anything"
            textSize = 18f
            setTextColor(Color.parseColor("#c0c0c0"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
            // Add subtle glow effect
            setShadowLayer(6f, 0f, 0f, Color.parseColor("#60a5fa"))
        }
        container.addView(subtitleText)
        
        // Universal camera preview - Always visible when active
        cameraPreview = PreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(700, 500).apply {
                setMargins(20, 20, 20, 20)
            }
            visibility = android.view.View.GONE
            background = GradientDrawable().apply {
                cornerRadius = 30f
                setStroke(4, Color.parseColor("#60a5fa"))
                // Add subtle shadow effect
                setColor(Color.parseColor("#1a1a2e"))
            }
            elevation = 16f
        }
        container.addView(cameraPreview)
        
        // Status text - Enhanced with modern styling
        statusText = TextView(this).apply {
            text = "Setting up..."
            textSize = 20f
            setTextColor(Color.parseColor("#60a5fa"))
            gravity = Gravity.CENTER
            setPadding(40, 0, 40, 30)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            // Add subtle glow effect
            setShadowLayer(4f, 0f, 0f, Color.parseColor("#3b82f6"))
        }
        container.addView(statusText)
        
        // Universal Vision Button - One button for everything
        visionButton = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(350, 350)
            
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#4f46e5"), // Modern indigo
                    Color.parseColor("#3730a3")  // Deeper indigo
                )
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(8, Color.parseColor("#818cf8")) // Light indigo border
            }
            
            setImageResource(android.R.drawable.ic_menu_view)
            scaleType = android.widget.ImageView.ScaleType.CENTER
            elevation = 20f
            isEnabled = false
            
            setOnTouchListener { _, event ->
                if (!isSetup) return@setOnTouchListener false
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startVisionMode()
                        animateButton(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        processVisionInput()
                        animateButton(false)
                    }
                }
                true
            }
        }
        container.addView(visionButton)
        
        // Instructions - Enhanced with modern styling
        val instructionText = TextView(this).apply {
            text = "👁️ HOLD to see and understand anything • Release to hear response"
            textSize = 18f
            setTextColor(Color.parseColor("#d1d5db"))
            gravity = Gravity.CENTER
            setPadding(40, 50, 40, 0)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        container.addView(instructionText)
        
        // Accessibility description - Enhanced with modern styling
        val accessibilityText = TextView(this).apply {
            text = "♿ Medicine labels • Forms • Signs • Objects • Everything you need to see"
            textSize = 14f
            setTextColor(Color.parseColor("#93c5fd"))
            gravity = Gravity.CENTER
            setPadding(40, 25, 40, 0)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            // Add subtle glow effect
            setShadowLayer(4f, 0f, 0f, Color.parseColor("#3b82f6"))
        }
        container.addView(accessibilityText)
        
        // Setup button - Enhanced with modern gradient
        setupButton = Button(this).apply {
            text = "⚙️ Setup API Keys"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.parseColor("#f59e0b"), // Golden orange
                    Color.parseColor("#d97706")  // Deeper orange
                )
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 30f
                setStroke(3, Color.parseColor("#fbbf24"))
            }
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 60, 0, 0)
            }
            
            setPadding(50, 25, 50, 25)
            elevation = 8f
            
            setOnClickListener { showAPISetupDialog() }
        }
        container.addView(setupButton)
        
        setContentView(container)
    }
    
    private fun checkAPIKeys() {
        // Use APIConfig to check setup
        if (APIConfig.isConfigured()) {
            initializeAPIs()
            isSetup = true
            setupButton.visibility = Button.GONE
            updateStatus("Ready! Hold button to see and understand anything")
            visionButton.isEnabled = true
            
        } else {
            updateStatus("Please set up API keys first")
            showAPISetupDialog()
        }
    }
    
    private fun showAPISetupDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        
        val titleText = TextView(this).apply {
            text = "🔑 Enter Your API Keys"
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 20)
        }
        dialogView.addView(titleText)
        
        val instructionText = TextView(this).apply {
            text = "Enter your API keys below. They'll be stored securely on your device."
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        dialogView.addView(instructionText)
        
        // Claude API Key Input
        val claudeLabel = TextView(this).apply {
            text = "Claude API Key (Required)"
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 8)
        }
        dialogView.addView(claudeLabel)
        
        val claudeInput = android.widget.EditText(this).apply {
            hint = "sk-ant-..."
            setText(APIConfig.getClaudeApiKey())
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        dialogView.addView(claudeInput)
        
        // OpenAI API Key Input
        val openaiLabel = TextView(this).apply {
            text = "OpenAI API Key (Optional - for speech)"
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 8)
        }
        dialogView.addView(openaiLabel)
        
        val openaiInput = android.widget.EditText(this).apply {
            hint = "sk-proj-..."
            setText(APIConfig.getOpenAiApiKey())
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        dialogView.addView(openaiInput)
        
        // Google Vision API Key Input
        val visionLabel = TextView(this).apply {
            text = "Google Vision API Key (Optional - for OCR)"
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 8)
        }
        dialogView.addView(visionLabel)
        
        val visionInput = android.widget.EditText(this).apply {
            hint = "AIza..."
            setText(APIConfig.getGoogleVisionApiKey())
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        dialogView.addView(visionInput)
        
        val helpText = TextView(this).apply {
            text = "\nGet API keys:\n• Claude: console.anthropic.com\n• OpenAI: platform.openai.com\n• Google: cloud.google.com/vision"
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, 20, 0, 0)
        }
        dialogView.addView(helpText)
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save & Continue") { _, _ -> 
                // Save API keys securely
                APIConfig.setClaudeApiKey(claudeInput.text.toString().trim())
                APIConfig.setOpenAiApiKey(openaiInput.text.toString().trim())
                APIConfig.setGoogleVisionApiKey(visionInput.text.toString().trim())
                
                // Re-check configuration
                checkAPIKeys()
            }
            .setNegativeButton("Use Demo Mode") { _, _ -> 
                enableDemoMode()
            }
            .setNeutralButton("Clear All") { _, _ ->
                APIConfig.clearAllKeys()
                updateStatus("API keys cleared")
            }
            .setCancelable(false)
            .show()
    }
    
    private fun enableDemoMode() {
        updateStatus("Please set up API keys for full functionality")
        visionButton.isEnabled = false
        isSetup = false
        setupButton.visibility = Button.VISIBLE
        
        // Initialize TTS for feedback
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.setLanguage(Locale.US)
                speak("Please add your API keys to use VoiceBridge.")
            }
        }
    }
    
    private fun initializeAPIs() {
        claudeAPI = ClaudeAPI(APIConfig.getClaudeApiKey())
        
        if (APIConfig.hasSpeechAPI()) {
            whisperAPI = WhisperAPI(APIConfig.getOpenAiApiKey(), this)
        }
        
        if (APIConfig.hasVisionAPI()) {
            googleVisionAPI = GoogleVisionAPI(APIConfig.getGoogleVisionApiKey())
        }
        
        // Initialize the new VLM service
        visionLLMService = VisionLLMService(this, claudeAPI)
        
        // Initialize BLIP-2 model asynchronously
        lifecycleScope.launch {
            visionLLMService.initializeBLIP2()
        }
        
        // Initialize TTS
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.setLanguage(Locale.US)
                speak("VoiceBridge ready! I can see and describe anything you point your camera at using advanced vision AI.")
            }
        }
    }
    
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            updateStatus("Requesting camera and microphone permissions...")
            speak("I need camera and microphone permissions to help you see and hear.")
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Permissions already granted, initialize camera
            initializeCamera()
        }
    }
    
    private fun startVisionMode() {
        if (isVisionActive) return
        isVisionActive = true
        
        // Make sure camera is initialized first
        if (camera == null || preview == null) {
            updateStatus("Initializing camera...")
            speak("Setting up camera for vision")
            initializeCamera()
            // Wait a moment for camera to initialize
            visionHandler.postDelayed({
                startVisionModeAfterCameraReady()
            }, 1000)
            return
        }
        
        startVisionModeAfterCameraReady()
    }
    
    private fun startVisionModeAfterCameraReady() {
        cameraPreview.visibility = android.view.View.VISIBLE
        
        // Change button to active state
        visionButton.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#dc2626"), // Red when active
                Color.parseColor("#b91c1c")  // Deeper red
            )
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(8, Color.parseColor("#f87171"))
        }
        
        updateStatus("👁️ Vision Active - I can see what you're pointing at!")
        speak("Vision activated. I can see what you're pointing at.")
        
        // Start continuous vision analysis
        startContinuousVisionAnalysis()
        
        Log.d(TAG, "✅ Vision mode started")
    }
    
    private fun processVisionInput() {
        if (!isVisionActive) return
        
        updateStatus("📸 Analyzing what I see...")
        
        // Change button back to normal
        visionButton.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#4f46e5"), // Modern indigo
                Color.parseColor("#3730a3")  // Deeper indigo
            )
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(8, Color.parseColor("#818cf8"))
        }
        
        // Capture current frame and analyze
        val bitmap = cameraPreview.getBitmap()
        if (bitmap != null) {
            lifecycleScope.launch {
                val visionResult = visionLLMService.analyzeImage(
                    bitmap, 
                    "Tell me about what you see in this image. If it's medicine, tell me about the medicine. If it's a form, help me understand it. If it's text, read it to me. Describe anything important."
                )
                
                updateStatus("AI sees: ${visionResult.description.take(50)}...")
                speak(visionResult.description)
                
                // Add to conversation history
                conversationHistory.add("Vision capture analyzed")
                conversationHistory.add(visionResult.description)
                
                Log.d(TAG, "✅ Vision analysis: Tier ${visionResult.tier}, ${visionResult.processingTime}ms")
            }
        } else {
            updateStatus("Couldn't capture image")
            speak("I couldn't capture what you're pointing at. Please try again.")
        }
        
        // Stop vision mode
        stopVisionMode()
    }
    
    private fun stopVisionMode() {
        isVisionActive = false
        cameraPreview.visibility = android.view.View.GONE
        
        // Stop continuous analysis
        visionRunnable?.let { 
            visionHandler.removeCallbacks(it)
        }
        
        Log.d(TAG, "Vision mode stopped")
    }
    
    // Removed processUserInput - using direct vision analysis instead
    
    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    private fun updateStatus(status: String) {
        runOnUiThread {
            statusText.text = status
        }
    }
    
    private fun animateButton(pressed: Boolean) {
        visionButton.animate()
            .scaleX(if (pressed) 0.9f else 1.0f)
            .scaleY(if (pressed) 0.9f else 1.0f)
            .setDuration(100)
            .start()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                updateStatus("Permissions granted! Initializing camera...")
                speak("Permissions granted. Setting up camera for live vision.")
                // Now initialize camera with permissions
                initializeCamera()
            } else {
                updateStatus("Camera and microphone permissions needed")
                speak("I need camera permission to help you see. Please grant permissions in settings.")
                
                // Show dialog explaining why permissions are needed
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Permissions Required for Accessibility")
                    .setMessage("VoiceBridge needs camera permission to provide live vision assistance for visual impairments and reading difficulties. Please grant permissions in Settings.")
                    .setPositiveButton("Settings") { _, _ ->
                        // Open app settings
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.fromParts("package", packageName, null)
                        startActivity(intent)
                    }
                    .setNegativeButton("Voice Only") { _, _ ->
                        updateStatus("Voice mode only - camera features disabled")
                        speak("Operating in voice mode only. Camera features are disabled.")
                    }
                    .show()
            }
        }
    }
    
    // Removed photo capture functions - using single vision button instead
    
    // Removed old live vision functions - using single vision button instead
    
    /**
     * Continuous Vision Analysis - Every 3 seconds
     */
    private fun startContinuousVisionAnalysis() {
        visionRunnable = object : Runnable {
            override fun run() {
                if (isVisionActive) {
                    analyzeLiveFrame()
                    // Schedule next analysis in 3 seconds
                    visionHandler.postDelayed(this, 3000)
                }
            }
        }
        
        // Start first analysis
        visionHandler.post(visionRunnable!!)
    }
    
    /**
     * Analyze current camera frame for accessibility
     */
    private fun analyzeLiveFrame() {
        if (!isVisionActive) {
            return
        }
        
        try {
            // Capture current frame from preview
            val bitmap = cameraPreview.getBitmap()
            
            if (bitmap == null) {
                Log.w(TAG, "Could not capture frame from camera preview")
                return
            }
            
            Log.d(TAG, "📸 Captured frame for continuous vision analysis")
            
            lifecycleScope.launch {
                try {
                    // Use the new VLM service for continuous analysis
                    val visionResult = visionLLMService.analyzeImage(
                        bitmap, 
                        "Briefly describe what you see for navigation assistance. Focus on obstacles, text, and important objects."
                    )
                    
                    // Update status with brief description
                    if (visionResult.description.length > 10 && visionResult.confidence > 0.3f) {
                        runOnUiThread {
                            updateStatus("👁️ Seeing (Tier ${visionResult.tier}): ${visionResult.description.take(60)}...")
                        }
                        Log.d(TAG, "✅ Continuous vision analysis: Tier ${visionResult.tier}, ${visionResult.processingTime}ms, confidence: ${visionResult.confidence}")
                        // Don't speak every frame - only when user releases button
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Continuous vision analysis error", e)
                    runOnUiThread {
                        updateStatus("👁️ Vision active (analysis paused)")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing frame for continuous vision", e)
        }
    }
    
    /**
     * Initialize camera for both photo capture AND live vision
     */
    private fun initializeCamera() {
        // Check if we have camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted")
            updateStatus("Camera permission needed")
            return
        }
        
        updateStatus("Setting up camera...")
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                
                // Preview for live vision
                preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(cameraPreview.surfaceProvider)
                    }
                
                // Image capture for photos
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind both preview and capture
                camera = cameraProvider.bindToLifecycle(
                    this, 
                    cameraSelector,
                    preview,
                    imageCapture
                )
                
                updateStatus("Camera ready! 📷 Tap green for photos, 👁️ golden for live vision")
                speak("Camera is ready. You can now use photo capture and live vision features.")
                
                Log.d(TAG, "✅ Camera successfully initialized for live vision and photo capture")
                Log.d(TAG, "✅ Camera object: $camera")
                Log.d(TAG, "✅ Preview object: $preview")
                Log.d(TAG, "✅ ImageCapture object: $imageCapture")
                
            } catch (exc: Exception) {
                Log.e(TAG, "❌ Camera initialization failed", exc)
                updateStatus("Camera setup failed - please restart app")
                speak("Camera setup failed. Please restart the app and grant camera permission.")
                
                // Show helpful error dialog
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Camera Setup Failed")
                    .setMessage("Unable to initialize camera. This may be because:\n\n• Another app is using the camera\n• Camera permission was denied\n• Device camera is not available\n\nPlease restart the app and ensure camera permission is granted.")
                    .setPositiveButton("Restart App") { _, _ ->
                        // Restart the app
                        val intent = intent
                        finish()
                        startActivity(intent)
                    }
                    .setNegativeButton("Voice Only") { _, _ ->
                        updateStatus("Voice mode only")
                        speak("Operating in voice mode only.")
                    }
                    .show()
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.shutdown()
        cameraExecutor.shutdown()
        if (APIConfig.hasSpeechAPI() && ::whisperAPI.isInitialized) {
            whisperAPI.cleanup()
        }
        if (::visionLLMService.isInitialized) {
            visionLLMService.cleanup()
        }
    }
}