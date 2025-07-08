package com.voicebridge.skills

import android.content.Context
import android.util.Log
import com.voicebridge.TextProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.InputStream

/**
 * Result of voice processing operations
 */
data class VoiceProcessingResult(
    val isSuccess: Boolean,
    val action: String,
    val message: String,
    val originalText: String,
    val processedText: String? = null,
    val skillName: String? = null,
    val skillId: String? = null,
    val formType: String? = null,
    val command: String? = null,
    val target: String? = null,
    val text: String? = null,
    val formData: Map<String, String>? = null
)

class SkillEngine private constructor(private val context: Context) {
    
    private val skills = mutableMapOf<String, Skill>()
    private val textProcessor = TextProcessor()
    private val yaml = Yaml()
    
    companion object {
        private const val TAG = "SkillEngine"
        private const val SKILLS_FOLDER = "skills"
        
        @Volatile
        private var INSTANCE: SkillEngine? = null
        
        fun getInstance(context: Context): SkillEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SkillEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
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
    
    /**
     * Process voice input and return a result for the UI
     */
    suspend fun processVoiceInput(voiceText: String): VoiceProcessingResult = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Processing voice input: $voiceText")
            
            // Clean and process the text
            val cleanText = textProcessor.cleanText(voiceText)
            
            // Find matching skill
            val matchedSkill = findSkillByCommand(cleanText)
            
            if (matchedSkill != null) {
                Log.i(TAG, "Found matching skill: ${matchedSkill.name}")
                
                return@withContext VoiceProcessingResult(
                    isSuccess = true,
                    action = "skill_found",
                    skillName = matchedSkill.name,
                    skillId = matchedSkill.id,
                    message = "Found skill: ${matchedSkill.name}",
                    originalText = voiceText,
                    processedText = cleanText
                )
            } else {
                // Try to extract general commands with fallback
                val extractedCommands = try {
                    textProcessor.extractCommands(cleanText)
                } catch (e: Exception) {
                    Log.w(TAG, "Native command extraction failed, using fallback", e)
                    extractCommandsFallback(cleanText)
                }
                
                if (extractedCommands.isNotEmpty()) {
                    val command = extractedCommands[0]
                    
                    return@withContext VoiceProcessingResult(
                        isSuccess = true,
                        action = "general_command",
                        command = command,
                        message = "Understood: $command",
                        originalText = voiceText,
                        processedText = cleanText
                    )
                } else {
                    // Be more lenient - accept any reasonable input
                    if (cleanText.trim().length >= 2) {
                        return@withContext VoiceProcessingResult(
                            isSuccess = true,
                            action = "general_command",
                            command = "general inquiry",
                            message = "I heard: $cleanText",
                            originalText = voiceText,
                            processedText = cleanText
                        )
                    } else {
                        return@withContext VoiceProcessingResult(
                            isSuccess = false,
                            action = "unclear",
                            message = "Could you please speak more clearly?",
                            originalText = voiceText,
                            processedText = cleanText
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing voice input", e)
            return@withContext VoiceProcessingResult(
                isSuccess = false,
                action = "error",
                message = "Error processing voice: ${e.message}",
                originalText = voiceText
            )
        }
    }
    
    /**
     * Process OCR text for form detection
     */
    suspend fun processOCRText(ocrText: String): VoiceProcessingResult = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Processing OCR text: $ocrText")
            
            // Clean the OCR text
            val cleanText = textProcessor.cleanText(ocrText)
            
            // Look for form-related skills
            val formSkills = skills.values.filter { it.description.contains("form", ignoreCase = true) }
            
            for (skill in formSkills) {
                // Check if OCR text matches form patterns
                for (prompt in skill.prompts) {
                    if (cleanText.contains(prompt.ask, ignoreCase = true) ||
                        cleanText.contains(prompt.field, ignoreCase = true)) {
                        
                        return@withContext VoiceProcessingResult(
                            isSuccess = true,
                            action = "form_detected",
                            skillName = skill.name,
                            skillId = skill.id,
                            formType = "form",
                            message = "Detected form: ${skill.name}",
                            originalText = ocrText,
                            processedText = cleanText
                        )
                    }
                }
            }
            
            return@withContext VoiceProcessingResult(
                isSuccess = false,
                action = "no_form_detected",
                message = "No matching form found",
                originalText = ocrText,
                processedText = cleanText
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing OCR text", e)
            return@withContext VoiceProcessingResult(
                isSuccess = false,
                action = "error",
                message = "Error processing OCR: ${e.message}",
                originalText = ocrText
            )
        }
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
    
    /**
     * Fallback command extraction when native method fails
     */
    private fun extractCommandsFallback(text: String): Array<String> {
        val commands = mutableListOf<String>()
        val lowerText = text.lowercase().trim()
        
        // Define flexible command patterns
        val commandPatterns = mapOf(
            "hello|hi|hey|greetings|good morning|good afternoon" to "greeting",
            "how.*are.*you|what.*up|how.*going" to "greeting",
            "fill.*form|complete.*form|finish.*form|form.*fill" to "fill form",
            "start.*application|new.*application|apply.*for" to "start application",
            "help.*me|help.*with|assist.*me|what.*can.*you.*do" to "help",
            "start.*camera|open.*camera|camera.*on" to "start camera",
            "capture.*image|take.*photo|take.*picture|snap.*photo" to "capture image",
            "yes|yeah|yep|okay|ok|sure|alright" to "confirm",
            "no|nope|cancel|stop|quit" to "cancel",
            "test.*recognition|test.*speech|testing" to "test"
        )
        
        // Check each pattern
        for ((pattern, command) in commandPatterns) {
            if (lowerText.contains(Regex(pattern))) {
                commands.add(command)
                Log.d(TAG, "Fallback matched: '$pattern' -> '$command'")
                break // Take first match
            }
        }
        
        // If no patterns match, try simple word matching
        if (commands.isEmpty()) {
            when {
                lowerText.contains("hello") || lowerText.contains("hi") -> commands.add("greeting")
                lowerText.contains("camera") -> commands.add("start camera")
                lowerText.contains("capture") || lowerText.contains("photo") -> commands.add("capture image")
                lowerText.contains("form") -> commands.add("fill form")
                lowerText.contains("help") -> commands.add("help")
                lowerText.contains("test") -> commands.add("test")
                lowerText.contains("yes") || lowerText.contains("ok") -> commands.add("confirm")
                lowerText.contains("no") || lowerText.contains("stop") -> commands.add("cancel")
                lowerText.length > 2 -> commands.add("general command")
            }
        }
        
        return commands.toTypedArray()
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