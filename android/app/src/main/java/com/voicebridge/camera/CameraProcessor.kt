package com.voicebridge.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.voicebridge.ocr.OCRProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraProcessor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val ocrProcessor = OCRProcessor(context)
    
    private var isProcessing = false
    private var onTextDetected: ((String, Float) -> Unit)? = null
    private var onImageCaptured: ((Bitmap) -> Unit)? = null
    
    companion object {
        private const val TAG = "CameraProcessor"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
    
    suspend fun initialize(): Boolean {
        return try {
            // Initialize OCR processor
            if (!ocrProcessor.initialize()) {
                Log.e(TAG, "Failed to initialize OCR processor")
                return false
            }
            
            // Initialize camera
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()
            
            setupCamera()
            
            Log.i(TAG, "Camera processor initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing camera processor", e)
            false
        }
    }
    
    private fun setupCamera() {
        val cameraProvider = cameraProvider ?: return
        
        // Preview use case
        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(previewView.display.rotation)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image capture use case
        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(previewView.display.rotation)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        
        // Image analysis use case for real-time OCR
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(previewView.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, OCRAnalyzer())
            }
        
        // Select back camera as default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()
            
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )
            
            Log.i(TAG, "Camera setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }
    
    fun startRealtimeOCR(onTextDetected: (String, Float) -> Unit) {
        this.onTextDetected = onTextDetected
        isProcessing = true
        Log.i(TAG, "Started real-time OCR")
    }
    
    fun stopRealtimeOCR() {
        isProcessing = false
        onTextDetected = null
        Log.i(TAG, "Stopped real-time OCR")
    }
    
    fun captureImage(onImageCaptured: (Bitmap) -> Unit) {
        this.onImageCaptured = onImageCaptured
        
        val imageCapture = imageCapture ?: return
        
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            context.cacheDir.resolve("captured_image.jpg")
        ).build()
        
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(output.savedUri?.path)
                    bitmap?.let { onImageCaptured(it) }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exception)
                }
            }
        )
    }
    
    fun toggleFlash() {
        camera?.let { camera ->
            if (camera.cameraInfo.hasFlashUnit()) {
                camera.cameraControl.enableTorch(!camera.cameraInfo.torchState.value!!)
            }
        }
    }
    
    fun focusOnPoint(x: Float, y: Float) {
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        
        camera?.cameraControl?.startFocusAndMetering(action)
    }
    
    private inner class OCRAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            if (!isProcessing) {
                image.close()
                return
            }
            
            try {
                val bitmap = imageProxyToBitmap(image)
                bitmap?.let { bmp ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val (text, confidence) = ocrProcessor.extractTextWithConfidence(bmp)
                        
                        if (text.isNotEmpty() && confidence > 50) {
                            CoroutineScope(Dispatchers.Main).launch {
                                onTextDetected?.invoke(text, confidence)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing image", e)
            } finally {
                image.close()
            }
        }
        
        private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
            return try {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                
                // Rotate bitmap if needed
                val rotationDegrees = image.imageInfo.rotationDegrees
                if (rotationDegrees != 0) {
                    rotateImage(bitmap, rotationDegrees.toFloat())
                } else {
                    bitmap
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error converting ImageProxy to Bitmap", e)
                null
            }
        }
        
        private fun rotateImage(bitmap: Bitmap, angle: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }
    
    fun getZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
    }
    
    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }
    
    fun getMaxZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
    }
    
    fun shutdown() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        ocrProcessor.close()
    }
}