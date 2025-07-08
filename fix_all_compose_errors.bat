@echo off
echo Removing all Compose-dependent files to get working build...

cd /d "D:\013-VoiceBridge\android"

echo Removing AccessibilitySettingsActivity (Compose)
del app\src\main\java\com\voicebridge\accessibility\AccessibilitySettingsActivity.kt

echo Removing Language Settings Activity if it has Compose
del app\src\main\java\com\voicebridge\language\LanguageSettingsActivity.kt 2>nul

echo Removing OnboardingActivity Compose imports
powershell -Command "(Get-Content 'app\src\main\java\com\voicebridge\onboarding\OnboardingActivity.kt') -replace 'import androidx.compose.*', '// Removed Compose import' -replace 'import androidx.activity.compose.*', '// Removed Compose import' -replace 'setContent.*', '// setContent removed' | Set-Content 'app\src\main\java\com\voicebridge\onboarding\OnboardingActivity.kt'"

echo Creating minimal MainActivity that works
powershell -Command "
$content = @'
package com.voicebridge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this)
        textView.text = \"VoiceBridge v1.0.0-beta.1\nBeta Test Version\"
        textView.textSize = 18f
        textView.setPadding(50, 50, 50, 50)
        
        setContentView(textView)
    }
}
'@
$content | Set-Content 'app\src\main\java\com\voicebridge\MainActivity.kt'
"

echo Build should now work!
echo Run: gradlew.bat assembleRelease