package com.voicebridge.tools.screenshot

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Screenshot Generator for VoiceBridge Play Store Assets
 * 
 * Generates high-quality screenshots showcasing app features
 * for Google Play Store listing.
 */
class ScreenshotGenerator(private val context: Context) {
    
    companion object {
        const val PHONE_WIDTH = 1080
        const val PHONE_HEIGHT = 1920
        const val TABLET_WIDTH = 1920
        const val TABLET_HEIGHT = 1080
        const val CORNER_RADIUS = 40f
        const val STATUS_BAR_HEIGHT = 80
        const val NAVIGATION_BAR_HEIGHT = 120
    }
    
    private val primaryColor = Color.parseColor("#2196F3")
    private val secondaryColor = Color.parseColor("#03DAC6")
    private val backgroundColor = Color.parseColor("#FFFFFF")
    private val darkBackgroundColor = Color.parseColor("#121212")
    private val textColor = Color.parseColor("#000000")
    private val darkTextColor = Color.parseColor("#FFFFFF")
    
    data class ScreenshotConfig(
        val title: String,
        val subtitle: String,
        val features: List<String>,
        val isDarkMode: Boolean = false,
        val showMicrophone: Boolean = false,
        val showCamera: Boolean = false,
        val showForm: Boolean = false,
        val showAccessibility: Boolean = false
    )
    
    fun generateAllScreenshots(outputDir: File) {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        val phoneDir = File(outputDir, "phone")
        val tabletDir = File(outputDir, "tablet")
        phoneDir.mkdirs()
        tabletDir.mkdirs()
        
        // Generate phone screenshots
        generatePhoneScreenshots(phoneDir)
        
        // Generate tablet screenshots
        generateTabletScreenshots(tabletDir)
    }
    
    private fun generatePhoneScreenshots(outputDir: File) {
        val screenshots = listOf(
            ScreenshotConfig(
                title = "Voice-Powered Form Filling",
                subtitle = "Fill forms hands-free with natural speech",
                features = listOf(
                    "Tap to speak",
                    "AI understands context",
                    "Auto-fills form fields",
                    "Works offline"
                ),
                showMicrophone = true,
                showForm = true
            ),
            ScreenshotConfig(
                title = "Document Scanning & OCR",
                subtitle = "Extract text from any document instantly",
                features = listOf(
                    "Point and capture",
                    "Automatic text extraction",
                    "High accuracy OCR",
                    "No internet needed"
                ),
                showCamera = true
            ),
            ScreenshotConfig(
                title = "Smart Form Detection",
                subtitle = "Automatically identifies form fields",
                features = listOf(
                    "Works with any app",
                    "Intelligent field matching",
                    "Learns from usage",
                    "Privacy protected"
                ),
                showForm = true
            ),
            ScreenshotConfig(
                title = "Accessibility First",
                subtitle = "Designed for everyone",
                features = listOf(
                    "Screen reader support",
                    "High contrast mode",
                    "Large text options",
                    "Voice navigation"
                ),
                showAccessibility = true
            ),
            ScreenshotConfig(
                title = "Dark Mode Support",
                subtitle = "Comfortable viewing day or night",
                features = listOf(
                    "OLED optimized",
                    "Reduces eye strain",
                    "Battery efficient",
                    "Auto theme switching"
                ),
                isDarkMode = true,
                showMicrophone = true
            ),
            ScreenshotConfig(
                title = "Complete Privacy",
                subtitle = "Your data stays on your device",
                features = listOf(
                    "No cloud processing",
                    "No data collection",
                    "No accounts required",
                    "Open source"
                )
            )
        )
        
        screenshots.forEachIndexed { index, config ->
            val bitmap = generatePhoneScreenshot(config)
            saveScreenshot(bitmap, File(outputDir, "screenshot-${index + 1}.png"))
        }
    }
    
