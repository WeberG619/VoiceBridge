package com.voicebridge

import com.voicebridge.skills.SkillEngine
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.io.File
import java.io.FileWriter

class SkillEngineTest {
    
    private lateinit var skillEngine: SkillEngine
    private lateinit var testSkillFile: File
    
    @Before
    fun setUp() {
        skillEngine = SkillEngine()
        
        // Create a temporary test skill file
        testSkillFile = File.createTempFile("test_skill", ".yaml")
        val skillContent = """
            id: test_form
            name: Test Form
            prompts:
              - field: full_name
                ask: "What is your full name?"
                type: name
                required: true
              - field: email
                ask: "What is your email address?"
                type: email
                format: "email"
                required: true
              - field: phone
                ask: "What is your phone number?"
                type: phone
                format: "(XXX) XXX-XXXX"
                required: false
            selectors:
              full_name: 
                - "input[name='fullName']"
                - "input[id='full-name']"
              email:
                - "input[type='email']"
                - "input[name='email']"
              phone:
                - "input[type='tel']"
                - "input[name='phone']"
            postprocess:
              full_name: "title_case"
              phone: "format_phone"
            commands:
              - trigger: "fill test form"
                action: "fill_form"
                skill: "test_form"
        """.trimIndent()
        
        FileWriter(testSkillFile).use { it.write(skillContent) }
    }
    
    @Test
    fun testLoadSkill() = runBlocking {
        val skill = skillEngine.loadSkill(testSkillFile.absolutePath)
        
        assertNotNull("Skill should be loaded", skill)
        assertEquals("test_form", skill?.id)
        assertEquals("Test Form", skill?.name)
        assertEquals(3, skill?.prompts?.size)
    }
    
    @Test
    fun testValidateFieldRequired() = runBlocking {
        val skill = skillEngine.loadSkill(testSkillFile.absolutePath)
        assertNotNull(skill)
        
        val requiredField = skill!!.prompts.find { it.field == "full_name" }
        assertNotNull(requiredField)
        assertTrue("full_name should be required", requiredField!!.required)
        
        val optionalField = skill.prompts.find { it.field == "phone" }
        assertNotNull(optionalField)
        assertFalse("phone should be optional", optionalField!!.required)
    }
    
    @Test
    fun testValidateFieldTypes() = runBlocking {
        val skill = skillEngine.loadSkill(testSkillFile.absolutePath)
        assertNotNull(skill)
        
        val nameField = skill!!.prompts.find { it.field == "full_name" }
        assertEquals("name", nameField?.type)
        
        val emailField = skill.prompts.find { it.field == "email" }
        assertEquals("email", emailField?.type)
        
        val phoneField = skill.prompts.find { it.field == "phone" }
        assertEquals("phone", phoneField?.type)
    }
    
    @Test
    fun testFormatValidation() = runBlocking {
        val skill = skillEngine.loadSkill(testSkillFile.absolutePath)
        assertNotNull(skill)
        
        val emailField = skill!!.prompts.find { it.field == "email" }
        assertEquals("email", emailField?.format)
        
        val phoneField = skill.prompts.find { it.field == "phone" }
        assertEquals("(XXX) XXX-XXXX", phoneField?.format)
    }
    
    @Test
    fun testSelectors() = runBlocking {
        val skill = skillEngine.loadSkill(testSkillFile.absolutePath)
        assertNotNull(skill)
        
        val fullNameSelectors = skill!!.selectors["full_name"]
        assertNotNull(fullNameSelectors)
        assertTrue(fullNameSelectors!!.contains("input[name='fullName']"))
        assertTrue(fullNameSelectors.contains("input[id='full-name']"))
        
        val emailSelectors = skill.selectors["email"]
        assertNotNull(emailSelectors)
        assertTrue(emailSelectors!!.contains("input[type='email']"))
    }
    
    @Test
    fun testPostprocessing() = runBlocking {
        val skill = skillEngine.loadSkill(testSkillFile.absolutePath)
        assertNotNull(skill)
        
        assertEquals("title_case", skill!!.postprocess["full_name"])
        assertEquals("format_phone", skill.postprocess["phone"])
    }
    
    @Test
    fun testCommands() = runBlocking {
        val skill = skillEngine.loadSkill(testSkillFile.absolutePath)
        assertNotNull(skill)
        
        assertEquals(1, skill!!.commands.size)
        val command = skill.commands[0]
        assertEquals("fill test form", command.trigger)
        assertEquals("fill_form", command.action)
        assertEquals("test_form", command.skill)
    }
    
