package com.voicebridge.skills

import android.content.Context
import android.util.Log
import com.voicebridge.language.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.IOException

/**
 * Skill Template Manager for VoiceBridge
 * Manages loading, parsing, and execution of skill templates
 */
class SkillTemplateManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SkillTemplateManager"
        private const val SKILLS_DIR = "skills"
        
        @Volatile
        private var INSTANCE: SkillTemplateManager? = null
        
        fun getInstance(context: Context): SkillTemplateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SkillTemplateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    data class SkillTemplate(
        val id: String,
        val language: String,
        val name: String,
        val description: String,
        val version: String,
        val category: String = "general",
        val prompts: List<FieldPrompt>,
        val postprocess: List<PostprocessAction> = emptyList(),
        val accessibility: AccessibilityConfig = AccessibilityConfig(),
        val commands: List<VoiceCommand> = emptyList()
    )
    
    data class FieldPrompt(
        val field: String,
        val ask: String,
        val hint: String = "",
        val type: String,
        val required: Boolean = true,
        val validation: String? = null,
        val format: String? = null,
        val options: List<String> = emptyList(),
        val min: Int? = null,
        val max: Int? = null,
        val default: String? = null,
        val dependsOn: String? = null,
        val showWhen: String? = null
    )
    
    data class PostprocessAction(
        val action: String,
        val field: String,
        val format: String? = null,
        val pattern: String? = null
    )
    
    data class AccessibilityConfig(
        val targetApp: String? = null,
        val formSelectors: Map<String, String> = emptyMap(),
        val submitButton: String? = null
    )
    
    data class VoiceCommand(
        val trigger: String,
        val action: String,
        val skill: String? = null,
        val parameters: Map<String, Any> = emptyMap()
    )
    
    data class SkillCategory(
        val name: String,
        val displayName: String,
        val description: String,
        val skills: List<SkillTemplate>
    )
    
    private val languageManager = LanguageManager.getInstance(context)
    private val yaml = Yaml()
    private var loadedSkills = mutableMapOf<String, SkillTemplate>()
    
    init {
        loadSkillTemplates()
    }
    
    /**
     * Load all skill templates from assets
     */
    private fun loadSkillTemplates() {
        try {
            val skillsPath = "skills/forms"
            val assetManager = context.assets
            
            // List all YAML files in skills directory
            val skillFiles = assetManager.list(skillsPath) ?: return
            
            skillFiles.filter { it.endsWith(".yaml") || it.endsWith(".yml") }
                .forEach { filename ->
                    try {
                        val inputStream = assetManager.open("$skillsPath/$filename")
                        val skillData = yaml.load<Map<String, Any>>(inputStream)
                        val skill = parseSkillTemplate(skillData)
                        
                        loadedSkills[skill.id] = skill
                        Log.d(TAG, "Loaded skill: ${skill.id} (${skill.name})")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading skill file: $filename", e)
                    }
                }
            
            Log.i(TAG, "Loaded ${loadedSkills.size} skill templates")
            
        } catch (e: IOException) {
            Log.e(TAG, "Error accessing skills directory", e)
        }
    }
    
    /**
     * Parse YAML skill template data into SkillTemplate object
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseSkillTemplate(data: Map<String, Any>): SkillTemplate {
        val prompts = (data["prompts"] as? List<Map<String, Any>>)?.map { promptData ->
            FieldPrompt(
                field = promptData["field"] as String,
                ask = promptData["ask"] as String,
                hint = promptData["hint"] as? String ?: "",
                type = promptData["type"] as String,
                required = promptData["required"] as? Boolean ?: true,
                validation = promptData["validation"] as? String,
                format = promptData["format"] as? String,
                options = promptData["options"] as? List<String> ?: emptyList(),
                min = promptData["min"] as? Int,
                max = promptData["max"] as? Int,
                default = promptData["default"] as? String,
                dependsOn = promptData["depends_on"] as? String,
                showWhen = promptData["show_when"] as? String
            )
        } ?: emptyList()
        
        val postprocess = (data["postprocess"] as? List<Map<String, Any>>)?.map { actionData ->
            PostprocessAction(
                action = actionData["action"] as String,
                field = actionData["field"] as String,
                format = actionData["format"] as? String,
                pattern = actionData["pattern"] as? String
            )
        } ?: emptyList()
        
        val accessibilityData = data["accessibility"] as? Map<String, Any>
        val accessibility = AccessibilityConfig(
            targetApp = accessibilityData?.get("target_app") as? String,
            formSelectors = accessibilityData?.get("form_selectors") as? Map<String, String> ?: emptyMap(),
            submitButton = accessibilityData?.get("submit_button") as? String
        )
        
        val commands = (data["commands"] as? List<Map<String, Any>>)?.map { commandData ->
            VoiceCommand(
                trigger = commandData["trigger"] as String,
                action = commandData["action"] as String,
                skill = commandData["skill"] as? String,
                parameters = commandData["parameters"] as? Map<String, Any> ?: emptyMap()
            )
        } ?: emptyList()
        
        return SkillTemplate(
            id = data["id"] as String,
            language = data["language"] as String,
            name = data["name"] as String,
            description = data["description"] as String,
            version = data["version"] as String,
            category = data["category"] as? String ?: "general",
            prompts = prompts,
            postprocess = postprocess,
            accessibility = accessibility,
            commands = commands
        )
    }
    
    /**
     * Get all available skill templates
     */
    fun getAllSkills(): List<SkillTemplate> {
        return loadedSkills.values.toList()
    }
    
    /**
     * Get skills for current language
     */
    fun getSkillsForCurrentLanguage(): List<SkillTemplate> {
        val currentLang = languageManager.getCurrentLanguage()
        return loadedSkills.values.filter { 
            it.language == currentLang || it.language == "en" // Fallback to English
        }
    }
    
    /**
     * Get skill by ID
     */
    fun getSkill(skillId: String): SkillTemplate? {
        return loadedSkills[skillId]
    }
    
    /**
     * Get skills by category
     */
    fun getSkillsByCategory(): Map<String, List<SkillTemplate>> {
        return getSkillsForCurrentLanguage().groupBy { it.category }
    }
    
    /**
     * Get all available categories
     */
    fun getCategories(): List<SkillCategory> {
        val categoryMap = mapOf(
            "general" to SkillCategory("general", "General", "General purpose forms", emptyList()),
            "employment" to SkillCategory("employment", "Employment", "Job applications and employment forms", emptyList()),
            "healthcare" to SkillCategory("healthcare", "Healthcare", "Medical and healthcare forms", emptyList()),
            "financial" to SkillCategory("financial", "Financial", "Tax, banking, and financial forms", emptyList()),
            "government" to SkillCategory("government", "Government", "Government and benefit forms", emptyList()),
            "education" to SkillCategory("education", "Education", "School and education forms", emptyList())
        )
        
        val skillsByCategory = getSkillsByCategory()
        
        return categoryMap.map { (categoryId, category) ->
            category.copy(skills = skillsByCategory[categoryId] ?: emptyList())
        }.filter { it.skills.isNotEmpty() }
    }
    
    /**
     * Find skills by voice command trigger
     */
    fun findSkillByCommand(command: String): SkillTemplate? {
        val lowerCommand = command.lowercase()
        
        return getSkillsForCurrentLanguage().find { skill ->
            skill.commands.any { voiceCommand ->
                voiceCommand.trigger.lowercase() == lowerCommand ||
                lowerCommand.contains(voiceCommand.trigger.lowercase())
            }
        }
    }
    
    /**
     * Get all voice command triggers
     */
    fun getAllVoiceCommands(): List<String> {
        return getSkillsForCurrentLanguage().flatMap { skill ->
            skill.commands.map { it.trigger }
        }
    }
    
    /**
     * Validate form data against skill template
     */
    fun validateFormData(skillId: String, formData: Map<String, String>): ValidationResult {
        val skill = getSkill(skillId) ?: return ValidationResult(false, "Skill not found")
        
        val errors = mutableListOf<String>()
        
        skill.prompts.forEach { prompt ->
            val value = formData[prompt.field]
            
            // Check required fields
            if (prompt.required && (value.isNullOrBlank())) {
                errors.add("${prompt.field}: This field is required")
                return@forEach
            }
            
            // Skip validation if field is empty and not required
            if (value.isNullOrBlank()) return@forEach
            
            // Validate against regex pattern
            prompt.validation?.let { pattern ->
                if (!value.matches(Regex(pattern))) {
                    errors.add("${prompt.field}: Invalid format")
                }
            }
            
            // Validate number ranges
            if (prompt.type == "number") {
                try {
                    val number = value.toInt()
                    prompt.min?.let { min ->
                        if (number < min) errors.add("${prompt.field}: Must be at least $min")
                    }
                    prompt.max?.let { max ->
                        if (number > max) errors.add("${prompt.field}: Must be at most $max")
                    }
                } catch (e: NumberFormatException) {
                    errors.add("${prompt.field}: Must be a valid number")
                }
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors.joinToString("; "))
    }
    
    /**
     * Apply postprocessing to form data
     */
    suspend fun applyPostprocessing(skillId: String, formData: MutableMap<String, String>): Map<String, String> {
        return withContext(Dispatchers.IO) {
            val skill = getSkill(skillId) ?: return@withContext formData
            
            skill.postprocess.forEach { action ->
                val value = formData[action.field] ?: return@forEach
                
                when (action.action) {
                    "format_phone" -> {
                        formData[action.field] = formatPhoneNumber(value, action.format)
                    }
                    "format_ssn" -> {
                        formData[action.field] = formatSSN(value, action.format)
                    }
                    "capitalize_name" -> {
                        formData[action.field] = value.split(" ").joinToString(" ") { 
                            it.lowercase().replaceFirstChar { char -> char.uppercase() }
                        }
                    }
                    "uppercase_state" -> {
                        formData[action.field] = value.uppercase()
                    }
                    "format_currency" -> {
                        formData[action.field] = formatCurrency(value, action.format)
                    }
                    "validate_zip" -> {
                        action.pattern?.let { pattern ->
                            if (!value.matches(Regex(pattern))) {
                                Log.w(TAG, "ZIP code validation failed: $value")
                            }
                        }
                    }
                }
            }
            
            formData
        }
    }
    
    private fun formatPhoneNumber(phone: String, format: String?): String {
        val digits = phone.filter { it.isDigit() }
        return when (format) {
            "(XXX) XXX-XXXX" -> {
                if (digits.length == 10) {
                    "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
                } else phone
            }
            else -> phone
        }
    }
    
    private fun formatSSN(ssn: String, format: String?): String {
        val digits = ssn.filter { it.isDigit() }
        return when (format) {
            "XXX-XX-XXXX" -> {
                if (digits.length == 9) {
                    "${digits.substring(0, 3)}-${digits.substring(3, 5)}-${digits.substring(5)}"
                } else ssn
            }
            else -> ssn
        }
    }
    
    private fun formatCurrency(amount: String, format: String?): String {
        return try {
            val number = amount.replace(Regex("[^\\d.]"), "").toDouble()
            when (format) {
                "\$XXX,XXX.XX", "\$XX,XXX.XX", "\$X,XXX.XX" -> {
                    String.format("$%,.2f", number)
                }
                else -> amount
            }
        } catch (e: NumberFormatException) {
            amount
        }
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String
    )
}