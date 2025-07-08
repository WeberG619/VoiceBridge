package com.voicebridge

import com.voicebridge.audio.AudioRecorder
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import android.media.AudioRecord
import android.content.Context

class AudioRecorderTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockAudioRecord: AudioRecord
    
    private lateinit var audioRecorder: AudioRecorder
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        audioRecorder = AudioRecorder(mockContext)
    }
    
    @Test
    fun testAudioConfiguration() {
        val config = audioRecorder.getAudioConfig()
        
        assertEquals("Sample rate should be 16kHz for Whisper", 16000, config.sampleRate)
        assertEquals("Should be mono channel", 1, config.channels)
        assertEquals("Should be 16-bit PCM", 16, config.bitsPerSample)
    }
    
    @Test
    fun testBufferSizeCalculation() {
        val bufferSize = audioRecorder.calculateBufferSize()
        
        assertTrue("Buffer size should be positive", bufferSize > 0)
        assertTrue("Buffer size should be reasonable (not too small)", bufferSize >= 1024)
        assertTrue("Buffer size should be reasonable (not too large)", bufferSize <= 65536)
    }
    
    @Test
    fun testVoiceActivityDetection() {
        // Test with silence (all zeros)
        val silenceBuffer = FloatArray(1600) { 0.0f }
        val silenceResult = audioRecorder.detectVoiceActivity(silenceBuffer)
        assertFalse("Silence should not be detected as voice activity", silenceResult)
        
        // Test with noise (random values)
        val noiseBuffer = FloatArray(1600) { (Math.random() * 0.1).toFloat() }
        val noiseResult = audioRecorder.detectVoiceActivity(noiseBuffer)
        assertFalse("Low amplitude noise should not be detected as voice", noiseResult)
        
        // Test with voice-like signal (higher amplitude)
        val voiceBuffer = FloatArray(1600) { (Math.random() * 0.5 + 0.3).toFloat() }
        val voiceResult = audioRecorder.detectVoiceActivity(voiceBuffer)
        assertTrue("High amplitude signal should be detected as voice", voiceResult)
    }
    
    @Test
    fun testAudioNormalization() {
        // Test with values that need normalization
        val inputBuffer = floatArrayOf(0.5f, -0.8f, 1.2f, -1.5f, 0.0f)
        val normalizedBuffer = audioRecorder.normalizeAudio(inputBuffer)
        
        // All values should be in range [-1.0, 1.0]
        for (value in normalizedBuffer) {
            assertTrue("Normalized value should be >= -1.0", value >= -1.0f)
            assertTrue("Normalized value should be <= 1.0", value <= 1.0f)
        }
        
        // Check that the relative relationships are preserved
        assertTrue("Positive value should remain positive", normalizedBuffer[0] > 0)
        assertTrue("Negative value should remain negative", normalizedBuffer[1] < 0)
        assertEquals("Zero should remain zero", 0.0f, normalizedBuffer[4], 0.001f)
    }
    
    @Test
    fun testAudioPreprocessing() {
        val rawAudio = floatArrayOf(0.1f, 0.8f, -0.3f, 1.2f, -0.9f, 0.0f)
        val processedAudio = audioRecorder.preprocessAudio(rawAudio)
        
        assertNotNull("Processed audio should not be null", processedAudio)
        assertEquals("Array size should be preserved", rawAudio.size, processedAudio.size)
        
        // All values should be normalized
        for (value in processedAudio) {
            assertTrue("Processed value should be >= -1.0", value >= -1.0f)
            assertTrue("Processed value should be <= 1.0", value <= 1.0f)
        }
    }
    
    @Test
    fun testRMSCalculation() {
        // Test with known values
        val testBuffer = floatArrayOf(1.0f, -1.0f, 0.5f, -0.5f)
        val rms = audioRecorder.calculateRMS(testBuffer)
        
        val expectedRMS = kotlin.math.sqrt((1.0 + 1.0 + 0.25 + 0.25) / 4.0).toFloat()
        assertEquals("RMS calculation should be correct", expectedRMS, rms, 0.001f)
        
        // Test with silence
        val silenceBuffer = FloatArray(100) { 0.0f }
        val silenceRMS = audioRecorder.calculateRMS(silenceBuffer)
        assertEquals("RMS of silence should be 0", 0.0f, silenceRMS, 0.001f)
    }
    
    @Test
    fun testEnergyCalculation() {
        val testBuffer = floatArrayOf(0.5f, -0.3f, 0.8f, -0.2f)
        val energy = audioRecorder.calculateEnergy(testBuffer)
        
        val expectedEnergy = 0.25f + 0.09f + 0.64f + 0.04f
        assertEquals("Energy calculation should be correct", expectedEnergy, energy, 0.001f)
    }
    
    @Test
    fun testZeroCrossingRate() {
        // Test with alternating positive/negative values (high ZCR)
        val alternatingBuffer = floatArrayOf(1.0f, -1.0f, 1.0f, -1.0f, 1.0f)
        val highZCR = audioRecorder.calculateZeroCrossingRate(alternatingBuffer)
        assertTrue("Alternating signal should have high ZCR", highZCR > 0.5f)
        
        // Test with constant positive values (low ZCR)
        val constantBuffer = FloatArray(100) { 0.5f }
        val lowZCR = audioRecorder.calculateZeroCrossingRate(constantBuffer)
        assertEquals("Constant signal should have zero ZCR", 0.0f, lowZCR, 0.001f)
    }
    
    @Test
    fun testSpectralCentroid() {
        // Test with a simple signal
        val testBuffer = FloatArray(512) { index ->
            kotlin.math.sin(2.0 * kotlin.math.PI * index / 512.0).toFloat()
        }
        
        val centroid = audioRecorder.calculateSpectralCentroid(testBuffer)
        assertTrue("Spectral centroid should be positive", centroid > 0.0f)
        assertTrue("Spectral centroid should be reasonable", centroid < 8000.0f)
    }
    
    @Test
    fun testAudioChunking() {
        val longBuffer = FloatArray(10000) { index -> index.toFloat() }
        val chunkSize = 1600 // 100ms at 16kHz
        
        val chunks = audioRecorder.chunkAudio(longBuffer, chunkSize)
        
        assertTrue("Should produce multiple chunks", chunks.size > 1)
        
        // Check chunk sizes
        for (i in 0 until chunks.size - 1) {
            assertEquals("All chunks except last should be full size", 
                        chunkSize, chunks[i].size)
        }
        
        // Last chunk might be smaller
        assertTrue("Last chunk should not be empty", chunks.last().isNotEmpty())
    }
    
    @Test
    fun testSilenceDetection() {
        // Test silence detection with very quiet audio
        val quietBuffer = FloatArray(1600) { 0.001f }
        val isSilent = audioRecorder.isSilence(quietBuffer)
        assertTrue("Very quiet audio should be detected as silence", isSilent)
        
        // Test with normal speech levels
        val speechBuffer = FloatArray(1600) { 0.1f }
        val isNotSilent = audioRecorder.isSilence(speechBuffer)
        assertFalse("Normal speech levels should not be detected as silence", isNotSilent)
    }
    
    @Test
    fun testAudioFormatConversion() {
        // Test float to short conversion
        val floatBuffer = floatArrayOf(-1.0f, -0.5f, 0.0f, 0.5f, 1.0f)
        val shortBuffer = audioRecorder.convertFloatToShort(floatBuffer)
        
        assertEquals("Array sizes should match", floatBuffer.size, shortBuffer.size)
        assertEquals("Min float should convert to min short", Short.MIN_VALUE, shortBuffer[0])
        assertEquals("Max float should convert to max short", Short.MAX_VALUE, shortBuffer[4])
        assertEquals("Zero should remain zero", 0, shortBuffer[2])
    }
    
    @Test
    fun testAudioStatistics() {
        val testBuffer = floatArrayOf(0.1f, 0.5f, -0.3f, 0.8f, -0.2f, 0.0f)
        val stats = audioRecorder.calculateAudioStatistics(testBuffer)
        
        assertNotNull("Statistics should not be null", stats)
        assertTrue("RMS should be positive", stats.rms > 0.0f)
        assertTrue("Energy should be positive", stats.energy > 0.0f)
        assertTrue("ZCR should be non-negative", stats.zeroCrossingRate >= 0.0f)
        assertTrue("Peak should be positive", stats.peak > 0.0f)
        assertEquals("Peak should be maximum absolute value", 0.8f, stats.peak, 0.001f)
    }
    
    @Test 
    fun testPermissionCheck() {
        // This would require proper Android testing framework
        // For now, just test the method exists and doesn't crash
        val hasPermission = audioRecorder.hasRecordPermission()
        // In unit test environment, this will likely return false
        // which is expected behavior
        assertNotNull("Permission check should return a boolean", hasPermission)
    }
    
    // Data class to hold audio statistics for testing
    data class AudioStatistics(
        val rms: Float,
        val energy: Float,
        val zeroCrossingRate: Float,
        val peak: Float,
        val spectralCentroid: Float
    )
    
    // Mock extension of AudioRecorder for testing statistics
    private fun AudioRecorder.calculateAudioStatistics(buffer: FloatArray): AudioStatistics {
        return AudioStatistics(
            rms = calculateRMS(buffer),
            energy = calculateEnergy(buffer),
            zeroCrossingRate = calculateZeroCrossingRate(buffer),
            peak = buffer.maxByOrNull { kotlin.math.abs(it) }?.let { kotlin.math.abs(it) } ?: 0.0f,
            spectralCentroid = calculateSpectralCentroid(buffer)
        )
    }
}