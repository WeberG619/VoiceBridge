package com.voicebridge.config

/**
 * API Configuration
 * 
 * Add your API keys here:
 * 
 * 1. CLAUDE_API_KEY: You already have this from Anthropic
 * 2. OPENAI_API_KEY: Get from platform.openai.com (for Whisper speech recognition)
 * 3. GOOGLE_VISION_API_KEY: Get from cloud.google.com (free tier: 1000 requests/month)
 * 
 * Monthly costs for moderate use:
 * - Claude: $5-15 (you already have this)
 * - OpenAI Whisper: $3-10 ($0.006/minute)
 * - Google Vision: FREE up to 1000 forms/month
 * Total: ~$8-25/month for heavy business use
 */
object APIConfig {
    
    // You already have this API key
    const val CLAUDE_API_KEY = "sk-ant-api03-YOUR_CLAUDE_API_KEY_HERE"
    
    // Get from https://platform.openai.com/api-keys
    // Cost: $0.006 per minute of audio (very affordable)
    const val OPENAI_API_KEY = "sk-YOUR_OPENAI_API_KEY_HERE"
    
    // Get from https://cloud.google.com/vision/docs/setup
    // FREE TIER: 1000 requests per month
    const val GOOGLE_VISION_API_KEY = "YOUR_GOOGLE_VISION_API_KEY_HERE"
    
    /**
     * Check if APIs are configured
     */
    fun isConfigured(): Boolean {
        return CLAUDE_API_KEY.startsWith("sk-ant-") && CLAUDE_API_KEY != "sk-ant-api03-YOUR_CLAUDE_API_KEY_HERE" &&
               OPENAI_API_KEY.startsWith("sk-") && OPENAI_API_KEY != "sk-YOUR_OPENAI_API_KEY_HERE"
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