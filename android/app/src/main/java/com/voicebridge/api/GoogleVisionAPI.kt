package com.voicebridge.api

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.Base64

/**
 * Google Vision API Integration
 * FREE TIER: 1000 requests per month
 * Excellent for form text recognition
 */
class GoogleVisionAPI(private val apiKey: String) {
    
    companion object {
        private const val TAG = "GoogleVisionAPI"
        private const val BASE_URL = "https://vision.googleapis.com/v1/images:annotate"
    }
    
    private val client = OkHttpClient.Builder().build()
    private val gson = Gson()
    
    data class VisionRequest(
        val requests: List<AnnotateImageRequest>
    )
    
    data class AnnotateImageRequest(
        val image: Image,
        val features: List<Feature>
    )
    
    data class Image(
        val content: String // Base64 encoded image
    )
    
    data class Feature(
        val type: String,
        @SerializedName("maxResults") val maxResults: Int = 50
    )
    
    data class VisionResponse(
        val responses: List<AnnotateImageResponse>
    )
    
    data class AnnotateImageResponse(
        @SerializedName("textAnnotations") val textAnnotations: List<TextAnnotation>?,
        val error: ErrorInfo?
    )
    
    data class TextAnnotation(
        val description: String,
        @SerializedName("boundingPoly") val boundingPoly: BoundingPoly?
    )
    
    data class BoundingPoly(
        val vertices: List<Vertex>
    )
    
    data class Vertex(
        val x: Int?,
        val y: Int?
    )
    
    data class ErrorInfo(
        val code: Int,
        val message: String
    )
    
    /**
     * Extract text from image using Google Vision OCR
     */
    suspend fun extractTextFromImage(bitmap: Bitmap): OCRResult = withContext(Dispatchers.IO) {
        
        try {
            // Convert bitmap to base64
            val base64Image = bitmapToBase64(bitmap)
            
            // Prepare request
            val request = VisionRequest(
                requests = listOf(
                    AnnotateImageRequest(
                        image = Image(content = base64Image),
                        features = listOf(
                            Feature(type = "TEXT_DETECTION", maxResults = 50)
                        )
                    )
                )
            )
            
            val requestBody = gson.toJson(request)
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()
            
            val response = client.newCall(httpRequest).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val visionResponse = gson.fromJson(responseBody, VisionResponse::class.java)
                
                val firstResponse = visionResponse.responses.firstOrNull()
                
                if (firstResponse?.error != null) {
                    Log.e(TAG, "Vision API error: ${firstResponse.error.message}")
                    return@withContext OCRResult("", 0f, emptyList())
                }
                
                val textAnnotations = firstResponse?.textAnnotations ?: emptyList()
                
                if (textAnnotations.isNotEmpty()) {
                    // First annotation contains the full text
                    val fullText = textAnnotations[0].description
                    
                    // Extract individual fields for form analysis
                    val fields = extractFormFields(fullText)
                    
                    Log.d(TAG, "Extracted text: $fullText")
                    
                    return@withContext OCRResult(
                        text = fullText,
                        confidence = 0.95f, // Google Vision is very accurate
                        fields = fields
                    )
                } else {
                    Log.w(TAG, "No text detected in image")
                    return@withContext OCRResult("", 0f, emptyList())
                }
                
            } else {
                Log.e(TAG, "Vision API HTTP error: ${response.code}")
                return@withContext OCRResult("", 0f, emptyList())
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Vision API", e)
            return@withContext OCRResult("", 0f, emptyList())
        }
    }
    
    /**
     * Extract form fields from OCR text
     */
    private fun extractFormFields(text: String): List<FormField> {
        val fields = mutableListOf<FormField>()
        val lines = text.split("\n")
        
        // Common form field patterns
        val fieldPatterns = mapOf(
            "name" to listOf("name", "full name", "first name", "last name"),
            "email" to listOf("email", "e-mail", "email address"),
            "phone" to listOf("phone", "telephone", "mobile", "cell"),
            "address" to listOf("address", "street", "location"),
            "date" to listOf("date", "birth", "dob"),
            "ssn" to listOf("ssn", "social security", "social"),
            "signature" to listOf("signature", "sign", "signed")
        )
        
        for (line in lines) {
            val lowerLine = line.lowercase()
            
            for ((fieldType, patterns) in fieldPatterns) {
                if (patterns.any { pattern -> lowerLine.contains(pattern) }) {
                    // Check if line looks like it has a blank field
                    if (lowerLine.contains("_") || lowerLine.contains("[]") || 
                        lowerLine.contains("blank") || lowerLine.contains(":")) {
                        
                        fields.add(FormField(
                            type = fieldType,
                            label = line.trim(),
                            isEmpty = true
                        ))
                    }
                }
            }
        }
        
        return fields
    }
    
    /**
     * Convert bitmap to base64 string
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.getEncoder().encodeToString(imageBytes)
    }
    
    data class OCRResult(
        val text: String,
        val confidence: Float,
        val fields: List<FormField>
    )
    
    data class FormField(
        val type: String,
        val label: String,
        val isEmpty: Boolean
    )
}