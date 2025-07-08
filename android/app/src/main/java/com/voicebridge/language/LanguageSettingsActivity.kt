package com.voicebridge.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.voicebridge.R
import com.voicebridge.telemetry.PrivacyAnalytics
import kotlinx.coroutines.launch

/**
 * Language Settings Activity for VoiceBridge
 * Allows users to configure language preferences
 */
class LanguageSettingsActivity : AppCompatActivity() {
    
    private lateinit var languageManager: LanguageManager
    private lateinit var analytics: PrivacyAnalytics
    
    private lateinit var autoDetectSwitch: Switch
    private lateinit var languageSpinner: Spinner
    private lateinit var currentLanguageText: TextView
    private lateinit var voiceSupportText: TextView
    private lateinit var ocrSupportText: TextView
    private lateinit var commandsContainer: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        languageManager = LanguageManager.getInstance(this)
        analytics = PrivacyAnalytics.getInstance(this)
        
        setupUI()
        loadCurrentSettings()
        
        analytics.trackFeatureUsage("language", "settings_opened")
    }
    
    private fun setupUI() {
        // Create layout programmatically for simplicity
        val scrollView = ScrollView(this)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Title
        val titleText = TextView(this).apply {
            text = getString(R.string.app_name) + " - Language Settings"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 32)
        }
        mainLayout.addView(titleText)
        
        // Auto-detect section
        val autoDetectContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
        }
        
        val autoDetectLabel = TextView(this).apply {
            text = "Auto-detect language"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        autoDetectSwitch = Switch(this).apply {
            setOnCheckedChangeListener { _, isChecked ->
                onAutoDetectChanged(isChecked)
            }
        }
        
        autoDetectContainer.addView(autoDetectLabel)
        autoDetectContainer.addView(autoDetectSwitch)
        mainLayout.addView(autoDetectContainer)
        
        // Language selection
        val languageLabel = TextView(this).apply {
            text = "Select Language"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 24, 0, 8)
        }
        mainLayout.addView(languageLabel)
        
        languageSpinner = Spinner(this).apply {
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    onLanguageSelected(position)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        mainLayout.addView(languageSpinner)
        
        // Current language info
        currentLanguageText = TextView(this).apply {
            textSize = 14f
            setPadding(0, 16, 0, 8)
        }
        mainLayout.addView(currentLanguageText)
        
        voiceSupportText = TextView(this).apply {
            textSize = 14f
            setPadding(0, 4, 0, 4)
        }
        mainLayout.addView(voiceSupportText)
        
        ocrSupportText = TextView(this).apply {
            textSize = 14f
            setPadding(0, 4, 0, 16)
        }
        mainLayout.addView(ocrSupportText)
        
        // Voice commands section
        val commandsLabel = TextView(this).apply {
            text = "Voice Commands for Current Language"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 24, 0, 8)
        }
        mainLayout.addView(commandsLabel)
        
        commandsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        mainLayout.addView(commandsContainer)
        
        // Test section
        val testLabel = TextView(this).apply {
            text = "Test Voice Recognition"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 24, 0, 8)
        }
        mainLayout.addView(testLabel)
        
        val testButton = Button(this).apply {
            text = "Test Voice in Current Language"
            setOnClickListener { testVoiceRecognition() }
        }
        mainLayout.addView(testButton)
        
        scrollView.addView(mainLayout)
        setContentView(scrollView)
        
        supportActionBar?.apply {
            title = "Language Settings"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun loadCurrentSettings() {
        // Setup language spinner
        val languageNames = languageManager.supportedLanguages.map { "${it.nativeName} (${it.name})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter
        
        // Set current selections
        autoDetectSwitch.isChecked = languageManager.isAutoDetectEnabled()
        
        val currentLang = languageManager.getCurrentLanguage()
        val langIndex = languageManager.supportedLanguages.indexOfFirst { it.code == currentLang }
        if (langIndex >= 0) {
            languageSpinner.setSelection(langIndex)
        }
        
        updateLanguageInfo()
        updateVoiceCommands()
    }
    
    private fun onAutoDetectChanged(enabled: Boolean) {
        languageManager.setAutoDetectEnabled(enabled)
        languageSpinner.isEnabled = !enabled
        
        if (enabled) {
            // Update to system language
            updateLanguageInfo()
            updateVoiceCommands()
        }
        
        analytics.trackFeatureUsage("language", "auto_detect_toggled", mapOf("enabled" to enabled))
    }
    
    private fun onLanguageSelected(position: Int) {
        if (!languageManager.isAutoDetectEnabled() && position < languageManager.supportedLanguages.size) {
            val selectedLang = languageManager.supportedLanguages[position]
            languageManager.setLanguage(selectedLang.code)
            
            updateLanguageInfo()
            updateVoiceCommands()
            
            analytics.trackFeatureUsage("language", "language_changed", mapOf("language" to selectedLang.code))
        }
    }
    
    private fun updateLanguageInfo() {
        val currentLang = languageManager.getCurrentLanguageInfo()
        
        currentLanguageText.text = "Current: ${currentLang.nativeName} (${currentLang.name})"
        
        voiceSupportText.text = "Voice Recognition: ${if (currentLang.voiceSupported) "✓ Supported" else "✗ Not supported"}"
        voiceSupportText.setTextColor(if (currentLang.voiceSupported) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
        
        ocrSupportText.text = "OCR/Text Scanning: ${if (currentLang.ocrSupported) "✓ Supported" else "✗ Not supported"}"
        ocrSupportText.setTextColor(if (currentLang.ocrSupported) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
    }
    
    private fun updateVoiceCommands() {
        commandsContainer.removeAllViews()
        
        val commands = languageManager.getVoiceCommandTriggers()
        
        commands.forEach { (action, triggers) ->
            val commandContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 8, 16, 8)
            }
            
            val actionText = TextView(this).apply {
                text = action.replace("_", " ").replaceFirstChar { it.uppercase() }
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            commandContainer.addView(actionText)
            
            val triggersText = TextView(this).apply {
                text = "• ${triggers.joinToString("  • ")}"
                textSize = 12f
                setTextColor(0xFF666666.toInt())
                setPadding(16, 4, 0, 0)
            }
            commandContainer.addView(triggersText)
            
            commandsContainer.addView(commandContainer)
        }
    }
    
    private fun testVoiceRecognition() {
        if (!languageManager.isVoiceSupported()) {
            Toast.makeText(this, "Voice recognition not supported for current language", Toast.LENGTH_SHORT).show()
            return
        }
        
        analytics.trackFeatureUsage("language", "voice_test_started")
        
        // TODO: Integrate with actual voice recognition system
        Toast.makeText(this, "Voice test feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        analytics.trackFeatureUsage("language", "settings_closed")
    }
}