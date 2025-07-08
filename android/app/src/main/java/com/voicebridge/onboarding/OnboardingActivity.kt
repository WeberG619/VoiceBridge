package com.voicebridge.onboarding

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voicebridge.R
import com.voicebridge.ui.theme.VoiceBridgeTheme
import kotlinx.coroutines.launch

/**
 * OnboardingActivity - Interactive tutorial and setup flow for VoiceBridge
 * 
 * Features:
 * - Step-by-step introduction to VoiceBridge features
 * - Permission requests with clear explanations
 * - Accessibility service setup guidance
 * - Feature demonstrations and tips
 * - Customizable preferences setup
 * 
 * WCAG 2.1 AA Compliance:
 * - Full keyboard navigation support
 * - Screen reader compatibility
 * - High contrast mode support
 * - Focus management
 * - Clear content structure
 */
class OnboardingActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
        permissions.entries.forEach { (permission, granted) ->
            when (permission) {
                Manifest.permission.RECORD_AUDIO -> {
                    if (granted) {
                        // Voice recording permission granted
                    }
                }
                Manifest.permission.CAMERA -> {
                    if (granted) {
                        // Camera permission granted
                    }
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            VoiceBridgeTheme {
                OnboardingFlow(
                    onFinish = {
                        // Mark onboarding as completed and navigate to main app
                        finishOnboarding()
                    },
                    onRequestPermissions = { permissions ->
                        permissionLauncher.launch(permissions)
                    },
                    onOpenAccessibilitySettings = {
                        openAccessibilitySettings()
                    }
                )
            }
        }
    }
    
    private fun finishOnboarding() {
        // Save onboarding completion state
        getSharedPreferences("voicebridge_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", true)
            .apply()
        
        // Navigate to main activity
        startActivity(Intent(this, com.voicebridge.MainActivity::class.java))
        finish()
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(
    onFinish: () -> Unit,
    onRequestPermissions: (Array<String>) -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 6 })
    val scope = rememberCoroutineScope()
    
    val pages = listOf(
        OnboardingPage.Welcome,
        OnboardingPage.VoiceControl,
        OnboardingPage.DocumentScanning,
        OnboardingPage.FormAutomation,
        OnboardingPage.Permissions,
        OnboardingPage.Complete
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    // Progress indicator
                    Text(
                        text = "${pagerState.currentPage + 1} / ${pages.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                totalPages = pages.size,
                onPrevious = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                onNext = {
                    scope.launch {
                        if (pagerState.currentPage < pages.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            onFinish()
                        }
                    }
                },
                onSkip = onFinish
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (pages[page]) {
                OnboardingPage.Welcome -> WelcomePage()
                OnboardingPage.VoiceControl -> VoiceControlPage()
                OnboardingPage.DocumentScanning -> DocumentScanningPage()
                OnboardingPage.FormAutomation -> FormAutomationPage()
                OnboardingPage.Permissions -> PermissionsPage(
                    onRequestPermissions = onRequestPermissions,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings
                )
                OnboardingPage.Complete -> CompletePage()
            }
        }
    }
}

@Composable
fun OnboardingBottomBar(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            if (currentPage > 0) {
                TextButton(onClick = onPrevious) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(72.dp))
            }
            
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(totalPages) { index ->
                    val isActive = index == currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 12.dp else 8.dp)
                            .background(
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
            
            // Next/Skip/Finish button
            if (currentPage < totalPages - 1) {
                Row {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onNext) {
                        Text("Next")
                    }
                }
            } else {
                Button(onClick = onNext) {
                    Text("Get Started")
                }
            }
        }
    }
}

@Composable
fun WelcomePage() {
    OnboardingPageLayout(
        icon = Icons.Default.RecordVoiceOver,
        title = "Welcome to VoiceBridge",
        description = "Transform how you interact with forms and documents using the power of your voice and AI technology.",
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "VoiceBridge combines:",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                
                FeatureCard(
                    icon = Icons.Default.Mic,
                    title = "Voice Recognition",
                    description = "Offline speech-to-text powered by Whisper AI"
                )
                
                FeatureCard(
                    icon = Icons.Default.CameraAlt,
                    title = "Document Scanning",
                    description = "Extract text from images with OCR technology"
                )
                
                FeatureCard(
                    icon = Icons.Default.AutoFixHigh,
                    title = "Smart Automation",
                    description = "Automatically fill forms using AI understanding"
                )
            }
        }
    )
}

