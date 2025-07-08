package com.voicebridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.voicebridge.accessibility.VoiceBridgeAccessibilityService
import com.voicebridge.audio.AudioRecorder
import com.voicebridge.camera.CameraProcessor
import com.voicebridge.databinding.ActivityMainBinding
import com.voicebridge.ocr.OCRProcessor
import com.voicebridge.skills.SkillEngine
import com.voicebridge.skills.SkillExecutionResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var voiceBridge: VoiceBridgeNative
    private lateinit var audioProcessor: AudioProcessor
    private lateinit var textProcessor: TextProcessor
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var cameraProcessor: CameraProcessor
    private lateinit var ocrProcessor: OCRProcessor
    private lateinit var skillEngine: SkillEngine
    
    private var isListening = false
    private var isProcessingAudio = false
    private var currentSkillResult: SkillExecutionResult? = null
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    private val PERMISSION_REQUEST_CODE = 1001
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if onboarding is completed
        if (!isOnboardingCompleted()) {
            // Start onboarding activity
            startActivity(Intent(this, com.voicebridge.onboarding.OnboardingActivity::class.java))
            finish()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize components
        initializeComponents()
        
        // Check and request permissions
        checkPermissions()
        
        // Set up UI listeners
        setupUI()
        
        // Initialize all systems
        initializeSystems()
    }
    
    private fun isOnboardingCompleted(): Boolean {
        return getSharedPreferences("voicebridge_prefs", MODE_PRIVATE)
            .getBoolean("onboarding_completed", false)
    }
    
    private fun initializeComponents() {
        voiceBridge = VoiceBridgeNative()
        audioProcessor = AudioProcessor()
        textProcessor = TextProcessor()
        audioRecorder = AudioRecorder(this)
        ocrProcessor = OCRProcessor(this)
        skillEngine = SkillEngine(this)
        
        // Initialize camera processor with preview view
        val previewView = binding.previewView
        cameraProcessor = CameraProcessor(this, this, previewView)
    }
    
    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter {
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
    
    private fun setupUI() {
        binding.btnStartListening.setOnClickListener {
            toggleListening()
        }
        
        binding.btnCapture.setOnClickListener {
            captureAndProcessImage()
        }
        
        binding.btnProcessText.setOnClickListener {
            processCurrentText()
        }
        
        binding.btnFillForm.setOnClickListener {
            fillForm()
        }
        
        binding.btnAccessibilitySettings.setOnClickListener {
            openAccessibilitySettings()
        }
    }
    
    private fun initializeSystems() {
        lifecycleScope.launch {
            try {
                updateStatus("Initializing systems...")
                
                // Initialize OCR
                if (!ocrProcessor.initialize()) {
                    showError("Failed to initialize OCR")
                    return@launch
                }
                
                // Initialize camera
                if (!cameraProcessor.initialize()) {
                    showError("Failed to initialize camera")
                    return@launch
                }
                
                // Initialize skill engine
                if (!skillEngine.initialize()) {
                    showError("Failed to initialize skill engine")
                    return@launch
                }
                
                // Initialize audio recorder
                if (!audioRecorder.initialize()) {
                    showError("Failed to initialize audio recorder")
                    return@launch
                }
                
                // Initialize native models
                initializeModels()
                
                updateStatus("All systems initialized successfully")
                enableUI()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing systems", e)
                showError("System initialization failed: ${e.message}")
            }
        }
    }
    
    private suspend fun initializeModels() {
        try {
            // Copy models from assets to internal storage if needed
            val whisperModelPath = copyAssetToInternalStorage("whisper-tiny-en.bin")
            val llamaModelPath = copyAssetToInternalStorage("llama-7b-q5.gguf")
            
            val whisperInitialized = voiceBridge.initializeWhisper(whisperModelPath)
            val llamaInitialized = voiceBridge.initializeLLaMA(llamaModelPath)
            
            if (whisperInitialized && llamaInitialized) {
                updateStatus("Models initialized successfully")
            } else {
                updateStatus("Warning: Some models failed to initialize")
                Log.w(TAG, "Whisper: $whisperInitialized, LLaMA: $llamaInitialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing models", e)
            updateStatus("Model initialization failed - running in limited mode")
        }
    }
    
    private fun copyAssetToInternalStorage(fileName: String): String {
        val internalFile = File(filesDir, fileName)
        
        if (!internalFile.exists()) {
            try {
                assets.open(fileName).use { inputStream ->
                    internalFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) {
                // Create placeholder file if asset doesn't exist
                Log.w(TAG, "Asset $fileName not found, creating placeholder")
                internalFile.writeText("placeholder")
            }
        }
        
        return internalFile.absolutePath
    }
    
    private fun toggleListening() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }
    
    private fun startListening() {
        if (!audioRecorder.hasPermission()) {
            showError("Microphone permission required")
            return
        }
        
        isListening = true
        updateUI()
        updateStatus("Listening...")
        
        // Start audio recording flow
        audioRecorder.startRecording()
            .onEach { audioData ->
                // Update volume indicator
                binding.volumeIndicator.progress = audioData.volume.toInt()
                
                // Process audio if voice is detected
                if (audioData.isVoiceActive && !isProcessingAudio) {
                    processAudioData(audioData.samples)
                }
            }
            .launchIn(lifecycleScope)
    }
    
    private fun stopListening() {
        isListening = false
        audioRecorder.stopRecording()
        updateUI()
        updateStatus("Stopped listening")
    }
    
    private fun processAudioData(audioSamples: FloatArray) {
        if (isProcessingAudio) return
        
        isProcessingAudio = true
        lifecycleScope.launch {
            try {
                // Transcribe audio using Whisper
                val transcription = voiceBridge.transcribeAudio(audioSamples)
                
                if (transcription.isNotEmpty()) {
                    binding.tvTranscription.text = transcription
                    
                    // Check if this matches a skill command
                    val skill = skillEngine.findSkillByCommand(transcription)
                    if (skill != null) {
                        updateStatus("Skill detected: ${skill.name}")
                        processSkillCommand(skill, transcription)
                    } else {
                        // Process as general text
                        processText(transcription)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio", e)
                showError("Audio processing failed")
            } finally {
                isProcessingAudio = false
            }
        }
    }
    
    private suspend fun processSkillCommand(skill: com.voicebridge.skills.Skill, command: String) {
        try {
            // For now, use placeholder data - in full implementation, this would
            // involve a conversation flow to collect all required fields
            val mockUserInputs = mapOf(
                "full_name" to "John Doe",
                "phone_number" to "555-123-4567",
                "email_address" to "john.doe@example.com"
            )
            
            val result = skillEngine.executeSkill(skill, mockUserInputs)
            currentSkillResult = result
            
            if (result.success) {
                binding.tvProcessedText.text = "Skill executed: ${skill.name}\n" +
                        "Processed ${result.processedData.size} fields"
                binding.btnFillForm.isEnabled = true
                updateStatus("Skill ready for form filling")
            } else {
                binding.tvProcessedText.text = "Skill errors:\n${result.errors.joinToString("\n")}"
                showError("Skill execution failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing skill command", e)
            showError("Skill processing failed")
        }
    }
    
    private fun captureAndProcessImage() {
        cameraProcessor.captureImage { bitmap ->
            lifecycleScope.launch {
                try {
                    updateStatus("Processing image...")
                    val (extractedText, confidence) = ocrProcessor.extractTextWithConfidence(bitmap)
                    
                    if (extractedText.isNotEmpty()) {
                        binding.tvOcrResult.text = "OCR Result (${confidence.toInt()}% confidence):\n$extractedText"
                        
                        // Check if this text matches any skill patterns
                        val skill = skillEngine.findSkillByCommand(extractedText)
                        if (skill != null) {
                            updateStatus("Document skill detected: ${skill.name}")
                        }
                    } else {
                        binding.tvOcrResult.text = "No text detected"
                    }
                    
                    updateStatus("Image processing complete")
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing image", e)
                    showError("Image processing failed")
                }
            }
        }
    }
    
    private fun processCurrentText() {
        val transcription = binding.tvTranscription.text.toString()
        if (transcription.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val processedText = voiceBridge.processText(transcription)
                    val cleanedText = textProcessor.cleanText(processedText)
                    val commands = textProcessor.extractCommands(cleanedText)
                    
                    binding.tvProcessedText.text = cleanedText
                    binding.tvCommands.text = "Commands: ${commands.joinToString(", ")}"
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing text", e)
                    showError("Text processing failed")
                }
            }
        }
    }
    
    private fun processText(text: String) {
        lifecycleScope.launch {
            try {
                val processedText = voiceBridge.processText(text)
                val cleanedText = textProcessor.cleanText(processedText)
                val commands = textProcessor.extractCommands(cleanedText)
                
                binding.tvProcessedText.text = cleanedText
                binding.tvCommands.text = "Commands detected: ${commands.joinToString(", ")}"
            } catch (e: Exception) {
                Log.e(TAG, "Error processing text", e)
            }
        }
    }
    
    private fun fillForm() {
        val skillResult = currentSkillResult
        if (skillResult == null) {
            showError("No skill result available for form filling")
            return
        }
        
        val accessibilityService = VoiceBridgeAccessibilityService.instance
        if (accessibilityService == null) {
            showError("Accessibility service not enabled")
            openAccessibilitySettings()
            return
        }
        
        accessibilityService.setCurrentSkill(skillResult.skill, skillResult)
        accessibilityService.fillForm(skillResult)
        updateStatus("Form filling initiated")
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Enable VoiceBridge Accessibility Service", Toast.LENGTH_LONG).show()
    }
    
    private fun updateStatus(message: String) {
        binding.tvStatus.text = message
        Log.i(TAG, message)
    }
    
    private fun showError(message: String) {
        binding.tvStatus.text = "Error: $message"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, message)
    }
    
    private fun updateUI() {
        binding.btnStartListening.text = if (isListening) "Stop Listening" else "Start Listening"
        binding.btnStartListening.isEnabled = true
    }
    
    private fun enableUI() {
        binding.btnStartListening.isEnabled = true
        binding.btnCapture.isEnabled = true
        binding.btnProcessText.isEnabled = true
        binding.btnAccessibilitySettings.isEnabled = true
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allPermissionsGranted) {
                showError("Some permissions are required for the app to work")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isListening) {
            audioRecorder.stopRecording()
        }
        audioRecorder.release()
        cameraProcessor.shutdown()
        ocrProcessor.close()
    }
}