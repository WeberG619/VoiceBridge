package com.voicebridge.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.voicebridge.skills.AccessibilityConfig
import com.voicebridge.skills.Skill
import com.voicebridge.skills.SkillExecutionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VoiceBridgeAccessibilityService : AccessibilityService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentSkill: Skill? = null
    private var skillExecutionResult: SkillExecutionResult? = null
    
    companion object {
        private const val TAG = "VoiceBridgeA11y"
        const val ACTION_FILL_FORM = "com.voicebridge.FILL_FORM"
        const val EXTRA_SKILL_RESULT = "skill_result"
        
        var instance: VoiceBridgeAccessibilityService? = null
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "VoiceBridge Accessibility Service created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "VoiceBridge Accessibility Service destroyed")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d(TAG, "Window state changed: ${event.packageName}")
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                Log.d(TAG, "View focused: ${event.className}")
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.d(TAG, "View clicked: ${event.className}")
            }
        }
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FILL_FORM -> {
                val skillResult = intent.getSerializableExtra(EXTRA_SKILL_RESULT) as? SkillExecutionResult
                skillResult?.let { fillForm(it) }
            }
        }
        return START_NOT_STICKY
    }
    
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        
        // Check if this matches our target app for current skill
        currentSkill?.accessibility?.targetApp?.let { targetApp ->
            if (packageName == targetApp) {
                Log.i(TAG, "Target app detected: $targetApp")
                // Auto-fill if we have execution result ready
                skillExecutionResult?.let { fillForm(it) }
            }
        }
    }
    
    fun fillForm(skillResult: SkillExecutionResult) {
        serviceScope.launch {
            try {
                val accessibilityConfig = skillResult.skill.accessibility
                if (accessibilityConfig == null) {
                    Log.w(TAG, "No accessibility configuration for skill ${skillResult.skill.id}")
                    return@launch
                }
                
                Log.i(TAG, "Starting form fill for skill: ${skillResult.skill.name}")
                
                val rootNode = rootInActiveWindow
                if (rootNode == null) {
                    Log.e(TAG, "Cannot access current window")
                    showToast("Cannot access current window")
                    return@launch
                }
                
                var fieldsFound = 0
                var fieldsFilled = 0
                
                // Fill each field based on selectors
                for ((fieldName, selector) in accessibilityConfig.formSelectors) {
                    val value = skillResult.processedData[fieldName]
                    if (value != null) {
                        fieldsFound++
                        if (fillField(rootNode, selector, value)) {
                            fieldsFilled++
                            Log.d(TAG, "Filled field: $fieldName = $value")
                        } else {
                            Log.w(TAG, "Failed to fill field: $fieldName")
                        }
                    }
                }
                
                Log.i(TAG, "Form fill completed: $fieldsFilled/$fieldsFound fields filled")
                showToast("Filled $fieldsFilled/$fieldsFound form fields")
                
                // Auto-submit if configured and all fields filled
                if (fieldsFilled == fieldsFound && accessibilityConfig.submitButton != null) {
                    kotlinx.coroutines.delay(1000) // Brief delay before submitting
                    submitForm(rootNode, accessibilityConfig.submitButton)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error filling form", e)
                showToast("Error filling form: ${e.message}")
            }
        }
    }
    
    private fun fillField(rootNode: AccessibilityNodeInfo, selector: String, value: String): Boolean {
        // Find node by selector (simplified - in production would support CSS-like selectors)
        val targetNode = findNodeBySelector(rootNode, selector)
        
        if (targetNode == null) {
            Log.w(TAG, "Node not found for selector: $selector")
            return false
        }
        
        return when {
            targetNode.isEditable -> {
                // Text input field
                fillTextNode(targetNode, value)
            }
            targetNode.className == "android.widget.Spinner" || 
            targetNode.className == "android.widget.AutoCompleteTextView" -> {
                // Dropdown/Spinner
                selectFromDropdown(targetNode, value)
            }
            targetNode.isClickable -> {
                // Button or clickable element
                performClick(targetNode)
            }
            else -> {
                Log.w(TAG, "Unsupported node type: ${targetNode.className}")
                false
            }
        }
    }
    
    private fun findNodeBySelector(rootNode: AccessibilityNodeInfo, selector: String): AccessibilityNodeInfo? {
        // Simplified selector parsing - supports basic attribute matching
        return when {
            selector.contains("input[name=") -> {
                val nameValue = selector.substringAfter("name='").substringBefore("']")
                findNodeByResourceId(rootNode, nameValue)
            }
            selector.contains("select[name=") -> {
                val nameValue = selector.substringAfter("name='").substringBefore("']")
                findNodeByResourceId(rootNode, nameValue)
            }
            selector.contains("button[type=") -> {
                findNodeByText(rootNode, "Submit") ?: findNodeByClassName(rootNode, "android.widget.Button")
            }
            else -> {
                // Try to find by resource ID directly
                findNodeByResourceId(rootNode, selector)
            }
        }
    }
    
    private fun findNodeByResourceId(rootNode: AccessibilityNodeInfo, resourceId: String): AccessibilityNodeInfo? {
        if (rootNode.viewIdResourceName?.contains(resourceId) == true) {
            return rootNode
        }
        
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            child?.let { 
                val result = findNodeByResourceId(it, resourceId)
                if (result != null) return result
            }
        }
        
        return null
    }
    
    private fun findNodeByText(rootNode: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (rootNode.text?.toString()?.contains(text, ignoreCase = true) == true) {
            return rootNode
        }
        
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            child?.let {
                val result = findNodeByText(it, text)
                if (result != null) return result
            }
        }
        
        return null
    }
    
    private fun findNodeByClassName(rootNode: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (rootNode.className?.toString() == className) {
            return rootNode
        }
        
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            child?.let {
                val result = findNodeByClassName(it, className)
                if (result != null) return result
            }
        }
        
        return null
    }
    
    private fun fillTextNode(node: AccessibilityNodeInfo, value: String): Boolean {
        return try {
            // Method 1: Use Bundle to set text
            val arguments = android.os.Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } catch (e: Exception) {
            Log.e(TAG, "Error filling text node", e)
            false
        }
    }
    
    private fun selectFromDropdown(node: AccessibilityNodeInfo, value: String): Boolean {
        return try {
            // Click to open dropdown
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            
            // Wait briefly for dropdown to open
            kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.delay(500)
            }
            
            // Find and click the matching option
            val rootNode = rootInActiveWindow
            rootNode?.let { root ->
                val optionNode = findNodeByText(root, value)
                optionNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting from dropdown", e)
            false
        }
    }
    
    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        return try {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing click", e)
            false
        }
    }
    
    private fun submitForm(rootNode: AccessibilityNodeInfo, submitSelector: String) {
        val submitNode = findNodeBySelector(rootNode, submitSelector)
        if (submitNode != null) {
            performClick(submitNode)
            Log.i(TAG, "Form submitted")
            showToast("Form submitted successfully")
        } else {
            Log.w(TAG, "Submit button not found")
        }
    }
    
    fun performGesture(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        dispatchGesture(gesture, null, null)
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    fun setCurrentSkill(skill: Skill, executionResult: SkillExecutionResult) {
        currentSkill = skill
        skillExecutionResult = executionResult
        Log.i(TAG, "Set current skill: ${skill.name}")
    }
}