    @Test
    fun testMatchCommand() = runBlocking {
        val skill = skillEngine.loadSkill(testSkillFile.absolutePath)
        assertNotNull(skill)
        
        val matchedCommand = skillEngine.matchCommand("fill test form", listOf(skill!!))
        assertNotNull("Command should match", matchedCommand)
        assertEquals("fill_form", matchedCommand?.action)
        
        val noMatch = skillEngine.matchCommand("invalid command", listOf(skill))
        assertNull("Invalid command should not match", noMatch)
    }
    
    @Test
    fun testValidateEmailFormat() {
        assertTrue(skillEngine.validateFormat("test@example.com", "email"))
        assertFalse(skillEngine.validateFormat("invalid-email", "email"))
        assertFalse(skillEngine.validateFormat("test@", "email"))
        assertFalse(skillEngine.validateFormat("@example.com", "email"))
    }
    
    @Test
    fun testValidatePhoneFormat() {
        assertTrue(skillEngine.validateFormat("(555) 123-4567", "(XXX) XXX-XXXX"))
        assertFalse(skillEngine.validateFormat("555-123-4567", "(XXX) XXX-XXXX"))
        assertFalse(skillEngine.validateFormat("555 123 4567", "(XXX) XXX-XXXX"))
        assertFalse(skillEngine.validateFormat("(555) 123-456", "(XXX) XXX-XXXX"))
    }
    
    @Test
    fun testPostprocessTitleCase() = runBlocking {
        val result = skillEngine.postprocessValue("john doe", "title_case")
        assertEquals("John Doe", result)
        
        val result2 = skillEngine.postprocessValue("MARY JANE SMITH", "title_case")
        assertEquals("Mary Jane Smith", result2)
    }
    
    @Test
    fun testPostprocessFormatPhone() = runBlocking {
        val result = skillEngine.postprocessValue("5551234567", "format_phone")
        assertEquals("(555) 123-4567", result)
        
        val result2 = skillEngine.postprocessValue("555-123-4567", "format_phone")
        assertEquals("(555) 123-4567", result2)
        
        val result3 = skillEngine.postprocessValue("555 123 4567", "format_phone")
        assertEquals("(555) 123-4567", result3)
    }
    
    @Test
    fun testEmptySkillFile() = runBlocking {
        val emptyFile = File.createTempFile("empty_skill", ".yaml")
        FileWriter(emptyFile).use { it.write("") }
        
        val skill = skillEngine.loadSkill(emptyFile.absolutePath)
        assertNull("Empty file should return null", skill)
        
        emptyFile.delete()
    }
    
    @Test
    fun testInvalidYamlFormat() = runBlocking {
        val invalidFile = File.createTempFile("invalid_skill", ".yaml")
        FileWriter(invalidFile).use { it.write("invalid: yaml: content: [") }
        
        val skill = skillEngine.loadSkill(invalidFile.absolutePath)
        assertNull("Invalid YAML should return null", skill)
        
        invalidFile.delete()
    }
    
    @Test
    fun testMissingRequiredFields() = runBlocking {
        val incompleteFile = File.createTempFile("incomplete_skill", ".yaml")
        val incompleteContent = """
            id: incomplete_form
            # Missing name and prompts
        """.trimIndent()
        
        FileWriter(incompleteFile).use { it.write(incompleteContent) }
        
        val skill = skillEngine.loadSkill(incompleteFile.absolutePath)
        assertNotNull("Should load even with missing optional fields", skill)
        assertEquals("incomplete_form", skill?.id)
        assertTrue("Prompts should be empty list", skill?.prompts?.isEmpty() == true)
        
        incompleteFile.delete()
    }
    
    @Test
    fun testCaseInsensitiveCommandMatching() = runBlocking {
        val skill = skillEngine.loadSkill(testSkillFile.absolutePath)
        assertNotNull(skill)
        
        val matchedCommand1 = skillEngine.matchCommand("FILL TEST FORM", listOf(skill!!))
        assertNotNull("Command should match case-insensitive", matchedCommand1)
        
        val matchedCommand2 = skillEngine.matchCommand("Fill Test Form", listOf(skill))
        assertNotNull("Command should match case-insensitive", matchedCommand2)
        
        val matchedCommand3 = skillEngine.matchCommand("fill test form", listOf(skill))
        assertNotNull("Command should match case-insensitive", matchedCommand3)
    }
    
    fun tearDown() {
        if (::testSkillFile.isInitialized) {
            testSkillFile.delete()
        }
    }
}