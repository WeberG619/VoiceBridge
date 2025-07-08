package com.voicebridge.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

class AudioRecorder(private val context: Context) {
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val audioDataChannel = Channel<FloatArray>(Channel.UNLIMITED)
    
    // Audio configuration
    private val sampleRate = 16000 // 16kHz for Whisper
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    // Voice activity detection parameters
    private val vadWindowSize = 480 // 30ms at 16kHz
    private val vadThreshold = 0.02f
    private val vadHangoverFrames = 16 // 480ms
    
    private var vadBuffer = mutableListOf<Float>()
    private var vadHangoverCount = 0
    private var isVoiceActive = false
    
    companion object {
        private const val TAG = "AudioRecorder"
        private const val SILENCE_THRESHOLD = 0.01f
        private const val ENERGY_THRESHOLD = 0.001f
    }
    
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun initialize(): Boolean {
        return try {
            if (!hasPermission()) {
                Log.e(TAG, "Audio recording permission not granted")
                return false
            }
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return false
            }
            
            Log.i(TAG, "AudioRecorder initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioRecorder", e)
            false
        }
    }
    
    fun startRecording(): Flow<AudioData> = flow {
        if (!initialize()) {
            throw IllegalStateException("AudioRecorder not initialized")
        }
        
        val audioRecord = audioRecord ?: return@flow
        
        try {
            audioRecord.startRecording()
            isRecording = true
            
            val buffer = ShortArray(bufferSize)
            val audioBuffer = mutableListOf<Short>()
            
            Log.i(TAG, "Started audio recording")
            
            while (isRecording) {
                val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                
                if (bytesRead > 0) {
                    // Convert to float array
                    val floatBuffer = FloatArray(bytesRead) { i ->
                        buffer[i] / 32768.0f
                    }
                    
                    // Apply voice activity detection
                    val vadResult = processVoiceActivityDetection(floatBuffer)
                    
                    // Calculate audio features
                    val energy = calculateEnergy(floatBuffer)
                    val rms = calculateRMS(floatBuffer)
                    val volume = calculateVolume(floatBuffer)
                    
                    // Add to buffer
                    audioBuffer.addAll(buffer.take(bytesRead).toList())
                    
                    // Emit audio data
                    emit(
                        AudioData(
                            samples = floatBuffer.copyOf(),
                            sampleRate = sampleRate,
                            isVoiceActive = vadResult.isVoiceDetected,
                            energy = energy,
                            rms = rms,
                            volume = volume,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    
                    // If we have enough samples for processing (1 second worth)
                    if (audioBuffer.size >= sampleRate) {
                        val processingChunk = audioBuffer.take(sampleRate).toShortArray()
                        audioBuffer.clear()
                        
                        // Send to processing if voice is active
                        if (vadResult.isVoiceDetected) {
                            val floatChunk = FloatArray(processingChunk.size) { i ->
                                processingChunk[i] / 32768.0f
                            }
                            audioDataChannel.trySend(floatChunk)
                        }
                    }
                }
                
                // Small delay to prevent overwhelming the system
                delay(10)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during recording", e)
            throw e
        }
    }
    
    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.i(TAG, "Stopped audio recording")
    }
    
    fun getAudioDataChannel(): Channel<FloatArray> = audioDataChannel
    
    private fun processVoiceActivityDetection(samples: FloatArray): VADResult {
        // Add samples to VAD buffer
        vadBuffer.addAll(samples.toList())
        
        // Process in windows
        var voiceDetected = false
        
        while (vadBuffer.size >= vadWindowSize) {
            val window = vadBuffer.take(vadWindowSize)
            vadBuffer = vadBuffer.drop(vadWindowSize).toMutableList()
            
            // Calculate energy for this window
            val energy = window.map { it * it }.average()
            
            // Voice activity detection
            if (energy > vadThreshold) {
                voiceDetected = true
                vadHangoverCount = vadHangoverFrames
            } else if (vadHangoverCount > 0) {
                vadHangoverCount--
                voiceDetected = vadHangoverCount > 0
            }
        }
        
        isVoiceActive = voiceDetected
        return VADResult(voiceDetected, isVoiceActive)
    }
    
    private fun calculateEnergy(samples: FloatArray): Float {
        return samples.map { it * it }.average().toFloat()
    }
    
    private fun calculateRMS(samples: FloatArray): Float {
        val meanSquare = samples.map { it * it }.average()
        return sqrt(meanSquare).toFloat()
    }
    
    private fun calculateVolume(samples: FloatArray): Float {
        val maxAmplitude = samples.map { abs(it) }.maxOrNull() ?: 0f
        return maxAmplitude * 100f // Convert to percentage
    }
    
    fun getAudioLevels(): AudioLevels {
        return AudioLevels(
            currentVolume = if (isVoiceActive) 75f else 10f,
            averageVolume = 45f,
            peakVolume = 85f,
            isVoiceActive = isVoiceActive
        )
    }
    
    fun release() {
        stopRecording()
        audioDataChannel.close()
    }
}

data class AudioData(
    val samples: FloatArray,
    val sampleRate: Int,
    val isVoiceActive: Boolean,
    val energy: Float,
    val rms: Float,
    val volume: Float,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as AudioData
        
        if (!samples.contentEquals(other.samples)) return false
        if (sampleRate != other.sampleRate) return false
        if (isVoiceActive != other.isVoiceActive) return false
        if (energy != other.energy) return false
        if (rms != other.rms) return false
        if (volume != other.volume) return false
        if (timestamp != other.timestamp) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + isVoiceActive.hashCode()
        result = 31 * result + energy.hashCode()
        result = 31 * result + rms.hashCode()
        result = 31 * result + volume.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

data class VADResult(
    val isVoiceDetected: Boolean,
    val isCurrentlyActive: Boolean
)

data class AudioLevels(
    val currentVolume: Float,
    val averageVolume: Float,
    val peakVolume: Float,
    val isVoiceActive: Boolean
)