package com.voicebridge.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class OCRProcessor(private val context: Context) {
    
    private var tessApi: TessBaseAPI? = null
    private val dataPath = "${context.filesDir}/tesseract/"
    
    companion object {
        private const val TAG = "OCRProcessor"
        private const val TESSDATA_FOLDER = "tessdata"
        private const val LANGUAGE = "eng"
        private const val TRAINED_DATA_FILE = "eng.traineddata"
    }
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Create tessdata directory
            val tessDataDir = File(dataPath, TESSDATA_FOLDER)
            if (!tessDataDir.exists()) {
                tessDataDir.mkdirs()
            }
            
            // Copy trained data file from assets
            val trainedDataFile = File(tessDataDir, TRAINED_DATA_FILE)
            if (!trainedDataFile.exists()) {
                copyTrainedDataFromAssets(trainedDataFile)
            }
            
            // Initialize TessBaseAPI
            tessApi = TessBaseAPI().apply {
                val initialized = init(dataPath, LANGUAGE)
                if (!initialized) {
                    Log.e(TAG, "Failed to initialize Tesseract")
                    return@withContext false
                }
                
                // Set page segmentation mode for better accuracy
                setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                
                // Set OCR Engine Mode
                setOcrEngineMode(TessBaseAPI.OcrEngineMode.OEM_TESSERACT_LSTM_COMBINED)
            }
            
            Log.i(TAG, "OCR initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing OCR", e)
            false
        }
    }
    
    private fun copyTrainedDataFromAssets(destinationFile: File) {
        try {
            context.assets.open("$TESSDATA_FOLDER/$TRAINED_DATA_FILE").use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.i(TAG, "Trained data copied successfully")
        } catch (e: IOException) {
            Log.e(TAG, "Error copying trained data", e)
            throw e
        }
    }
    
    suspend fun extractText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val api = tessApi ?: return@withContext ""
            
            // Preprocess bitmap for better OCR accuracy
            val preprocessedBitmap = preprocessBitmap(bitmap)
            
            // Set the bitmap for OCR
            api.setImage(preprocessedBitmap)
            
            // Get OCR result
            val result = api.utF8Text ?: ""
            
            // Clean up the result
            val cleanedResult = cleanOCRText(result)
            
            Log.d(TAG, "OCR extracted text: $cleanedResult")
            cleanedResult
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text", e)
            ""
        }
    }
    
    suspend fun extractTextWithConfidence(bitmap: Bitmap): Pair<String, Float> = withContext(Dispatchers.IO) {
        try {
            val api = tessApi ?: return@withContext Pair("", 0f)
            
            val preprocessedBitmap = preprocessBitmap(bitmap)
            api.setImage(preprocessedBitmap)
            
            val text = api.utF8Text ?: ""
            val confidence = api.meanConfidence()
            
            val cleanedText = cleanOCRText(text)
            
            Log.d(TAG, "OCR text: $cleanedText, confidence: $confidence")
            Pair(cleanedText, confidence)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text with confidence", e)
            Pair("", 0f)
        }
    }
    
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        // Convert to grayscale and apply basic preprocessing
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Convert to grayscale
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            pixels[i] = (0xff shl 24) or (gray shl 16) or (gray shl 8) or gray
        }
        
        // Apply threshold to create binary image
        val threshold = 128
        for (i in pixels.indices) {
            val gray = pixels[i] and 0xff
            pixels[i] = if (gray > threshold) 0xffffffff.toInt() else 0xff000000.toInt()
        }
        
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
    
    private fun cleanOCRText(text: String): String {
        return text
            .replace("\n\n+".toRegex(), "\n") // Remove multiple newlines
            .replace("\\s+".toRegex(), " ") // Replace multiple spaces with single space
            .trim()
    }
    
    fun rotateImage(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    fun detectTextRegions(bitmap: Bitmap): List<OCRRegion> {
        return try {
            val api = tessApi ?: return emptyList()
            
            val preprocessedBitmap = preprocessBitmap(bitmap)
            api.setImage(preprocessedBitmap)
            
            val regions = mutableListOf<OCRRegion>()
            
            // Get word bounding boxes
            val words = api.getWords()
            val confidences = api.getWordConfidences()
            
            words.resultIterator?.let { iterator ->
                var wordIndex = 0
                do {
                    val word = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                    val boundingBox = iterator.getBoundingBox(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                    val confidence = if (wordIndex < confidences.size) confidences[wordIndex] else 0
                    
                    if (word != null && boundingBox != null) {
                        regions.add(
                            OCRRegion(
                                text = word,
                                boundingBox = boundingBox,
                                confidence = confidence
                            )
                        )
                    }
                    wordIndex++
                } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
            }
            
            regions
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting text regions", e)
            emptyList()
        }
    }
    
    fun close() {
        tessApi?.end()
        tessApi = null
    }
}

data class OCRRegion(
    val text: String,
    val boundingBox: android.graphics.Rect,
    val confidence: Int
)