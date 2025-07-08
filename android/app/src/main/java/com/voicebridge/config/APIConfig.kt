package com.voicebridge.config

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * API Configuration with Runtime Key Management
 * 
 * Users can enter API keys directly in the app - no code editing needed!
 * Keys are stored securely using Android's EncryptedSharedPreferences
 * 
 * Get your API keys from:
 * 1. Claude API: https://console.anthropic.com (~$5-15/month)
 * 2. OpenAI API: https://platform.openai.com ($0.006/minute for Whisper)
 * 3. Google Vision API: https://cloud.google.com (FREE 1000 requests/month)
 */
object APIConfig {
    
    private const val PREFS_NAME = "voicebridge_api_keys"
    private const val KEY_CLAUDE = "claude_api_key"
    private const val KEY_OPENAI = "openai_api_key"
    private const val KEY_GOOGLE_VISION = "google_vision_api_key"
    
    private var encryptedPrefs: SharedPreferences? = null
    
    /**
     * Initialize secure storage
     */
    fun initialize(context: Context) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            encryptedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            encryptedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Get Claude API key (entered by user in app)
     */
    fun getClaudeApiKey(): String {
        return encryptedPrefs?.getString(KEY_CLAUDE, "") ?: ""
    }
    
    /**
     * Get OpenAI API key (entered by user in app)
     */
    fun getOpenAiApiKey(): String {
        return encryptedPrefs?.getString(KEY_OPENAI, "") ?: ""
    }
    
    /**
     * Get Google Vision API key (entered by user in app)
     */
    fun getGoogleVisionApiKey(): String {
        return encryptedPrefs?.getString(KEY_GOOGLE_VISION, "") ?: ""
    }
    
    /**
     * Save Claude API key securely
     */
    fun setClaudeApiKey(key: String) {
        encryptedPrefs?.edit()?.putString(KEY_CLAUDE, key)?.apply()
    }
    
    /**
     * Save OpenAI API key securely
     */
    fun setOpenAiApiKey(key: String) {
        encryptedPrefs?.edit()?.putString(KEY_OPENAI, key)?.apply()
    }
    
    /**
     * Save Google Vision API key securely
     */
    fun setGoogleVisionApiKey(key: String) {
        encryptedPrefs?.edit()?.putString(KEY_GOOGLE_VISION, key)?.apply()
    }
    
    /**
     * Clear all API keys (for troubleshooting)
     */
    fun clearAllKeys() {
        encryptedPrefs?.edit()?.clear()?.apply()
    }
    
    /**
     * Check if Claude API is configured
     */
    fun isConfigured(): Boolean {
        val claudeKey = getClaudeApiKey()
        return claudeKey.isNotEmpty() && claudeKey.startsWith("sk-ant-")
    }
    
    /**
     * Check if speech recognition is available
     */
    fun hasSpeechAPI(): Boolean {
        val openaiKey = getOpenAiApiKey()
        return openaiKey.isNotEmpty() && openaiKey.startsWith("sk-")
    }
    
    /**
     * Check if OCR is available
     */
    fun hasVisionAPI(): Boolean {
        val visionKey = getGoogleVisionApiKey()
        return visionKey.isNotEmpty() && visionKey.length > 10
    }
    
    /**
     * Check if only demo mode is available
     */
    fun isDemoOnly(): Boolean {
        return !isConfigured()
    }
    
    /**
     * Get setup instructions
     */
    fun getSetupInstructions(): String {
        return buildString {
            appendLine("🚀 VoiceBridge API Setup")
            appendLine()
            appendLine("You need these API keys:")
            appendLine()
            appendLine("✅ Claude API (you have this)")
            appendLine("   Add to CLAUDE_API_KEY in APIConfig.kt")
            appendLine()
            appendLine("🎤 OpenAI API (for speech recognition)")
            appendLine("   1. Go to platform.openai.com")
            appendLine("   2. Create account and get API key")
            appendLine("   3. Add to OPENAI_API_KEY in APIConfig.kt")
            appendLine("   Cost: $0.006/minute (~$3-10/month)")
            appendLine()
            appendLine("📷 Google Vision API (for OCR)")
            appendLine("   1. Go to cloud.google.com/vision")
            appendLine("   2. Enable Vision API and get key")
            appendLine("   3. Add to GOOGLE_VISION_API_KEY")
            appendLine("   FREE: 1000 requests/month")
            appendLine()
            appendLine("💰 Total cost: ~$5-15/month for heavy use")
            appendLine("🆓 Free tier covers most personal use")
        }
    }
}