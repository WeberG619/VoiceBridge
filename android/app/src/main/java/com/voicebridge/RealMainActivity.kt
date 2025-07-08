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
    private lateinit var statusText: TextView
    private lateinit var container: LinearLayout
    private lateinit var setupButton: Button
    
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
        
        createUI()
        checkAPIKeys()
        checkPermissions()
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
        
        // App title
        val titleText = TextView(this).apply {
            text = "🎤 VoiceBridge AI"
            textSize = 32f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 80, 0, 10)
        }
        container.addView(titleText)
        
        val subtitleText = TextView(this).apply {
            text = "Real AI • Natural Speech • Any Accent"
            textSize = 16f
            setTextColor(Color.parseColor("#a0a0a0"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        container.addView(subtitleText)
        
        // Status text
        statusText = TextView(this).apply {
            text = "Setting up..."
            textSize = 18f
            setTextColor(Color.parseColor("#60a5fa"))
            gravity = Gravity.CENTER
            setPadding(40, 0, 40, 30)
        }
        container.addView(statusText)
        
        // Main voice button (like ChatGPT)
        mainButton = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(250, 250)
            
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#6366f1")) // Indigo
                setStroke(6, Color.parseColor("#8b5cf6")) // Purple border
            }
            
            setImageResource(android.R.drawable.ic_btn_speak_now)
            scaleType = android.widget.ImageView.ScaleType.CENTER
            elevation = 12f
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
        
        // Instructions
        val instructionText = TextView(this).apply {
            text = "Hold to speak • Natural conversation • Any form"
            textSize = 14f
            setTextColor(Color.parseColor("#9ca3af"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 0)
        }
        container.addView(instructionText)
        
        // Setup button
        setupButton = Button(this).apply {
            text = "⚙️ Setup API Keys"
            textSize = 16f
            setTextColor(Color.WHITE)
            
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 25f
                setColor(Color.parseColor("#f59e0b")) // Orange
            }
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 50, 0, 0)
            }
            
            setPadding(40, 20, 40, 20)
            
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
        updateStatus("Demo mode - Hold button to try")
        mainButton.isEnabled = true
        isSetup = true
        setupButton.visibility = Button.GONE
        
        // Initialize TTS for demo
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.setLanguage(Locale.US)
                speak("Demo mode ready! This simulates the real experience.")
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
                speak("VoiceBridge ready! I understand any accent and can help with forms.")
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
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
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
            // Demo mode
            val demoResponses = listOf(
                "I can help you fill out forms",
                "Point your camera at any form",
                "I understand any accent",
                "Just speak naturally"
            )
            val response = demoResponses.random()
            updateStatus("You: Demo input")
            speak(response)
        }
    }
    
    private suspend fun processUserInput(userInput: String) {
        updateStatus("You: $userInput")
        
        try {
            // Use Claude for natural conversation
            val response = claudeAPI.chatAboutForm(
                userMessage = userInput,
                conversationHistory = conversationHistory
            )
            
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
                updateStatus("Permissions granted!")
            } else {
                updateStatus("Permissions needed for voice & camera")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.shutdown()
        if (APIConfig.hasSpeechAPI() && ::whisperAPI.isInitialized) {
            whisperAPI.cleanup()
        }
    }
}