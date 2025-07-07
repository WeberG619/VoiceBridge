package com.voicebridge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.voicebridge.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var voiceBridge: VoiceBridgeNative
    private lateinit var audioProcessor: AudioProcessor
    private lateinit var textProcessor: TextProcessor
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    private val PERMISSION_REQUEST_CODE = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize native components
        voiceBridge = VoiceBridgeNative()
        audioProcessor = AudioProcessor()
        textProcessor = TextProcessor()
        
        // Check and request permissions
        checkPermissions()
        
        // Set up UI listeners
        setupUI()
        
        // Initialize models
        initializeModels()
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
            startListening()
        }
        
        binding.btnStopListening.setOnClickListener {
            stopListening()
        }
        
        binding.btnProcessText.setOnClickListener {
            processCurrentText()
        }
    }
    
    private fun initializeModels() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Copy models from assets to internal storage if needed
                val whisperModelPath = copyAssetToInternalStorage("whisper-tiny-en.bin")
                val llamaModelPath = copyAssetToInternalStorage("llama-7b-q5.gguf")
                
                val whisperInitialized = voiceBridge.initializeWhisper(whisperModelPath)
                val llamaInitialized = voiceBridge.initializeLLaMA(llamaModelPath)
                
                withContext(Dispatchers.Main) {
                    if (whisperInitialized && llamaInitialized) {
                        binding.tvStatus.text = "Models initialized successfully"
                        binding.btnStartListening.isEnabled = true
                    } else {
                        binding.tvStatus.text = "Failed to initialize models"
                        Toast.makeText(this@MainActivity, "Model initialization failed", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = "Error initializing models: ${e.message}"
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
                throw RuntimeException("Failed to copy asset $fileName", e)
            }
        }
        
        return internalFile.absolutePath
    }
    
    private fun startListening() {
        binding.tvStatus.text = "Listening..."
        binding.btnStartListening.isEnabled = false
        binding.btnStopListening.isEnabled = true
        
        // TODO: Implement audio recording and real-time processing
        // For now, just simulate
        CoroutineScope(Dispatchers.IO).launch {
            // Simulate audio processing
            kotlinx.coroutines.delay(3000)
            
            // Simulate transcription
            val mockAudioData = FloatArray(16000) { 0.1f * kotlin.random.Random.nextFloat() }
            val transcription = voiceBridge.transcribeAudio(mockAudioData)
            
            withContext(Dispatchers.Main) {
                binding.tvTranscription.text = transcription.ifEmpty { "No speech detected" }
                binding.tvStatus.text = "Transcription complete"
                binding.btnStartListening.isEnabled = true
                binding.btnStopListening.isEnabled = false
            }
        }
    }
    
    private fun stopListening() {
        binding.tvStatus.text = "Stopped listening"
        binding.btnStartListening.isEnabled = true
        binding.btnStopListening.isEnabled = false
    }
    
    private fun processCurrentText() {
        val transcription = binding.tvTranscription.text.toString()
        if (transcription.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val processedText = voiceBridge.processText(transcription)
                val cleanedText = textProcessor.cleanText(processedText)
                val commands = textProcessor.extractCommands(cleanedText)
                
                withContext(Dispatchers.Main) {
                    binding.tvProcessedText.text = cleanedText
                    binding.tvCommands.text = "Commands: ${commands.joinToString(", ")}"
                }
            }
        }
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
                Toast.makeText(this, "Some permissions are required for the app to work", Toast.LENGTH_LONG).show()
            }
        }
    }
}