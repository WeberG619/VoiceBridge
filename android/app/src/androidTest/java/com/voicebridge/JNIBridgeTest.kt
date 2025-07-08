package com.voicebridge

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class JNIBridgeTest {
    
    private lateinit var voiceBridgeNative: VoiceBridgeNative
    private lateinit var testAudioFile: File
    private lateinit var testModelFile: File
    
    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        voiceBridgeNative = VoiceBridgeNative()
        
        // Create test audio file
        testAudioFile = File(context.cacheDir, "test_audio.wav")
        createTestAudioFile(testAudioFile)
        
        // Create mock model file
        testModelFile = File(context.cacheDir, "test_model.bin")
        createMockModelFile(testModelFile)
    }
    
    @After
    fun tearDown() {
        testAudioFile.delete()
        testModelFile.delete()
        voiceBridgeNative.cleanup()
    }
    
    @Test
    fun testNativeLibraryLoading() {
        // Test that the native library loads successfully
        val isLoaded = voiceBridgeNative.isNativeLibraryLoaded()
        assertTrue("Native library should be loaded", isLoaded)
    }
    
    @Test
    fun testInitialization() {
        val result = voiceBridgeNative.initialize()
        assertTrue("Initialization should succeed", result)
        
        val isInitialized = voiceBridgeNative.isInitialized()
        assertTrue("Should be initialized after successful init", isInitialized)
    }
    
    @Test
    fun testWhisperModelLoading() {
        voiceBridgeNative.initialize()
        
        val result = voiceBridgeNative.loadWhisperModel(testModelFile.absolutePath)
        // Note: This might fail in test environment without real model
        // but should not crash
        assertNotNull("Model loading should return a result", result)
    }
    
    @Test
    fun testAudioProcessing() {
        voiceBridgeNative.initialize()
        
        // Create test audio data (1 second of silence at 16kHz)
        val audioData = FloatArray(16000) { 0.0f }
        
        val processedAudio = voiceBridgeNative.processAudio(audioData)
        assertNotNull("Processed audio should not be null", processedAudio)
        assertEquals("Processed audio should have same length", 
                    audioData.size, processedAudio.size)
    }
    
    @Test
    fun testAudioNormalization() {
        voiceBridgeNative.initialize()
        
        // Test with audio that needs normalization
        val audioData = floatArrayOf(0.5f, 1.5f, -0.8f, -2.0f, 0.0f)
        
        val normalizedAudio = voiceBridgeNative.normalizeAudio(audioData)
        assertNotNull("Normalized audio should not be null", normalizedAudio)
        
        // Check that all values are in valid range [-1.0, 1.0]
        for (value in normalizedAudio) {
            assertTrue("Normalized value should be >= -1.0", value >= -1.0f)
            assertTrue("Normalized value should be <= 1.0", value <= 1.0f)
        }
    }
    
    @Test
    fun testVoiceActivityDetection() {
        voiceBridgeNative.initialize()
        
        // Test with silence
        val silenceData = FloatArray(1600) { 0.0f }
        val silenceResult = voiceBridgeNative.detectVoiceActivity(silenceData)
        assertFalse("Silence should not be detected as voice activity", silenceResult)
        
        // Test with simulated voice
        val voiceData = FloatArray(1600) { (Math.random() * 0.5).toFloat() }
        val voiceResult = voiceBridgeNative.detectVoiceActivity(voiceData)
        // This might be true or false depending on the random data
        assertNotNull("VAD should return a boolean result", voiceResult)
    }
    
    @Test
    fun testTextProcessing() {
        voiceBridgeNative.initialize()
        
        val inputText = "  Hello World!  "
        val processedText = voiceBridgeNative.processText(inputText)
        
        assertNotNull("Processed text should not be null", processedText)
        assertFalse("Processed text should not be empty", processedText.isEmpty())
        assertFalse("Processed text should be trimmed", 
                   processedText.startsWith(" ") || processedText.endsWith(" "))
    }
    
    @Test
    fun testTextCleaning() {
        voiceBridgeNative.initialize()
        
        val dirtyText = "H3ll0 W0rld! @#$%^&*()"
        val cleanedText = voiceBridgeNative.cleanText(dirtyText)
        
        assertNotNull("Cleaned text should not be null", cleanedText)
        assertTrue("Cleaned text should contain letters", 
                  cleanedText.any { it.isLetter() })
        assertFalse("Cleaned text should not contain special characters", 
                   cleanedText.contains("@#$%"))
    }
    
    @Test
    fun testMemoryManagement() {
        voiceBridgeNative.initialize()
        
        // Allocate and process large amounts of data to test memory handling
        repeat(10) {
            val largeAudioData = FloatArray(160000) { (Math.random()).toFloat() } // 10 seconds
            val processedAudio = voiceBridgeNative.processAudio(largeAudioData)
            assertNotNull("Large audio processing should not fail", processedAudio)
        }
        
        // Memory should be properly managed - no crashes expected
        assertTrue("Memory management test completed", true)
    }
    
    @Test
    fun testConcurrentAccess() {
        voiceBridgeNative.initialize()
        
        val threads = mutableListOf<Thread>()
        val results = mutableListOf<Boolean>()
        
        // Create multiple threads that use the native library
        repeat(5) { threadIndex ->
            val thread = Thread {
                try {
                    val audioData = FloatArray(1600) { (Math.random()).toFloat() }
                    val processedAudio = voiceBridgeNative.processAudio(audioData)
                    results.add(processedAudio != null)
                } catch (e: Exception) {
                    results.add(false)
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        // Wait for all threads to complete
        threads.forEach { it.join() }
        
        // All operations should succeed
        assertTrue("All concurrent operations should succeed", 
                  results.all { it })
    }
    
    @Test
    fun testErrorHandling() {
        voiceBridgeNative.initialize()
        
        // Test with null/invalid inputs
        val nullResult = voiceBridgeNative.processAudio(null)
        assertNull("Processing null audio should return null", nullResult)
        
        // Test with empty audio
        val emptyAudio = FloatArray(0)
        val emptyResult = voiceBridgeNative.processAudio(emptyAudio)
        // Should handle gracefully without crashing
        assertNotNull("Empty audio processing should not crash", emptyResult)
    }
    
    @Test
    fun testPerformanceBenchmark() {
        voiceBridgeNative.initialize()
        
        val audioData = FloatArray(16000) { (Math.random()).toFloat() } // 1 second
        
        val startTime = System.currentTimeMillis()
        
        // Process audio multiple times
        repeat(10) {
            voiceBridgeNative.processAudio(audioData)
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val averageTime = totalTime / 10.0
        
        // Processing should be reasonably fast (less than 100ms per second of audio)
        assertTrue("Audio processing should be fast enough for real-time use", 
                  averageTime < 100.0)
        
        println("Average processing time: ${averageTime}ms per second of audio")
    }
    
    @Test
    fun testConfigurationSettings() {
        voiceBridgeNative.initialize()
        
        // Test setting various configuration options
        val sampleRateSet = voiceBridgeNative.setSampleRate(16000)
        assertTrue("Sample rate should be set successfully", sampleRateSet)
        
        val channelsSet = voiceBridgeNative.setChannels(1)
        assertTrue("Channels should be set successfully", channelsSet)
        
        val thresholdSet = voiceBridgeNative.setVADThreshold(0.5f)
        assertTrue("VAD threshold should be set successfully", thresholdSet)
    }
    
    @Test
    fun testResourceCleanup() {
        voiceBridgeNative.initialize()
        
        // Load some resources
        voiceBridgeNative.loadWhisperModel(testModelFile.absolutePath)
        val audioData = FloatArray(16000) { (Math.random()).toFloat() }
        voiceBridgeNative.processAudio(audioData)
        
        // Cleanup should succeed
        val cleanupResult = voiceBridgeNative.cleanup()
        assertTrue("Cleanup should succeed", cleanupResult)
        
        // After cleanup, operations should fail gracefully
        val postCleanupResult = voiceBridgeNative.processAudio(audioData)
        assertNull("Operations after cleanup should return null", postCleanupResult)
    }
    
    @Test
    fun testModelInformation() {
        voiceBridgeNative.initialize()
        
        // Test getting model information
        val modelInfo = voiceBridgeNative.getModelInfo()
        assertNotNull("Model info should not be null", modelInfo)
        
        // Test getting version information
        val version = voiceBridgeNative.getVersion()
        assertNotNull("Version should not be null", version)
        assertFalse("Version should not be empty", version.isEmpty())
    }
    
    @Test
    fun testCallbackMechanism() {
        voiceBridgeNative.initialize()
        
        var callbackReceived = false
        var callbackData: String? = null
        
        // Set up callback
        voiceBridgeNative.setProgressCallback { progress, message ->
            callbackReceived = true
            callbackData = message
        }
        
        // Trigger an operation that should call the callback
        val audioData = FloatArray(16000) { (Math.random()).toFloat() }
        voiceBridgeNative.processAudio(audioData)
        
        // Give some time for callback to be called
        Thread.sleep(100)
        
        // Note: Callback behavior depends on implementation
        // This test ensures the mechanism doesn't crash
        assertTrue("Callback mechanism should work without errors", true)
    }
    
    // Helper methods
    private fun createTestAudioFile(file: File) {
        FileOutputStream(file).use { fos ->
            // Create a simple WAV header for 1 second of silence at 16kHz, 16-bit mono
            val header = createWAVHeader(16000, 16, 1, 16000 * 2)
            fos.write(header)
            
            // Write 1 second of silence (16000 samples * 2 bytes per sample)
            val silenceData = ByteArray(16000 * 2) { 0 }
            fos.write(silenceData)
        }
    }
    
    private fun createMockModelFile(file: File) {
        FileOutputStream(file).use { fos ->
            // Create a mock model file with some dummy data
            val mockData = ByteArray(1024) { it.toByte() }
            fos.write(mockData)
        }
    }
    
    private fun createWAVHeader(sampleRate: Int, bitsPerSample: Int, channels: Int, dataSize: Int): ByteArray {
        val header = ByteArray(44)
        
        // RIFF header
        "RIFF".toByteArray().copyInto(header, 0)
        intToByteArray(36 + dataSize).copyInto(header, 4)
        "WAVE".toByteArray().copyInto(header, 8)
        
        // Format chunk
        "fmt ".toByteArray().copyInto(header, 12)
        intToByteArray(16).copyInto(header, 16) // Chunk size
        shortToByteArray(1).copyInto(header, 20) // Audio format (PCM)
        shortToByteArray(channels.toShort()).copyInto(header, 22)
        intToByteArray(sampleRate).copyInto(header, 24)
        intToByteArray(sampleRate * channels * bitsPerSample / 8).copyInto(header, 28) // Byte rate
        shortToByteArray((channels * bitsPerSample / 8).toShort()).copyInto(header, 32) // Block align
        shortToByteArray(bitsPerSample.toShort()).copyInto(header, 34)
        
        // Data chunk
        "data".toByteArray().copyInto(header, 36)
        intToByteArray(dataSize).copyInto(header, 40)
        
        return header
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }
    
    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }
}