    private fun generateTabletScreenshots(outputDir: File) {
        val screenshots = listOf(
            ScreenshotConfig(
                title = "Optimized for Tablets",
                subtitle = "Enhanced productivity on larger screens",
                features = listOf(
                    "Split-screen support",
                    "Multi-column layouts",
                    "Landscape optimization",
                    "Keyboard shortcuts"
                ),
                showForm = true
            ),
            ScreenshotConfig(
                title = "Professional Form Processing",
                subtitle = "Handle complex forms with ease",
                features = listOf(
                    "Batch processing",
                    "Template library",
                    "Export capabilities",
                    "Team workflows"
                ),
                showForm = true
            )
        )
        
        screenshots.forEachIndexed { index, config ->
            val bitmap = generateTabletScreenshot(config)
            saveScreenshot(bitmap, File(outputDir, "tablet-screenshot-${index + 1}.png"))
        }
    }
    
    private fun generatePhoneScreenshot(config: ScreenshotConfig): Bitmap {
        val bitmap = Bitmap.createBitmap(PHONE_WIDTH, PHONE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        val bgColor = if (config.isDarkMode) darkBackgroundColor else backgroundColor
        canvas.drawColor(bgColor)
        
        // Draw status bar
        drawStatusBar(canvas, config.isDarkMode)
        
        // Draw app bar
        drawAppBar(canvas, "VoiceBridge", config.isDarkMode)
        
        // Draw main content
        drawPhoneContent(canvas, config)
        
        // Draw navigation bar
        drawNavigationBar(canvas, config.isDarkMode)
        
        return bitmap
    }
    
    private fun generateTabletScreenshot(config: ScreenshotConfig): Bitmap {
        val bitmap = Bitmap.createBitmap(TABLET_WIDTH, TABLET_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        canvas.drawColor(backgroundColor)
        
        // Draw tablet UI
        drawTabletContent(canvas, config)
        
        return bitmap
    }
    
    private fun drawStatusBar(canvas: Canvas, isDarkMode: Boolean) {
        val paint = Paint().apply {
            color = if (isDarkMode) Color.parseColor("#1F1F1F") else primaryColor
        }
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), STATUS_BAR_HEIGHT.toFloat(), paint)
        
        // Draw status icons
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("9:41 AM", canvas.width - 50f, 55f, textPaint)
    }
    
    private fun drawAppBar(canvas: Canvas, title: String, isDarkMode: Boolean) {
        val paint = Paint().apply {
            color = if (isDarkMode) Color.parseColor("#1F1F1F") else primaryColor
        }
        val top = STATUS_BAR_HEIGHT.toFloat()
        val bottom = top + 150f
        canvas.drawRect(0f, top, canvas.width.toFloat(), bottom, paint)
        
        // Draw title
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(title, canvas.width / 2f, top + 95f, textPaint)
    }
    
    private fun drawNavigationBar(canvas: Canvas, isDarkMode: Boolean) {
        val paint = Paint().apply {
            color = if (isDarkMode) Color.parseColor("#1F1F1F") else Color.parseColor("#F5F5F5")
        }
        val top = canvas.height - NAVIGATION_BAR_HEIGHT
        canvas.drawRect(0f, top.toFloat(), canvas.width.toFloat(), canvas.height.toFloat(), paint)
        
        // Draw navigation buttons
        val buttonPaint = Paint().apply {
            color = if (isDarkMode) Color.WHITE else Color.BLACK
            strokeWidth = 8f
            style = Paint.Style.STROKE
        }
        
        val centerY = top + NAVIGATION_BAR_HEIGHT / 2f
        val buttonSpacing = canvas.width / 4f
        
        // Back button
        canvas.drawLine(buttonSpacing - 30f, centerY, buttonSpacing + 30f, centerY - 30f, buttonPaint)
        canvas.drawLine(buttonSpacing - 30f, centerY, buttonSpacing + 30f, centerY + 30f, buttonPaint)
        
        // Home button
        canvas.drawCircle(canvas.width / 2f, centerY, 30f, buttonPaint)
        
        // Recent apps button
        canvas.drawRect(canvas.width - buttonSpacing - 30f, centerY - 25f, 
                       canvas.width - buttonSpacing + 30f, centerY + 25f, buttonPaint)
    }
    
