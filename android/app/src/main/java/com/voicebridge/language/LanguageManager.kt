package com.voicebridge.language

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import java.util.*

/**
 * Language Manager for VoiceBridge
 * Handles language detection, switching, and localization
 */
class LanguageManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "LanguageManager"
        private const val PREFS_NAME = "language_preferences"
        private const val PREF_SELECTED_LANGUAGE = "selected_language"
        private const val PREF_AUTO_DETECT = "auto_detect_language"
        
        @Volatile
        private var INSTANCE: LanguageManager? = null
        
        fun getInstance(context: Context): LanguageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LanguageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    data class SupportedLanguage(
        val code: String,
        val name: String,
        val nativeName: String,
        val voiceSupported: Boolean = true,
        val ocrSupported: Boolean = true
    )
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Supported languages with comprehensive voice and OCR support
    val supportedLanguages = listOf(
        SupportedLanguage("en", "English", "English", true, true),
        SupportedLanguage("es", "Spanish", "Español", true, true),
        SupportedLanguage("pt", "Portuguese", "Português", true, true),
        SupportedLanguage("fr", "French", "Français", true, true),
        SupportedLanguage("de", "German", "Deutsch", true, true),
        SupportedLanguage("it", "Italian", "Italiano", true, true),
        SupportedLanguage("zh", "Chinese", "中文", true, true),
        SupportedLanguage("ja", "Japanese", "日本語", true, true),
        SupportedLanguage("ko", "Korean", "한국어", true, true),
        SupportedLanguage("ru", "Russian", "Русский", true, true),
        SupportedLanguage("ar", "Arabic", "العربية", true, true),
        SupportedLanguage("hi", "Hindi", "हिन्दी", true, true)
    )
    
    /**
     * Get the current language code
     */
    fun getCurrentLanguage(): String {
        return if (isAutoDetectEnabled()) {
            detectSystemLanguage()
        } else {
            prefs.getString(PREF_SELECTED_LANGUAGE, "en") ?: "en"
        }
    }
    
    /**
     * Set the application language
     */
    fun setLanguage(languageCode: String) {
        if (supportedLanguages.any { it.code == languageCode }) {
            prefs.edit()
                .putString(PREF_SELECTED_LANGUAGE, languageCode)
                .putBoolean(PREF_AUTO_DETECT, false)
                .apply()
            
            Log.i(TAG, "Language set to: $languageCode")
        } else {
            Log.w(TAG, "Unsupported language code: $languageCode")
        }
    }
    
    /**
     * Enable or disable automatic language detection
     */
    fun setAutoDetectEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(PREF_AUTO_DETECT, enabled)
            .apply()
        
        Log.i(TAG, "Auto-detect language: $enabled")
    }
    
    /**
     * Check if auto-detect is enabled
     */
    fun isAutoDetectEnabled(): Boolean {
        return prefs.getBoolean(PREF_AUTO_DETECT, true)
    }
    
    /**
     * Detect system language
     */
    fun detectSystemLanguage(): String {
        val systemLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        val languageCode = systemLocale.language
        
        // Check if we support this language
        return if (supportedLanguages.any { it.code == languageCode }) {
            languageCode
        } else {
            "en" // Default to English
        }
    }
    
    /**
     * Get language object by code
     */
    fun getLanguage(code: String): SupportedLanguage? {
        return supportedLanguages.find { it.code == code }
    }
    
    /**
     * Get current language object
     */
    fun getCurrentLanguageInfo(): SupportedLanguage {
        val code = getCurrentLanguage()
        return getLanguage(code) ?: supportedLanguages.first { it.code == "en" }
    }
    
    /**
     * Check if voice recognition is supported for current language
     */
    fun isVoiceSupported(): Boolean {
        return getCurrentLanguageInfo().voiceSupported
    }
    
    /**
     * Check if OCR is supported for current language
     */
    fun isOCRSupported(): Boolean {
        return getCurrentLanguageInfo().ocrSupported
    }
    
    /**
     * Apply language configuration to context
     */
    fun applyLanguageConfiguration(context: Context): Context {
        val language = getCurrentLanguage()
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
    
    /**
     * Get available voice recognition languages for Whisper
     */
    fun getWhisperLanguageCodes(): List<String> {
        // Whisper model language codes
        return mapOf(
            "en" to "en",
            "es" to "es",
            "pt" to "pt",
            "fr" to "fr",
            "de" to "de",
            "it" to "it",
            "zh" to "zh",
            "ja" to "ja",
            "ko" to "ko",
            "ru" to "ru",
            "ar" to "ar",
            "hi" to "hi"
        ).let { mapping ->
            supportedLanguages.filter { it.voiceSupported }
                .mapNotNull { mapping[it.code] }
        }
    }
    
    /**
     * Get Tesseract language codes for OCR
     */
    fun getTesseractLanguageCodes(): List<String> {
        // Tesseract language codes
        return mapOf(
            "en" to "eng",
            "es" to "spa",
            "pt" to "por",
            "fr" to "fra", 
            "de" to "deu",
            "it" to "ita",
            "zh" to "chi_sim",
            "ja" to "jpn",
            "ko" to "kor",
            "ru" to "rus",
            "ar" to "ara",
            "hi" to "hin"
        ).let { mapping ->
            supportedLanguages.filter { it.ocrSupported }
                .mapNotNull { mapping[it.code] }
        }
    }
    
    /**
     * Get voice command triggers for current language
     */
    fun getVoiceCommandTriggers(): Map<String, List<String>> {
        return when (getCurrentLanguage()) {
            "es" -> mapOf(
                "start_listening" to listOf("empezar", "comenzar", "escuchar", "iniciar"),
                "stop_listening" to listOf("parar", "detener", "finalizar", "terminar"),
                "capture_image" to listOf("capturar", "fotografiar", "escanear", "tomar foto"),
                "fill_form" to listOf("llenar formulario", "completar", "rellenar", "llenar"),
                "submit_form" to listOf("enviar", "confirmar", "mandar", "completar")
            )
            "pt" -> mapOf(
                "start_listening" to listOf("começar", "iniciar", "escutar", "ouvir"),
                "stop_listening" to listOf("parar", "finalizar", "terminar", "encerrar"),
                "capture_image" to listOf("capturar", "fotografar", "escanear", "tirar foto"),
                "fill_form" to listOf("preencher formulário", "completar", "preencher"),
                "submit_form" to listOf("enviar", "confirmar", "mandar", "submeter")
            )
            "fr" -> mapOf(
                "start_listening" to listOf("commencer", "démarrer", "écouter", "initier"),
                "stop_listening" to listOf("arrêter", "stopper", "terminer", "finir"),
                "capture_image" to listOf("capturer", "photographier", "scanner", "prendre photo"),
                "fill_form" to listOf("remplir formulaire", "compléter", "remplir"),
                "submit_form" to listOf("envoyer", "confirmer", "soumettre", "valider")
            )
            "de" -> mapOf(
                "start_listening" to listOf("anfangen", "starten", "hören", "beginnen"),
                "stop_listening" to listOf("stoppen", "beenden", "aufhören", "anhalten"),
                "capture_image" to listOf("erfassen", "fotografieren", "scannen", "foto machen"),
                "fill_form" to listOf("formular ausfüllen", "vervollständigen", "ausfüllen"),
                "submit_form" to listOf("senden", "bestätigen", "abschicken", "übermitteln")
            )
            "it" -> mapOf(
                "start_listening" to listOf("iniziare", "cominciare", "ascoltare", "avviare"),
                "stop_listening" to listOf("fermare", "smettere", "terminare", "finire"),
                "capture_image" to listOf("catturare", "fotografare", "scansionare", "fare foto"),
                "fill_form" to listOf("compilare modulo", "completare", "riempire"),
                "submit_form" to listOf("inviare", "confermare", "sottomettere", "mandare")
            )
            else -> mapOf( // Default English
                "start_listening" to listOf("start listening", "begin", "listen", "start"),
                "stop_listening" to listOf("stop listening", "stop", "end", "finish"),
                "capture_image" to listOf("capture", "take photo", "scan", "photograph"),
                "fill_form" to listOf("fill form", "complete form", "fill out", "complete"),
                "submit_form" to listOf("submit", "send", "confirm", "complete")
            )
        }
    }
    
    /**
     * Get language statistics for analytics
     */
    fun getLanguageStats(): Map<String, Any> {
        return mapOf(
            "current_language" to getCurrentLanguage(),
            "auto_detect_enabled" to isAutoDetectEnabled(),
            "supported_languages_count" to supportedLanguages.size,
            "voice_supported" to isVoiceSupported(),
            "ocr_supported" to isOCRSupported(),
            "system_language" to detectSystemLanguage()
        )
    }
}