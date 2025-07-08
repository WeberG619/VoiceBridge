package com.voicebridge.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Claude API Integration for Natural Conversation
 * Perfect for understanding accents and natural speech
 */
class ClaudeAPI(private val apiKey: String) {
    
    companion object {
        private const val TAG = "ClaudeAPI"
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-3-haiku-20240307" // Fast and affordable
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()
    
    private val gson = Gson()
    
    data class ClaudeRequest(
        val model: String,
        @SerializedName("max_tokens") val maxTokens: Int,
        val messages: List<Message>
    )
    
    data class Message(
        val role: String,
        val content: String
    )
    
    data class ClaudeResponse(
        val content: List<Content>,
        val usage: Usage?
    )
    
    data class Content(
        val text: String,
        val type: String
    )
    
    data class Usage(
        @SerializedName("input_tokens") val inputTokens: Int,
        @SerializedName("output_tokens") val outputTokens: Int
    )
    
    /**
     * Have a natural conversation with Claude about forms
     */
    suspend fun chatAboutForm(
        userMessage: String,
        formText: String? = null,
        conversationHistory: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        
        try {
            // Build conversation context
            val messages = mutableListOf<Message>()
            
            // System message for form assistance
            val systemPrompt = buildString {
                append("You are VoiceBridge AI, a helpful assistant that helps people fill out forms using voice commands. ")
                append("You speak naturally and clearly, like a human conversation. ")
                append("You understand people with different accents and speech patterns. ")
                append("Keep responses short, friendly, and actionable. ")
                if (formText != null) {
                    append("The user is looking at this form: $formText. ")
                    append("Help them fill it out step by step. ")
                }
                append("Always ask one question at a time and be encouraging.")
            }
            
            messages.add(Message("user", systemPrompt))
            
            // Add conversation history
            conversationHistory.forEach { history ->
                messages.add(Message("assistant", history))
            }
            
            // Add current user message
            messages.add(Message("user", userMessage))
            
            val request = ClaudeRequest(
                model = MODEL,
                maxTokens = 150, // Keep responses short and fast
                messages = messages
            )
            
            val requestBody = gson.toJson(request)
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .build()
            
            val response = client.newCall(httpRequest).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val claudeResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
                
                val aiResponse = claudeResponse.content.firstOrNull()?.text ?: "I'm here to help!"
                
                Log.d(TAG, "Claude response: $aiResponse")
                return@withContext aiResponse
                
            } else {
                Log.e(TAG, "Claude API error: ${response.code} - ${response.message}")
                return@withContext "I'm having trouble right now. Please try again."
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error calling Claude API", e)
            return@withContext "I can't connect right now. Please check your internet."
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Claude API", e)
            return@withContext "Something went wrong. Let me try to help you anyway."
        }
    }
    
    /**
     * Have a natural conversation with Claude about vision/sight assistance
     */
    suspend fun chatAboutVision(
        userMessage: String,
        sceneText: String? = null,
        conversationHistory: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        
        try {
            // Build conversation context
            val messages = mutableListOf<Message>()
            
            // System message for vision assistance
            val systemPrompt = buildString {
                append("You are VoiceBridge AI, a sight assistant helping people see and understand their environment. ")
                append("You describe what you see clearly and helpfully, like a caring friend. ")
                append("You help people with visual impairments, reading difficulties, and navigation. ")
                append("Be descriptive but concise. Focus on what's useful and important. ")
                append("Describe objects, people, text, obstacles, and surroundings naturally. ")
                if (sceneText != null && sceneText.isNotEmpty()) {
                    append("I can see this text in the image: '$sceneText'. ")
                }
                append("Answer their question about what you see directly and helpfully.")
            }
            
            messages.add(Message("user", systemPrompt))
            
            // Add conversation history (keep recent for context)
            conversationHistory.takeLast(6).forEach { history ->
                messages.add(Message("assistant", history))
            }
            
            // Add current user message
            messages.add(Message("user", userMessage))
            
            val request = ClaudeRequest(
                model = MODEL,
                maxTokens = 200, // Longer for vision descriptions
                messages = messages
            )
            
            val requestBody = gson.toJson(request)
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .build()
            
            val response = client.newCall(httpRequest).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val claudeResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
                
                val aiResponse = claudeResponse.content.firstOrNull()?.text ?: "I'm here to help you see!"
                
                Log.d(TAG, "Claude vision response: $aiResponse")
                return@withContext aiResponse
                
            } else {
                Log.e(TAG, "Claude API error: ${response.code} - ${response.message}")
                return@withContext "I'm having trouble seeing right now. Please try again."
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error calling Claude API", e)
            return@withContext "I can't connect to analyze what you're seeing. Please check your internet."
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Claude API", e)
            return@withContext "Something went wrong with my vision. Let me try again."
        }
    }
    
    /**
     * Analyze form text and suggest what to do next
     */
    suspend fun analyzeForm(formText: String): FormAnalysis = withContext(Dispatchers.IO) {
        
        val prompt = """
        Analyze this form text and identify what type of form it is and what fields need to be filled:
        
        $formText
        
        Respond in this format:
        Form Type: [type of form]
        Fields: [list main fields that need to be filled]
        Next Step: [what should the user do first]
        """.trimIndent()
        
        try {
            val response = chatAboutForm(prompt)
            
            // Parse the response into structured data
            val lines = response.split("\n")
            val formType = lines.find { it.startsWith("Form Type:") }?.substringAfter(":") ?.trim() ?: "Unknown Form"
            val fields = lines.find { it.startsWith("Fields:") }?.substringAfter(":") ?.trim() ?: ""
            val nextStep = lines.find { it.startsWith("Next Step:") }?.substringAfter(":") ?.trim() ?: "Let's start filling this out."
            
            return@withContext FormAnalysis(formType, fields, nextStep)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing form", e)
            return@withContext FormAnalysis("Form", "Let's fill this out together", "Tell me what information you'd like to enter")
        }
    }
    
    data class FormAnalysis(
        val formType: String,
        val fields: String,
        val nextStep: String
    )
}