    private fun drawPhoneContent(canvas: Canvas, config: ScreenshotConfig) {
        val contentTop = STATUS_BAR_HEIGHT + 150f + 50f
        val contentBottom = canvas.height - NAVIGATION_BAR_HEIGHT - 50f
        val padding = 50f
        
        var currentY = contentTop
        
        // Draw title
        val titlePaint = Paint().apply {
            color = if (config.isDarkMode) darkTextColor else textColor
            textSize = 72f
            typeface = Typeface.DEFAULT_BOLD
        }
        drawWrappedText(canvas, config.title, padding, currentY, canvas.width - padding * 2, titlePaint)
        currentY += 150f
        
        // Draw subtitle
        val subtitlePaint = Paint().apply {
            color = if (config.isDarkMode) Color.parseColor("#B0B0B0") else Color.parseColor("#666666")
            textSize = 48f
        }
        drawWrappedText(canvas, config.subtitle, padding, currentY, canvas.width - padding * 2, subtitlePaint)
        currentY += 150f
        
        // Draw feature cards
        config.features.forEach { feature ->
            drawFeatureCard(canvas, feature, padding, currentY, canvas.width - padding * 2, config.isDarkMode)
            currentY += 120f
        }
        
        // Draw UI elements based on config
        if (config.showMicrophone) {
            drawMicrophoneButton(canvas, currentY + 100f, config.isDarkMode)
        }
        if (config.showCamera) {
            drawCameraPreview(canvas, currentY + 100f, config.isDarkMode)
        }
        if (config.showForm) {
            drawFormFields(canvas, currentY + 100f, config.isDarkMode)
        }
        if (config.showAccessibility) {
            drawAccessibilityIcons(canvas, currentY + 100f, config.isDarkMode)
        }
    }
    
