package com.voicebridge.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.voicebridge.api.ClaudeAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

/**
 * Multi-tiered Vision Language Model Service
 * 
 * Tier 0: ML Kit on-device (free, fast)
 * Tier 1: BLIP-2 ONNX captions (180MB, on-device)
 * Tier 2: LLaVA-1.6 Q&A (future)
 * Tier 3: Claude Vision fallback (paid)
 */
class VisionLLMService(
    private val context: Context,
    private val claudeAPI: ClaudeAPI
) {
    
    companion object {
        private const val TAG = "VisionLLMService"
        private const val CONFIDENCE_THRESHOLD = 0.6f
    }
    
    // ML Kit detectors (Tier 0)
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )
    
    // BLIP-2 ONNX session (Tier 1) - will be initialized later
    private var blipSession: BlipCaptionModel? = null
    
    data class VisionResult(
        val description: String,
        val confidence: Float,
        val tier: Int,
        val processingTime: Long
    )
    
    /**
     * Main vision analysis function - uses tiered approach
     */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        userQuery: String = "What do you see?"
    ): VisionResult = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Tier 0: Quick ML Kit analysis
            val mlKitResult = analyzeWithMLKit(bitmap)
            val processingTime = System.currentTimeMillis() - startTime
            
            // If ML Kit is confident enough, use it
            if (mlKitResult.confidence > CONFIDENCE_THRESHOLD) {
                Log.d(TAG, "✅ Tier 0 (ML Kit) handled query: $userQuery")
                return@withContext VisionResult(
                    description = mlKitResult.description,
                    confidence = mlKitResult.confidence,
                    tier = 0,
                    processingTime = processingTime
                )
            }
            
            // Tier 1: BLIP-2 captions (if available)
            blipSession?.let { blip ->
                val caption = blip.generateCaption(bitmap)
                if (caption.isNotBlank()) {
                    Log.d(TAG, "✅ Tier 1 (BLIP-2) handled query: $userQuery")
                    return@withContext VisionResult(
                        description = enhanceWithQuery(caption, userQuery),
                        confidence = 0.8f,
                        tier = 1,
                        processingTime = System.currentTimeMillis() - startTime
                    )
                }
            }
            
            // Tier 3: Claude Vision fallback
            Log.d(TAG, "⚠️ Falling back to Tier 3 (Claude Vision) for: $userQuery")
            val claudeResponse = claudeAPI.chatAboutVision(
                userMessage = userQuery,
                sceneText = mlKitResult.description,
                conversationHistory = emptyList()
            )
            
            return@withContext VisionResult(
                description = claudeResponse,
                confidence = 0.9f,
                tier = 3,
                processingTime = System.currentTimeMillis() - startTime
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in vision analysis", e)
            return@withContext VisionResult(
                description = "I'm having trouble analyzing this image. Please try again.",
                confidence = 0.0f,
                tier = -1,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Tier 0: ML Kit on-device analysis
     */
    private suspend fun analyzeWithMLKit(bitmap: Bitmap): VisionResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        
        try {
            // Run all ML Kit detectors in parallel
            val textResult = textRecognizer.process(image).await()
            val labelResults = imageLabeler.process(image).await()
            val objectResults = objectDetector.process(image).await()
            
            // Combine results
            val description = buildString {
                // Text recognition
                if (textResult.text.isNotBlank()) {
                    append("Text: ${textResult.text.trim()}")
                }
                
                // Object detection
                if (objectResults.isNotEmpty()) {
                    if (isNotEmpty()) append(" | ")
                    append("Objects: ")
                    objectResults.take(3).forEach { obj ->
                        obj.labels.firstOrNull()?.let { label ->
                            append("${label.text}(${(label.confidence * 100).toInt()}%) ")
                        }
                    }
                }
                
                // Image labeling
                if (labelResults.isNotEmpty()) {
                    if (isNotEmpty()) append(" | ")
                    append("Scene: ")
                    labelResults.take(3).forEach { label ->
                        append("${label.text}(${(label.confidence * 100).toInt()}%) ")
                    }
                }
            }
            
            // Calculate confidence based on results quality
            val confidence = calculateMLKitConfidence(textResult.text, labelResults.size, objectResults.size)
            
            return VisionResult(
                description = description.ifBlank { "I can see the image but couldn't identify specific details." },
                confidence = confidence,
                tier = 0,
                processingTime = 0L
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit analysis failed", e)
            return VisionResult(
                description = "On-device analysis failed",
                confidence = 0.0f,
                tier = 0,
                processingTime = 0L
            )
        }
    }
    
    /**
     * Calculate confidence score for ML Kit results
     */
    private fun calculateMLKitConfidence(text: String, labelCount: Int, objectCount: Int): Float {
        var confidence = 0.0f
        
        // Text recognition adds confidence
        if (text.isNotBlank()) {
            confidence += 0.4f
            if (text.length > 10) confidence += 0.2f
        }
        
        // Object detection adds confidence
        if (objectCount > 0) {
            confidence += 0.3f
            if (objectCount > 2) confidence += 0.1f
        }
        
        // Image labeling adds confidence
        if (labelCount > 0) {
            confidence += 0.2f
            if (labelCount > 2) confidence += 0.1f
        }
        
        return confidence.coerceAtMost(1.0f)
    }
    
    /**
     * Enhance simple captions with user query context
     */
    private fun enhanceWithQuery(caption: String, userQuery: String): String {
        return when {
            userQuery.lowercase().contains("medicine") -> 
                "I can see what appears to be a medicine or pharmaceutical product. $caption"
            userQuery.lowercase().contains("read") -> 
                "Reading the text I can see: $caption"
            userQuery.lowercase().contains("count") -> 
                "Looking at the quantities: $caption"
            else -> caption
        }
    }
    
    /**
     * Initialize BLIP-2 model (Tier 1)
     */
    suspend fun initializeBLIP2() {
        withContext(Dispatchers.IO) {
            try {
                // This will be implemented when we add BLIP-2 ONNX model
                Log.d(TAG, "BLIP-2 initialization placeholder")
                // blipSession = BlipCaptionModel(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize BLIP-2", e)
            }
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        textRecognizer.close()
        imageLabeler.close()
        objectDetector.close()
        blipSession?.close()
    }
}

/**
 * Placeholder for BLIP-2 ONNX model
 */
private class BlipCaptionModel(context: Context) {
    fun generateCaption(bitmap: Bitmap): String {
        // TODO: Implement BLIP-2 ONNX inference
        return ""
    }
    
    fun close() {
        // TODO: Cleanup ONNX session
    }
}