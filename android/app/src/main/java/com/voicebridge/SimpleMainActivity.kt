package com.voicebridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ONE-BUTTON VOICEBRIDGE - Simple as ChatGPT Voice Mode
 * 
 * Just one button:
 * - Tap to talk
 * - Shows camera preview
 * - Automatic form detection
 * - Voice responses
 */
class SimpleMainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "VoiceBridge"
        private const val PERMISSION_REQUEST_CODE = 100
    }
    
    // UI
    private lateinit var mainButton: ImageButton
    private lateinit var statusText: TextView
    private lateinit var container: LinearLayout
    
    // Core
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        createOneButtonUI()
        checkPermissions()
    }
    
    private fun createOneButtonUI() {
        // Main container with gradient
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            
            // Beautiful gradient background
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#1a1a2e"), // Dark blue
                    Color.parseColor("#16213e")  // Darker blue
                )
            )
        }
        
        // App title
        val titleText = TextView(this).apply {
            text = "VoiceBridge AI"
            textSize = 32f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 100, 0, 20)
        }
        container.addView(titleText)
        
        // Status text
        statusText = TextView(this).apply {
            text = "Tap to start"
            textSize = 18f
            setTextColor(Color.parseColor("#a0a0a0"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }
        container.addView(statusText)
        
        // ONE BIG BUTTON - Like ChatGPT
        mainButton = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300)
            
            // Circular gradient background
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#3b82f6")) // Blue
                setStroke(5, Color.parseColor("#60a5fa")) // Light blue border
            }
            
            // Microphone icon
            setImageResource(android.R.drawable.ic_btn_speak_now)
            scaleType = android.widget.ImageView.ScaleType.CENTER
            setPadding(50, 50, 50, 50)
            elevation = 10f
            
            // Touch handling - press and hold to talk
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startListening()
                        animateButtonPress(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        stopListening()
                        animateButtonPress(false)
                    }
                }
                true
            }
        }
        container.addView(mainButton)
        
        // Instructions
        val instructionText = TextView(this).apply {
            text = "Hold to speak • Point at forms • Get instant help"
            textSize = 14f
            setTextColor(Color.parseColor("#808080"))
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 0)
        }
        container.addView(instructionText)
        
        setContentView(container)
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
        } else {
            initialize()
        }
    }
    
    private fun initialize() {
        // Initialize TTS
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.setLanguage(Locale.US)
                speak("VoiceBridge ready. Hold the button and tell me what you need.")
            }
        }
        
        // Initialize Speech Recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
        }
        
        updateStatus("Ready")
    }
    
    private fun startListening() {
        isListening = true
        updateStatus("Listening...")
        
        // Visual feedback
        mainButton.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#ef4444")) // Red when listening
            setStroke(8, Color.parseColor("#f87171"))
        }
        
        // Start speech recognition
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer?.startListening(intent)
    }
    
    private fun stopListening() {
        isListening = false
        updateStatus("Processing...")
        
        // Visual feedback
        mainButton.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#3b82f6")) // Back to blue
            setStroke(5, Color.parseColor("#60a5fa"))
        }
        
        speechRecognizer?.stopListening()
    }
    
    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            updateStatus("Speak now...")
        }
        
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                processCommand(matches[0])
            }
        }
        
        override fun onError(error: Int) {
            updateStatus("Ready")
            speak("Sorry, I didn't catch that. Please try again.")
        }
        
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
    
    private fun processCommand(command: String) {
        updateStatus("\"$command\"")
        
        lifecycleScope.launch {
            when {
                command.contains("form", ignoreCase = true) || 
                command.contains("fill", ignoreCase = true) -> {
                    speak("I'll help you fill out the form. Point your camera at it.")
                    // TODO: Activate camera and OCR
                }
                
                command.contains("help", ignoreCase = true) -> {
                    speak("I can help you fill out forms. Just point your camera at any form and I'll guide you through it.")
                }
                
                command.contains("hello", ignoreCase = true) -> {
                    speak("Hello! I'm VoiceBridge, your AI assistant for forms.")
                }
                
                else -> {
                    speak("I can help you with forms. Just say 'fill out form' or point your camera at one.")
                }
            }
            
            updateStatus("Ready")
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
    
    private fun animateButtonPress(pressed: Boolean) {
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
                initialize()
            } else {
                updateStatus("Permissions required")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
}