    private fun drawTabletContent(canvas: Canvas, config: ScreenshotConfig) {
        // Similar to phone but with different layout
        // Implementation simplified for brevity
        val titlePaint = Paint().apply {
            color = textColor
            textSize = 96f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(config.title, canvas.width / 2f, 200f, titlePaint)
    }
    
    private fun drawFeatureCard(canvas: Canvas, text: String, x: Float, y: Float, width: Float, isDarkMode: Boolean) {
        val cardPaint = Paint().apply {
            color = if (isDarkMode) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")
        }
        
        val rect = RectF(x, y, x + width, y + 100f)
        canvas.drawRoundRect(rect, 20f, 20f, cardPaint)
        
        // Draw checkmark
        val checkPaint = Paint().apply {
            color = secondaryColor
            textSize = 48f
        }
        canvas.drawText("✓", x + 30f, y + 65f, checkPaint)
        
        // Draw text
        val textPaint = Paint().apply {
            color = if (isDarkMode) darkTextColor else textColor
            textSize = 42f
        }
        canvas.drawText(text, x + 100f, y + 65f, textPaint)
    }
    
    private fun drawMicrophoneButton(canvas: Canvas, y: Float, isDarkMode: Boolean) {
        val centerX = canvas.width / 2f
        val centerY = y + 150f
        
        // Draw button background
        val bgPaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, 120f, bgPaint)
        
        // Draw microphone icon
        val iconPaint = Paint().apply {
            color = Color.WHITE
            strokeWidth = 12f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        
        // Microphone body
        val micRect = RectF(centerX - 40f, centerY - 60f, centerX + 40f, centerY + 20f)
        canvas.drawRoundRect(micRect, 40f, 40f, iconPaint)
        
        // Microphone stand
        canvas.drawLine(centerX - 60f, centerY + 20f, centerX + 60f, centerY + 20f, iconPaint)
        canvas.drawLine(centerX, centerY + 20f, centerX, centerY + 50f, iconPaint)
    }
    
    private fun drawCameraPreview(canvas: Canvas, y: Float, isDarkMode: Boolean) {
        val previewRect = RectF(100f, y, canvas.width - 100f, y + 400f)
        
        // Draw camera preview background
        val bgPaint = Paint().apply {
            color = Color.BLACK
        }
        canvas.drawRoundRect(previewRect, 20f, 20f, bgPaint)
        
        // Draw camera UI overlay
        val overlayPaint = Paint().apply {
            color = Color.WHITE
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        
        // Focus brackets
        val centerX = canvas.width / 2f
        val centerY = y + 200f
        val bracketSize = 100f
        
        // Top-left
        canvas.drawLine(centerX - bracketSize, centerY - bracketSize, 
                       centerX - bracketSize + 40f, centerY - bracketSize, overlayPaint)
        canvas.drawLine(centerX - bracketSize, centerY - bracketSize, 
                       centerX - bracketSize, centerY - bracketSize + 40f, overlayPaint)
        
        // Top-right
        canvas.drawLine(centerX + bracketSize - 40f, centerY - bracketSize, 
                       centerX + bracketSize, centerY - bracketSize, overlayPaint)
        canvas.drawLine(centerX + bracketSize, centerY - bracketSize, 
                       centerX + bracketSize, centerY - bracketSize + 40f, overlayPaint)
        
        // Bottom-left
        canvas.drawLine(centerX - bracketSize, centerY + bracketSize - 40f, 
                       centerX - bracketSize, centerY + bracketSize, overlayPaint)
        canvas.drawLine(centerX - bracketSize, centerY + bracketSize, 
                       centerX - bracketSize + 40f, centerY + bracketSize, overlayPaint)
        
        // Bottom-right
        canvas.drawLine(centerX + bracketSize, centerY + bracketSize - 40f, 
                       centerX + bracketSize, centerY + bracketSize, overlayPaint)
        canvas.drawLine(centerX + bracketSize - 40f, centerY + bracketSize, 
                       centerX + bracketSize, centerY + bracketSize, overlayPaint)
    }
    
    private fun drawFormFields(canvas: Canvas, y: Float, isDarkMode: Boolean) {
        val fieldBgPaint = Paint().apply {
            color = if (isDarkMode) Color.parseColor("#2A2A2A") else Color.WHITE
            style = Paint.Style.FILL
        }
        
        val fieldStrokePaint = Paint().apply {
            color = if (isDarkMode) Color.parseColor("#505050") else Color.parseColor("#E0E0E0")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        
        val labelPaint = Paint().apply {
            color = if (isDarkMode) Color.parseColor("#B0B0B0") else Color.parseColor("#666666")
            textSize = 36f
        }
        
        val valuePaint = Paint().apply {
            color = if (isDarkMode) darkTextColor else textColor
            textSize = 48f
        }
        
        var currentY = y
        val fields = listOf(
            Pair("Full Name", "John Smith"),
            Pair("Email", "john.smith@example.com"),
            Pair("Phone", "(555) 123-4567")
        )
        
        fields.forEach { (label, value) ->
            // Draw label
            canvas.drawText(label, 100f, currentY, labelPaint)
            currentY += 50f
            
            // Draw field background
            val fieldRect = RectF(100f, currentY - 40f, canvas.width - 100f, currentY + 40f)
            canvas.drawRoundRect(fieldRect, 15f, 15f, fieldBgPaint)
            canvas.drawRoundRect(fieldRect, 15f, 15f, fieldStrokePaint)
            
            // Draw value
            canvas.drawText(value, 130f, currentY + 10f, valuePaint)
            currentY += 100f
        }
    }
    
    private fun drawAccessibilityIcons(canvas: Canvas, y: Float, isDarkMode: Boolean) {
        val iconSize = 100f
        val spacing = 50f
        val totalWidth = iconSize * 4 + spacing * 3
        val startX = (canvas.width - totalWidth) / 2f
        var currentX = startX
        
        val iconPaint = Paint().apply {
            color = primaryColor
            textSize = 80f
            textAlign = Paint.Align.CENTER
        }
        
        val icons = listOf("♿", "👁", "👂", "⌨")
        
        icons.forEach { icon ->
            canvas.drawText(icon, currentX + iconSize / 2f, y + iconSize / 2f, iconPaint)
            currentX += iconSize + spacing
        }
    }
    
    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint): Float {
        val words = text.split(" ")
        var currentLine = ""
        var currentY = y
        val lineHeight = paint.textSize * 1.2f
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)
            
            if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, x, currentY, paint)
                currentLine = word
                currentY += lineHeight
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
        }
        
        return currentY + lineHeight
    }
    
    private fun saveScreenshot(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            println("Screenshot saved: ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}