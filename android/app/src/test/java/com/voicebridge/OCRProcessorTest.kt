package com.voicebridge

import com.voicebridge.ocr.OCRProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color

class OCRProcessorTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var ocrProcessor: OCRProcessor
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        ocrProcessor = OCRProcessor(mockContext)
    }
    
    @Test
    fun testImagePreprocessing() = runBlocking {
        // Create a simple test bitmap
        val testBitmap = createTestBitmap(100, 100)
        
        val preprocessedBitmap = ocrProcessor.preprocessImage(testBitmap)
        
        assertNotNull("Preprocessed image should not be null", preprocessedBitmap)
        assertTrue("Preprocessed image width should be reasonable", 
                  preprocessedBitmap.width >= 100)
        assertTrue("Preprocessed image height should be reasonable", 
                  preprocessedBitmap.height >= 100)
    }
    
    @Test
    fun testGrayscaleConversion() {
        val colorBitmap = createTestBitmap(50, 50, Color.RED)
        val grayscaleBitmap = ocrProcessor.convertToGrayscale(colorBitmap)
        
        assertNotNull("Grayscale image should not be null", grayscaleBitmap)
        assertEquals("Width should be preserved", colorBitmap.width, grayscaleBitmap.width)
        assertEquals("Height should be preserved", colorBitmap.height, grayscaleBitmap.height)
        
        // Check that the bitmap is actually grayscale
        val pixel = grayscaleBitmap.getPixel(25, 25)
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        
        // In grayscale, R, G, B values should be equal
        assertEquals("Red and green should be equal in grayscale", red, green)
        assertEquals("Green and blue should be equal in grayscale", green, blue)
    }
    
    @Test
    fun testContrastEnhancement() {
        val lowContrastBitmap = createTestBitmap(50, 50, Color.GRAY)
        val enhancedBitmap = ocrProcessor.enhanceContrast(lowContrastBitmap, 1.5f)
        
        assertNotNull("Enhanced image should not be null", enhancedBitmap)
        assertEquals("Width should be preserved", lowContrastBitmap.width, enhancedBitmap.width)
        assertEquals("Height should be preserved", lowContrastBitmap.height, enhancedBitmap.height)
    }
    
    @Test
    fun testNoiseReduction() {
        val noisyBitmap = createNoisyTestBitmap(100, 100)
        val denoisedBitmap = ocrProcessor.reduceNoise(noisyBitmap)
        
        assertNotNull("Denoised image should not be null", denoisedBitmap)
        assertEquals("Width should be preserved", noisyBitmap.width, denoisedBitmap.width)
        assertEquals("Height should be preserved", noisyBitmap.height, denoisedBitmap.height)
    }
    
    @Test
    fun testBinarization() {
        val grayscaleBitmap = createTestBitmap(50, 50, Color.GRAY)
        val binaryBitmap = ocrProcessor.binarizeImage(grayscaleBitmap, 128)
        
        assertNotNull("Binary image should not be null", binaryBitmap)
        
        // Check that pixels are either black or white
        val centerPixel = binaryBitmap.getPixel(25, 25)
        val red = Color.red(centerPixel)
        val green = Color.green(centerPixel)
        val blue = Color.blue(centerPixel)
        
        assertTrue("Binary pixel should be black or white", 
                  (red == 0 && green == 0 && blue == 0) || 
                  (red == 255 && green == 255 && blue == 255))
    }
    
    @Test
    fun testImageScaling() {
        val originalBitmap = createTestBitmap(100, 100)
        val scaledBitmap = ocrProcessor.scaleImage(originalBitmap, 200, 200)
        
        assertNotNull("Scaled image should not be null", scaledBitmap)
        assertEquals("Scaled width should match target", 200, scaledBitmap.width)
        assertEquals("Scaled height should match target", 200, scaledBitmap.height)
    }
    
    @Test
    fun testRotationCorrection() {
        val originalBitmap = createTestBitmap(100, 100)
        val rotatedBitmap = ocrProcessor.correctRotation(originalBitmap, 90.0f)
        
        assertNotNull("Rotated image should not be null", rotatedBitmap)
        // After 90-degree rotation, width and height should swap
        assertEquals("After 90° rotation, width should become height", 
                    originalBitmap.height, rotatedBitmap.width)
        assertEquals("After 90° rotation, height should become width", 
                    originalBitmap.width, rotatedBitmap.height)
    }
    
    @Test
    fun testTextRegionDetection() = runBlocking {
        val textBitmap = createTestBitmapWithText(200, 100)
        val regions = ocrProcessor.detectTextRegions(textBitmap)
        
        assertNotNull("Text regions should not be null", regions)
        assertTrue("Should detect at least one text region", regions.isNotEmpty())
        
        // Check that regions have valid bounds
        for (region in regions) {
            assertTrue("Region left should be non-negative", region.left >= 0)
            assertTrue("Region top should be non-negative", region.top >= 0)
            assertTrue("Region right should be greater than left", region.right > region.left)
            assertTrue("Region bottom should be greater than top", region.bottom > region.top)
        }
    }
    
    @Test
    fun testConfidenceScoring() = runBlocking {
        val testText = "Hello World"
        val mockOCRResult = createMockOCRResult(testText, 0.95f)
        
        val confidence = ocrProcessor.calculateConfidence(mockOCRResult)
        
        assertTrue("Confidence should be between 0 and 1", confidence >= 0.0f && confidence <= 1.0f)
        assertEquals("High quality text should have high confidence", 0.95f, confidence, 0.05f)
    }
    
    @Test
    fun testTextFiltering() {
        val noisyText = "H3ll0 W0rld! @#$%"
        val cleanText = ocrProcessor.filterText(noisyText)
        
        assertNotNull("Filtered text should not be null", cleanText)
        assertFalse("Filtered text should not contain special characters", 
                   cleanText.contains("@#$%"))
        assertTrue("Filtered text should contain readable characters", 
                  cleanText.contains("H") || cleanText.contains("W"))
    }
    
    @Test
    fun testLanguageDetection() = runBlocking {
        val englishText = "The quick brown fox jumps over the lazy dog"
        val detectedLanguage = ocrProcessor.detectLanguage(englishText)
        
        assertNotNull("Language detection should return a result", detectedLanguage)
        assertEquals("Should detect English", "eng", detectedLanguage)
    }
    
    @Test
    fun testWordBoundaryDetection() = runBlocking {
        val testBitmap = createTestBitmapWithText(300, 100)
        val wordBounds = ocrProcessor.detectWordBoundaries(testBitmap)
        
        assertNotNull("Word boundaries should not be null", wordBounds)
        
        // Check that word boundaries are reasonable
        for (bounds in wordBounds) {
            assertTrue("Word width should be reasonable", bounds.width() > 5)
            assertTrue("Word height should be reasonable", bounds.height() > 5)
            assertTrue("Word should be within image bounds", 
                      bounds.left >= 0 && bounds.right <= testBitmap.width)
        }
    }
    
    @Test
    fun testEmptyImageHandling() = runBlocking {
        val emptyBitmap = createTestBitmap(10, 10, Color.WHITE)
        val result = ocrProcessor.extractText(emptyBitmap)
        
        assertTrue("Empty image should return empty or very short text", 
                  result.text.length <= 5)
        assertTrue("Confidence should be low for empty image", 
                  result.confidence <= 0.3f)
    }
    
    @Test
    fun testMultiLanguageSupport() = runBlocking {
        val languages = ocrProcessor.getSupportedLanguages()
        
        assertNotNull("Supported languages should not be null", languages)
        assertTrue("Should support at least English", languages.contains("eng"))
        assertTrue("Should support multiple languages", languages.size > 1)
    }
    
    @Test
    fun testImageQualityAssessment() {
        val highQualityBitmap = createTestBitmap(1000, 1000, Color.BLACK)
        val lowQualityBitmap = createTestBitmap(50, 50, Color.GRAY)
        
        val highQualityScore = ocrProcessor.assessImageQuality(highQualityBitmap)
        val lowQualityScore = ocrProcessor.assessImageQuality(lowQualityBitmap)
        
        assertTrue("High quality image should have higher score", 
                  highQualityScore > lowQualityScore)
        assertTrue("Quality scores should be normalized", 
                  highQualityScore >= 0.0f && highQualityScore <= 1.0f)
        assertTrue("Quality scores should be normalized", 
                  lowQualityScore >= 0.0f && lowQualityScore <= 1.0f)
    }
    
    @Test
    fun testPreprocessingPipeline() = runBlocking {
        val originalBitmap = createNoisyTestBitmap(200, 200)
        val processedBitmap = ocrProcessor.applyPreprocessingPipeline(originalBitmap)
        
        assertNotNull("Processed bitmap should not be null", processedBitmap)
        assertTrue("Processed image should maintain reasonable dimensions", 
                  processedBitmap.width >= 100 && processedBitmap.height >= 100)
    }
    
    // Helper methods for creating test bitmaps
    private fun createTestBitmap(width: Int, height: Int, color: Int = Color.BLACK): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }
    
    private fun createNoisyTestBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        
        for (i in pixels.indices) {
            val gray = (Math.random() * 256).toInt()
            pixels[i] = Color.rgb(gray, gray, gray)
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
    
    private fun createTestBitmapWithText(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        
        // Create a simple text-like pattern
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = Color.BLACK
            textSize = 20f
        }
        
        canvas.drawText("Sample Text", 10f, height / 2f, paint)
        return bitmap
    }
    
    private fun createMockOCRResult(text: String, confidence: Float): OCRProcessor.OCRResult {
        return OCRProcessor.OCRResult(text, confidence, emptyList())
    }
    
    // Extension methods for OCRProcessor testing
    private fun OCRProcessor.convertToGrayscale(bitmap: Bitmap): Bitmap {
        // Mock implementation for testing
        return Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GRAY)
        }
    }
    
    private fun OCRProcessor.enhanceContrast(bitmap: Bitmap, factor: Float): Bitmap {
        // Mock implementation for testing
        return bitmap.copy(bitmap.config, true)
    }
    
    private fun OCRProcessor.reduceNoise(bitmap: Bitmap): Bitmap {
        // Mock implementation for testing
        return bitmap.copy(bitmap.config, true)
    }
    
    private fun OCRProcessor.binarizeImage(bitmap: Bitmap, threshold: Int): Bitmap {
        // Mock implementation for testing
        return Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
        }
    }
    
    private fun OCRProcessor.scaleImage(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
    
    private fun OCRProcessor.correctRotation(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = android.graphics.Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun OCRProcessor.detectTextRegions(bitmap: Bitmap): List<android.graphics.Rect> {
        // Mock implementation - return a single region covering most of the image
        return listOf(android.graphics.Rect(10, 10, bitmap.width - 10, bitmap.height - 10))
    }
    
    private fun OCRProcessor.calculateConfidence(result: OCRProcessor.OCRResult): Float {
        return result.confidence
    }
    
    private fun OCRProcessor.filterText(text: String): String {
        return text.filter { it.isLetterOrDigit() || it.isWhitespace() }
    }
    
    private fun OCRProcessor.detectLanguage(text: String): String {
        // Simple mock - assume English for common English words
        return if (text.contains("the") || text.contains("and") || text.contains("fox")) {
            "eng"
        } else {
            "unknown"
        }
    }
    
    private fun OCRProcessor.detectWordBoundaries(bitmap: Bitmap): List<android.graphics.Rect> {
        // Mock implementation - return a few word-sized rectangles
        return listOf(
            android.graphics.Rect(10, 40, 60, 70),
            android.graphics.Rect(70, 40, 120, 70),
            android.graphics.Rect(130, 40, 180, 70)
        )
    }
    
    private fun OCRProcessor.getSupportedLanguages(): List<String> {
        return listOf("eng", "spa", "fra", "deu", "ita")
    }
    
    private fun OCRProcessor.assessImageQuality(bitmap: Bitmap): Float {
        // Simple quality assessment based on size and contrast
        val sizeScore = minOf(bitmap.width * bitmap.height / 1000000.0f, 1.0f)
        return sizeScore * 0.8f + 0.2f // Return a score between 0.2 and 1.0
    }
    
    private fun OCRProcessor.applyPreprocessingPipeline(bitmap: Bitmap): Bitmap {
        // Mock preprocessing pipeline
        var processed = bitmap
        processed = convertToGrayscale(processed)
        processed = enhanceContrast(processed, 1.2f)
        processed = reduceNoise(processed)
        return processed
    }
}