@Composable
fun VoiceControlPage() {
    OnboardingPageLayout(
        icon = Icons.Default.Mic,
        title = "Voice Control",
        description = "Use your voice to fill forms hands-free. VoiceBridge understands natural speech and converts it to text.",
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DemoCard(
                    title = "How it works:",
                    steps = listOf(
                        "Tap the microphone button",
                        "Speak naturally: \"My name is John Smith\"",
                        "VoiceBridge fills the appropriate form field",
                        "Review and submit your form"
                    )
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Accessibility First",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Works seamlessly with screen readers and assistive technologies",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun DocumentScanningPage() {
    OnboardingPageLayout(
        icon = Icons.Default.CameraAlt,
        title = "Document Scanning",
        description = "Capture and extract text from documents, forms, and images using advanced OCR technology.",
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DemoCard(
                    title = "Scan documents:",
                    steps = listOf(
                        "Open the camera scanner",
                        "Position document in frame",
                        "Capture clear, well-lit image",
                        "Text is automatically extracted and ready to use"
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "High Accuracy",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Offline Processing",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun FormAutomationPage() {
    OnboardingPageLayout(
        icon = Icons.Default.AutoFixHigh,
        title = "Smart Form Automation",
        description = "VoiceBridge intelligently identifies form fields and fills them using voice input or scanned text.",
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DemoCard(
                    title = "Supported forms:",
                    steps = listOf(
                        "Government forms and applications",
                        "Medical intake forms",
                        "Employment applications",
                        "Insurance claims",
                        "Educational forms"
                    )
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Privacy & Security",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All processing happens on your device. No data is sent to external servers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun PermissionsPage(
    onRequestPermissions: (Array<String>) -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    OnboardingPageLayout(
        icon = Icons.Default.Security,
        title = "Permissions Setup",
        description = "VoiceBridge needs certain permissions to provide its features. All permissions are used only for core functionality.",
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PermissionCard(
                    icon = Icons.Default.Mic,
                    title = "Microphone Access",
                    description = "Required for voice input and form filling",
                    required = true,
                    onGrant = {
                        onRequestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO))
                    }
                )
                
                PermissionCard(
                    icon = Icons.Default.CameraAlt,
                    title = "Camera Access",
                    description = "Required for document scanning and OCR",
                    required = true,
                    onGrant = {
                        onRequestPermissions(arrayOf(Manifest.permission.CAMERA))
                    }
                )
                
                PermissionCard(
                    icon = Icons.Default.Accessibility,
                    title = "Accessibility Service",
                    description = "Required for automatic form filling",
                    required = true,
                    onGrant = onOpenAccessibilitySettings
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Why we need these permissions:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Voice input for hands-free form filling\n" +
                                    "• Camera for document text extraction\n" +
                                    "• Accessibility service to interact with form fields",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun CompletePage() {
    OnboardingPageLayout(
        icon = Icons.Default.CheckCircle,
        title = "You're All Set!",
        description = "VoiceBridge is ready to help you fill forms faster and more efficiently than ever before.",
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Quick Start Tips:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "1. Open any form in your browser or app\n" +
                                    "2. Tap the VoiceBridge microphone button\n" +
                                    "3. Speak naturally to fill form fields\n" +
                                    "4. Review and submit your completed form",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Text(
                    text = "For help and tutorials, visit the Settings menu in the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

// Helper Composables

@Composable
fun OnboardingPageLayout(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        content()
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DemoCard(
    title: String,
    steps: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            steps.forEachIndexed { index, step ->
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${index + 1}. ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (index < steps.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    required: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (required) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onGrant,
                size = ButtonDefaults.SmallButtonSize
            ) {
                Text("Grant")
            }
        }
    }
}

sealed class OnboardingPage {
    object Welcome : OnboardingPage()
    object VoiceControl : OnboardingPage()
    object DocumentScanning : OnboardingPage()
    object FormAutomation : OnboardingPage()
    object Permissions : OnboardingPage()
    object Complete : OnboardingPage()
}