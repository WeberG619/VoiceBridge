package com.voicebridge.skills

import android.content.Context
import android.util.Log
import com.voicebridge.TextProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.InputStream

class SkillEngine(private val context: Context) {
    
    private val skills = mutableMapOf<String, Skill>()
    private val textProcessor = TextProcessor()
    private val yaml = Yaml()
    
    companion object {
        private const val TAG = "SkillEngine"
        private const val SKILLS_FOLDER = "skills"
    }
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            loadSkillsFromAssets()
            Log.i(TAG, "Skill engine initialized with ${skills.size} skills")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing skill engine", e)
            false
        }
    }
    
    private suspend fun loadSkillsFromAssets() {
        val assetManager = context.assets
        
        try {
            // Load skills from forms folder
            val formsFolder = "$SKILLS_FOLDER/forms"
            val formFiles = assetManager.list(formsFolder) ?: emptyArray()
            
            for (fileName in formFiles) {
                if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                    val inputStream = assetManager.open("$formsFolder/$fileName")
                    loadSkillFromStream(inputStream, fileName)
                }
            }
            
            // Load skills from documents folder
            val documentsFolder = "$SKILLS_FOLDER/documents"
            val documentFiles = assetManager.list(documentsFolder) ?: emptyArray()
            
            for (fileName in documentFiles) {
                if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                    val inputStream = assetManager.open("$documentsFolder/$fileName")
                    loadSkillFromStream(inputStream, fileName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading skills from assets", e)
        }
    }
    
    private fun loadSkillFromStream(inputStream: InputStream, fileName: String) {
        try {
            val skillData = yaml.load<Map<String, Any>>(inputStream)
            val skill = parseSkillData(skillData)
            skills[skill.id] = skill
            Log.d(TAG, "Loaded skill: ${skill.name} (${skill.id})")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading skill from $fileName", e)
        }
    }
    
    private fun parseSkillData(data: Map<String, Any>): Skill {
        val id = data["id"] as String
        val name = data["name"] as String
        val description = data["description"] as? String ?: ""
        val language = data["language"] as? String ?: "en"
        val version = data["version"] as? String ?: "1.0"
        
        // Parse prompts
        val promptsData = data["prompts"] as? List<Map<String, Any>> ?: emptyList()
        val prompts = promptsData.map { parsePrompt(it) }
        
        // Parse postprocessing
        val postprocessData = data["postprocess"] as? List<Map<String, Any>> ?: emptyList()
        val postprocess = postprocessData.map { parsePostprocessAction(it) }
        
        // Parse accessibility configuration
        val accessibilityData = data["accessibility"] as? Map<String, Any>
        val accessibility = accessibilityData?.let { parseAccessibilityConfig(it) }
        
        // Parse commands
        val commandsData = data["commands"] as? List<Map<String, Any>> ?: emptyList()
        val commands = commandsData.map { parseCommand(it) }
        
        return Skill(
            id = id,
            name = name,
            description = description,
            language = language,
            version = version,
            prompts = prompts,
            postprocess = postprocess,
            accessibility = accessibility,
            commands = commands
        )
    }
    
    private fun parsePrompt(data: Map<String, Any>): SkillPrompt {
        return SkillPrompt(
            field = data["field"] as String,
            ask = data["ask"] as String,
            hint = data["hint"] as? String ?: "",
            type = data["type"] as? String ?: "text",
            required = data["required"] as? Boolean ?: false,
            validation = data["validation"] as? String,
            format = data["format"] as? String,
            options = data["options"] as? List<String>,
            defaultValue = data["default"] as? String,
            min = data["min"] as? Int,
            max = data["max"] as? Int
        )
    }
    
    private fun parsePostprocessAction(data: Map<String, Any>): PostprocessAction {
        return PostprocessAction(
            action = data["action"] as String,
            field = data["field"] as String,
            format = data["format"] as? String,
            pattern = data["pattern"] as? String,
            options = data["options"] as? Map<String, String>
        )
    }
    
    private fun parseAccessibilityConfig(data: Map<String, Any>): AccessibilityConfig {
        val formSelectors = data["form_selectors"] as? Map<String, String> ?: emptyMap()
        return AccessibilityConfig(
            targetApp = data["target_app"] as? String,
            formSelectors = formSelectors,
            submitButton = data["submit_button"] as? String
        )
    }
    
    private fun parseCommand(data: Map<String, Any>): SkillCommand {
        return SkillCommand(
            trigger = data["trigger"] as String,
            action = data["action"] as String,
            skill = data["skill"] as? String,
            parameters = data["parameters"] as? Map<String, Any> ?: emptyMap()
        )
    }
    
    fun findSkillByCommand(command: String): Skill? {
        val cleanCommand = textProcessor.cleanText(command).lowercase()
        
        return skills.values.find { skill ->
            skill.commands.any { cmd ->
                cleanCommand.contains(cmd.trigger.lowercase()) ||
                cmd.trigger.lowercase().contains(cleanCommand)
            }
        }
    }
    
    fun getSkillById(id: String): Skill? {
        return skills[id]
    }
    
    fun getAllSkills(): List<Skill> {
        return skills.values.toList()
    }
    
    suspend fun executeSkill(skill: Skill, userInputs: Map<String, String>): SkillExecutionResult = withContext(Dispatchers.IO) {
        try {
            val processedData = mutableMapOf<String, String>()
            val errors = mutableListOf<String>()
            
            // Process each prompt
            for (prompt in skill.prompts) {
                val userInput = userInputs[prompt.field]
                
                if (prompt.required && userInput.isNullOrBlank()) {
                    errors.add("Required field '${prompt.field}' is missing")
                    continue
                }
                
                if (userInput != null) {
                    // Validate input
                    if (prompt.validation != null && !userInput.matches(Regex(prompt.validation))) {
                        errors.add("Field '${prompt.field}' does not match required format")
                        continue
                    }
                    
                    // Format input
                    val formattedInput = textProcessor.formatForForm(userInput, prompt.type)
                    processedData[prompt.field] = formattedInput
                }
            }
            
            // Apply postprocessing
            for (action in skill.postprocess) {
                val fieldValue = processedData[action.field]
                if (fieldValue != null) {
                    val processedValue = applyPostprocessAction(action, fieldValue)
                    processedData[action.field] = processedValue
                }
            }
            
            SkillExecutionResult(
                success = errors.isEmpty(),
                processedData = processedData,
                errors = errors,
                skill = skill
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing skill ${skill.id}", e)
            SkillExecutionResult(
                success = false,
                processedData = emptyMap(),
                errors = listOf("Execution error: ${e.message}"),
                skill = skill
            )
        }
    }
    
    private fun applyPostprocessAction(action: PostprocessAction, value: String): String {
        return when (action.action) {
            "format_phone" -> {
                val digits = value.replace(Regex("[^0-9]"), "")
                if (digits.length == 10) {
                    "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
                } else value
            }
            "format_ssn" -> {
                val digits = value.replace(Regex("[^0-9]"), "")
                if (digits.length == 9) {
                    "${digits.substring(0, 3)}-${digits.substring(3, 5)}-${digits.substring(5)}"
                } else value
            }
            "capitalize_name" -> {
                value.split(" ").joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
            }
            "uppercase_state" -> value.uppercase()
            "format_currency" -> {
                val amount = value.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                if (amount != null) {
                    "$%.2f".format(amount)
                } else value
            }
            else -> value
        }
    }
    
    fun getSkillPrompts(skillId: String): List<SkillPrompt> {
        return skills[skillId]?.prompts ?: emptyList()
    }
    
    fun validateSkillInput(skillId: String, field: String, value: String): ValidationResult {
        val skill = skills[skillId] ?: return ValidationResult(false, "Skill not found")
        val prompt = skill.prompts.find { it.field == field } ?: return ValidationResult(false, "Field not found")
        
        if (prompt.required && value.isBlank()) {
            return ValidationResult(false, "This field is required")
        }
        
        if (prompt.validation != null && !value.matches(Regex(prompt.validation))) {
            return ValidationResult(false, "Input does not match required format")
        }
        
        return ValidationResult(true, "Valid")
    }
}

data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val language: String,
    val version: String,
    val prompts: List<SkillPrompt>,
    val postprocess: List<PostprocessAction>,
    val accessibility: AccessibilityConfig?,
    val commands: List<SkillCommand>
)

data class SkillPrompt(
    val field: String,
    val ask: String,
    val hint: String,
    val type: String,
    val required: Boolean,
    val validation: String?,
    val format: String?,
    val options: List<String>?,
    val defaultValue: String?,
    val min: Int?,
    val max: Int?
)

data class PostprocessAction(
    val action: String,
    val field: String,
    val format: String?,
    val pattern: String?,
    val options: Map<String, String>?
)

data class AccessibilityConfig(
    val targetApp: String?,
    val formSelectors: Map<String, String>,
    val submitButton: String?
)

data class SkillCommand(
    val trigger: String,
    val action: String,
    val skill: String?,
    val parameters: Map<String, Any>
)

data class SkillExecutionResult(
    val success: Boolean,
    val processedData: Map<String, String>,
    val errors: List<String>,
    val skill: Skill
)

data class ValidationResult(
    val isValid: Boolean,
    val message: String
)