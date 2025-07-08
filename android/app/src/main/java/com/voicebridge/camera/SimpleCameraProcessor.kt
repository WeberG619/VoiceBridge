package com.voicebridge.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Simple Camera Processor for VoiceBridge
 * Lightweight implementation focused on basic camera functionality
 */
class SimpleCameraProcessor private constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "SimpleCameraProcessor"
        private const val REQUIRED_PERMISSIONS = Manifest.permission.CAMERA
        
        @Volatile
        private var INSTANCE: SimpleCameraProcessor? = null
        
        fun getInstance(context: Context): SimpleCameraProcessor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SimpleCameraProcessor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var onImageCapturedListener: ((Bitmap?) -> Unit)? = null
    private var onErrorListener: ((String) -> Unit)? = null
    
    /**
     * Check if camera permission is granted
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            REQUIRED_PERMISSIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Initialize camera
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        if (!hasPermission()) {
            Log.e(TAG, "Camera permission not granted")
            return@withContext false
        }
        
        try {
            // Get camera provider
            cameraProvider = getCameraProvider()
            
            // Configure camera use cases
            setupPreview()
            setupImageCapture()
            
            Log.i(TAG, "Camera processor initialized successfully")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize camera", e)
            return@withContext false
        }
    }
    
    /**
     * Start camera preview
     */
    suspend fun startPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider?
    ): Boolean = withContext(Dispatchers.Main) {
        try {
            val cameraProvider = this@SimpleCameraProcessor.cameraProvider ?: return@withContext false
            
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()
            
            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // Set surface provider for preview if available
            if (surfaceProvider != null) {
                preview?.setSurfaceProvider(surfaceProvider)
            }
            
            // Bind use cases to camera
            val useCases = mutableListOf<UseCase>().apply {
                preview?.let { add(it) }
                imageCapture?.let { add(it) }
            }
            
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )
            
            Log.i(TAG, "Camera preview started")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera preview", e)
            onErrorListener?.invoke("Failed to start camera: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Capture image
     */
    suspend fun captureImage(): Boolean = withContext(Dispatchers.IO) {
        try {
            val imageCapture = this@SimpleCameraProcessor.imageCapture ?: {
                Log.e(TAG, "ImageCapture not initialized")
                return@withContext false
            }()
            
            Log.d(TAG, "Starting image capture...")
            
            // Create temp file
            val outputFile = context.cacheDir.resolve("temp_capture_${System.currentTimeMillis()}.jpg")
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
            
            Log.d(TAG, "Output file: ${outputFile.absolutePath}")
            
            // Capture image
            val result = suspendCoroutine<Boolean> { continuation ->
                imageCapture.takePicture(
                    outputFileOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            try {
                                Log.d(TAG, "Image saved successfully")
                                val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                                if (bitmap != null) {
                                    Log.d(TAG, "Bitmap created successfully: ${bitmap.width}x${bitmap.height}")
                                    onImageCapturedListener?.invoke(bitmap)
                                } else {
                                    Log.e(TAG, "Failed to decode bitmap from file")
                                }
                                
                                // Clean up temp file
                                outputFile.delete()
                                
                                continuation.resume(true)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error loading captured image", e)
                                continuation.resume(false)
                            }
                        }
                        
                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Image capture failed", exception)
                            onErrorListener?.invoke("Image capture failed: ${exception.message}")
                            continuation.resume(false)
                        }
                    }
                )
            }
            
            return@withContext result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing image", e)
            onErrorListener?.invoke("Failed to capture image: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Set event listeners
     */
    fun setOnImageCapturedListener(listener: (Bitmap?) -> Unit) {
        onImageCapturedListener = listener
    }
    
    fun setOnErrorListener(listener: (String) -> Unit) {
        onErrorListener = listener
    }
    
    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(context).also { future ->
            future.addListener({
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(context))
        }
    }
    
    private fun setupPreview() {
        preview = Preview.Builder()
            .setTargetResolution(Size(1080, 1920))
            .build()
    }
    
    private fun setupImageCapture() {
        imageCapture = ImageCapture.Builder()
            .setTargetResolution(Size(1080, 1920))
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    
    /**
     * Stop camera and clean up resources
     */
    fun cleanup() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            
            Log.d(TAG, "Camera processor cleaned up")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up camera processor", e)
        }
    }
}