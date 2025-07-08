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
    private lateinit var mainButton: ImageButton
    private lateinit var cameraButton: ImageButton
    private lateinit var liveVisionButton: ImageButton
    private lateinit var cameraPreview: PreviewView
    private lateinit var statusText: TextView
    private lateinit var container: LinearLayout
    private lateinit var setupButton: Button
    
    // Camera Components
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    
    // Live Vision State
    private var isLiveVisionActive = false
    private var liveVisionHandler = Handler(Looper.getMainLooper())
    private var liveVisionRunnable: Runnable? = null
    
    // APIs
    private lateinit var claudeAPI: ClaudeAPI
    private lateinit var whisperAPI: WhisperAPI
    private lateinit var googleVisionAPI: GoogleVisionAPI
    private var textToSpeech: TextToSpeech? = null
    
    // State
    private var isListening = false
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
            text = "🌟 Universal Vision Assistant • Read Anything • See Everything"
            textSize = 18f
            setTextColor(Color.parseColor("#c0c0c0"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
            // Add subtle glow effect
            setShadowLayer(6f, 0f, 0f, Color.parseColor("#60a5fa"))
        }
        container.addView(subtitleText)
        
        // Live camera preview (initially hidden) - Made larger and more modern
        cameraPreview = PreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(600, 400).apply {
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
        
        // Main voice button (like ChatGPT) - Enhanced with modern gradient
        mainButton = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(280, 280)
            
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
            
            setImageResource(android.R.drawable.ic_btn_speak_now)
            scaleType = android.widget.ImageView.ScaleType.CENTER
            elevation = 16f
            isEnabled = false
            
            setOnTouchListener { _, event ->
                if (!isSetup) return@setOnTouchListener false
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startListening()
                        animateButton(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        stopListening()
                        animateButton(false)
                    }
                }
                true
            }
        }
        container.addView(mainButton)
        
        // Camera button - Enhanced with modern gradient
        cameraButton = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                setMargins(0, 40, 0, 0)
            }
            
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#059669"), // Modern emerald
                    Color.parseColor("#047857")  // Deeper emerald
                )
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(6, Color.parseColor("#6ee7b7")) // Light emerald border
            }
            
            setImageResource(android.R.drawable.ic_menu_camera)
            scaleType = android.widget.ImageView.ScaleType.CENTER
            elevation = 12f
            
            setOnClickListener {
                capturePhoto()
            }
        }
        container.addView(cameraButton)
        
        // Live Vision Button (GAME CHANGER!) - Enhanced with modern gradient
        liveVisionButton = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                setMargins(0, 30, 0, 0)
            }
            
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#f59e0b"), // Golden orange
                    Color.parseColor("#d97706")  // Deeper orange
                )
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(6, Color.parseColor("#fde68a")) // Light golden border
            }
            
            setImageResource(android.R.drawable.ic_menu_view)
            scaleType = android.widget.ImageView.ScaleType.CENTER
            elevation = 12f
            
            setOnClickListener {
                toggleLiveVision()
            }
        }
        container.addView(liveVisionButton)
        
        // Instructions - Enhanced with modern styling
        val instructionText = TextView(this).apply {
            text = "🎤 Hold to talk • 📷 Capture anything • 👁️ Live vision for everything"
            textSize = 16f
            setTextColor(Color.parseColor("#d1d5db"))
            gravity = Gravity.CENTER
            setPadding(40, 50, 40, 0)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        container.addView(instructionText)
        
        // Accessibility description - Enhanced with modern styling
        val accessibilityText = TextView(this).apply {
            text = "♿ Read medicine labels • Identify objects • Navigate the world"
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
            updateStatus("Ready! Hold button to speak")
            mainButton.isEnabled = true
            
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
        mainButton.isEnabled = false
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
        
        // Initialize TTS
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.setLanguage(Locale.US)
                speak("VoiceBridge ready! I can see and describe anything you point your camera at.")
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
    
    private fun startListening() {
        if (isListening) return
        isListening = true
        
        updateStatus("Listening... speak naturally")
        
        // Change button to red when listening
        mainButton.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#ef4444")) // Red
            setStroke(8, Color.parseColor("#f87171"))
        }
        
        if (isSetup && APIConfig.hasSpeechAPI() && ::whisperAPI.isInitialized) {
            // Real speech recognition
            whisperAPI.startRecording()
        }
    }
    
    private fun stopListening() {
        if (!isListening) return
        isListening = false
        
        updateStatus("Processing...")
        
        // Change button back to blue
        mainButton.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#6366f1")) // Indigo
            setStroke(6, Color.parseColor("#8b5cf6"))
        }
        
        if (isSetup && APIConfig.hasSpeechAPI() && ::whisperAPI.isInitialized) {
            // Real transcription
            lifecycleScope.launch {
                val transcription = whisperAPI.stopRecordingAndTranscribe()
                if (transcription.isNotEmpty()) {
                    processUserInput(transcription)
                } else {
                    updateStatus("Didn't catch that. Try again!")
                    speak("I didn't catch that. Please try again.")
                }
            }
        } else {
            // No speech API - still try to process as text
            updateStatus("Voice input not available. Please set up speech API key.")
            speak("Please add your speech API key to use voice input.")
        }
    }
    
    private suspend fun processUserInput(userInput: String) {
        updateStatus("You: $userInput")
        
        try {
            var response = ""
            
            // Always check if live vision is active first
            if (isLiveVisionActive) {
                // Live vision is on - analyze what camera sees
                updateStatus("📸 Analyzing what you're pointing at...")
                
                // Ensure camera is ready
                if (camera == null || preview == null) {
                    response = "Camera is not ready yet. Please wait a moment and try again."
                    Log.w(TAG, "⚠️ Camera not initialized for live vision")
                } else {
                    val bitmap = cameraPreview.getBitmap()
                    if (bitmap != null) {
                        Log.d(TAG, "📸 Successfully captured frame from camera preview")
                        val ocrResult = googleVisionAPI.extractTextFromImage(bitmap)
                        
                        // Pure vision assistance - let AI decide what it sees
                        response = claudeAPI.chatAboutVision(
                            userMessage = userInput,
                            sceneText = ocrResult.text,
                            conversationHistory = conversationHistory
                        )
                        
                        Log.d(TAG, "✅ Live vision analysis completed for: $userInput")
                        
                    } else {
                        response = "I can't capture the current camera frame. Make sure live vision is on and the camera has permission."
                        Log.w(TAG, "⚠️ Could not capture frame from camera preview - bitmap is null")
                    }
                }
            } else {
                // No live vision - regular conversation
                response = claudeAPI.chatAboutVision(
                    userMessage = userInput,
                    sceneText = null,
                    conversationHistory = conversationHistory
                )
            }
            
            // Add to conversation history
            conversationHistory.add(userInput)
            conversationHistory.add(response)
            
            // Keep history manageable
            if (conversationHistory.size > 10) {
                conversationHistory = conversationHistory.takeLast(6).toMutableList()
            }
            
            updateStatus("AI: $response")
            speak(response)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing input", e)
            updateStatus("Having trouble. Try again!")
            speak("I'm having trouble right now. Please try again.")
        }
    }
    
    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    private fun updateStatus(status: String) {
        runOnUiThread {
            statusText.text = status
        }
    }
    
    private fun animateButton(pressed: Boolean) {
        mainButton.animate()
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
    
    /**
     * Capture photo using camera for OCR processing
     */
    private fun capturePhoto() {
        if (!APIConfig.hasVisionAPI()) {
            updateStatus("Add Google Vision API key for camera features")
            speak("Please add your Google Vision API key to use camera features")
            showAPISetupDialog()
            return
        }
        
        // Create image file
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        
        // Create output options
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
        
        // Set up image capture listener
        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    updateStatus("Camera error - try again")
                    speak("Camera error, please try again")
                }
                
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    updateStatus("Photo captured! Processing...")
                    speak("Photo captured, analyzing form")
                    
                    // Process the captured image with OCR
                    output.savedUri?.let { uri ->
                        processImageWithOCR(uri)
                    }
                }
            }
        )
        
        // Visual feedback
        cameraButton.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(100)
            .withEndAction {
                cameraButton.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    
    /**
     * Process captured image with Google Vision OCR - Pure vision analysis
     */
    private fun processImageWithOCR(imageUri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                // Convert URI to bitmap
                val inputStream = contentResolver.openInputStream(imageUri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                if (bitmap != null) {
                    // Use Google Vision API for OCR
                    val ocrResult = googleVisionAPI.extractTextFromImage(bitmap)
                    
                    // Always analyze what's in the image, regardless of text
                    val claudeResponse = claudeAPI.chatAboutVision(
                        userMessage = "I took a photo. Please describe what you see in this image and read any text that's visible.",
                        sceneText = ocrResult.text,
                        conversationHistory = conversationHistory
                    )
                    
                    updateStatus("Photo analyzed! Listen to response...")
                    speak(claudeResponse)
                    
                    // Add to conversation history
                    conversationHistory.add("Photo captured with text: ${ocrResult.text}")
                    conversationHistory.add(claudeResponse)
                    
                    Log.d(TAG, "✅ Photo processed successfully")
                    Log.d(TAG, "📝 OCR Text found: ${ocrResult.text}")
                    
                } else {
                    updateStatus("Error processing image")
                    speak("Error processing the image. Please try again.")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image", e)
                updateStatus("Image processing failed")
                speak("Error analyzing the image. Please try again.")
            }
        }
    }
    
    /**
     * Toggle Live Vision Mode - ACCESSIBILITY GAME CHANGER!
     */
    private fun toggleLiveVision() {
        if (!APIConfig.hasVisionAPI()) {
            updateStatus("Add Google Vision API key for live vision")
            speak("Please add your Google Vision API key to use the live vision assistant")
            showAPISetupDialog()
            return
        }
        
        isLiveVisionActive = !isLiveVisionActive
        
        if (isLiveVisionActive) {
            startLiveVision()
        } else {
            stopLiveVision()
        }
    }
    
    /**
     * Start Live Vision - Continuous AI sight assistant
     */
    private fun startLiveVision() {
        // Make sure camera is initialized first
        if (camera == null || preview == null) {
            updateStatus("Initializing camera for live vision...")
            speak("Setting up camera for live vision")
            initializeCamera()
            // Wait a moment for camera to initialize
            liveVisionHandler.postDelayed({
                startLiveVisionAfterCameraReady()
            }, 1000)
            return
        }
        
        startLiveVisionAfterCameraReady()
    }
    
    private fun startLiveVisionAfterCameraReady() {
        cameraPreview.visibility = android.view.View.VISIBLE
        
        // Update button appearance
        liveVisionButton.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#dc2626"), // Red when active
                Color.parseColor("#b91c1c")  // Deeper red
            )
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(4, Color.parseColor("#f87171"))
        }
        
        updateStatus("👁️ Live Vision Active - Camera is ON and I can see!")
        speak("Live vision activated. Camera is now on and I can see your surroundings. Ask me what I see!")
        
        // Start continuous vision analysis
        startContinuousVisionAnalysis()
        
        Log.d(TAG, "✅ Live Vision started - Camera active and ready")
    }
    
    /**
     * Stop Live Vision
     */
    private fun stopLiveVision() {
        cameraPreview.visibility = android.view.View.GONE
        
        // Reset button appearance
        liveVisionButton.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#f59e0b"), // Golden orange
                Color.parseColor("#d97706")  // Deeper orange
            )
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(4, Color.parseColor("#fbbf24"))
        }
        
        // Stop continuous analysis
        liveVisionRunnable?.let { 
            liveVisionHandler.removeCallbacks(it)
        }
        
        updateStatus("Live Vision stopped")
        speak("Live vision stopped. Tap the orange button to restart when needed.")
        
        Log.d(TAG, "Live Vision stopped")
    }
    
    /**
     * Continuous Vision Analysis - Every 3 seconds
     */
    private fun startContinuousVisionAnalysis() {
        liveVisionRunnable = object : Runnable {
            override fun run() {
                if (isLiveVisionActive) {
                    analyzeLiveFrame()
                    // Schedule next analysis in 3 seconds
                    liveVisionHandler.postDelayed(this, 3000)
                }
            }
        }
        
        // Start first analysis
        liveVisionHandler.post(liveVisionRunnable!!)
    }
    
    /**
     * Analyze current camera frame for accessibility
     */
    private fun analyzeLiveFrame() {
        if (!isLiveVisionActive || !APIConfig.hasVisionAPI()) {
            return
        }
        
        try {
            // Capture current frame from preview
            val bitmap = cameraPreview.getBitmap()
            
            if (bitmap == null) {
                Log.w(TAG, "Could not capture frame from camera preview")
                return
            }
            
            Log.d(TAG, "📸 Captured frame for live vision analysis")
            
            lifecycleScope.launch {
                try {
                    // Use Google Vision for scene analysis
                    val ocrResult = googleVisionAPI.extractTextFromImage(bitmap)
                    
                    // Create accessibility-focused prompt
                    val prompt = buildString {
                        append("You are an AI sight assistant helping someone navigate their environment. ")
                        append("Describe what you see in this scene briefly and helpfully. ")
                        append("Focus on: obstacles, text to read, objects, people, navigation help. ")
                        append("Be concise but descriptive. ")
                        if (ocrResult.text.isNotEmpty()) {
                            append("Text visible: ${ocrResult.text}")
                        }
                    }
                    
                    // Get Claude's description using vision method
                    val description = claudeAPI.chatAboutVision(
                        userMessage = prompt,
                        sceneText = ocrResult.text,
                        conversationHistory = emptyList() // Keep it fresh for live analysis
                    )
                    
                    // Update status with brief description
                    if (description.length > 10 && !description.contains("I can't see")) {
                        runOnUiThread {
                            updateStatus("👁️ Live: ${description.take(60)}...")
                        }
                        Log.d(TAG, "✅ Live vision analysis: $description")
                        // Don't speak every frame - only on voice request
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Live vision analysis error", e)
                    runOnUiThread {
                        updateStatus("👁️ Live vision active (analysis paused)")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing frame for live vision", e)
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
    }
}