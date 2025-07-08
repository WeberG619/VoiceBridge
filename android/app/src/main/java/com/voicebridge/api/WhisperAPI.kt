package com.voicebridge.api

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * OpenAI Whisper API Integration
 * Excellent for accent recognition and multiple languages
 * Cost: $0.006 per minute (very affordable)
 */
class WhisperAPI(private val apiKey: String, private val context: Context) {
    
    companion object {
        private const val TAG = "WhisperAPI"
        private const val BASE_URL = "https://api.openai.com/v1/audio/transcriptions"
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            chain.proceed(request)
        }
        .build()
    
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    
    /**
     * Start recording audio for speech recognition
     */
    fun startRecording(): Boolean {
        try {
            // Create temporary audio file
            audioFile = File(context.cacheDir, "voice_recording_${System.currentTimeMillis()}.m4a")
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                
                prepare()
                start()
            }
            
            Log.d(TAG, "Started recording audio")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            return false
        }
    }
    
    /**
     * Stop recording and transcribe with Whisper
     */
    suspend fun stopRecordingAndTranscribe(): String = withContext(Dispatchers.IO) {
        
        try {
            // Stop recording
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            val file = audioFile ?: return@withContext ""
            
            if (!file.exists() || file.length() == 0L) {
                Log.w(TAG, "Audio file is empty or doesn't exist")
                return@withContext ""
            }
            
            Log.d(TAG, "Audio file size: ${file.length()} bytes")
            
            // Send to Whisper API
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("audio/m4a".toMediaType())
                )
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("language", "en") // Can be auto-detected
                .addFormDataPart("response_format", "json")
                .build()
            
            val request = Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "{}")
                val transcription = jsonResponse.optString("text", "")
                
                Log.d(TAG, "Whisper transcription: $transcription")
                
                // Clean up audio file
                file.delete()
                
                return@withContext transcription.trim()
                
            } else {
                Log.e(TAG, "Whisper API error: ${response.code} - ${response.message}")
                return@withContext ""
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error calling Whisper API", e)
            return@withContext ""
        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing audio", e)
            return@withContext ""
        } finally {
            // Clean up
            mediaRecorder?.release()
            mediaRecorder = null
            audioFile?.delete()
        }
    }
    
    /**
     * Transcribe an existing audio file
     */
    suspend fun transcribeFile(audioFile: File): String = withContext(Dispatchers.IO) {
        
        try {
            if (!audioFile.exists()) {
                Log.w(TAG, "Audio file doesn't exist")
                return@withContext ""
            }
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/*".toMediaType())
                )
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("response_format", "json")
                .build()
            
            val request = Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "{}")
                val transcription = jsonResponse.optString("text", "")
                
                Log.d(TAG, "File transcription: $transcription")
                return@withContext transcription.trim()
                
            } else {
                Log.e(TAG, "Whisper API error: ${response.code}")
                return@withContext ""
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing file", e)
            return@withContext ""
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
            audioFile?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up", e)
        }
    }
}