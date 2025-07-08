# App Signing Guide for VoiceBridge

## 🔐 Overview
This guide covers how to sign your VoiceBridge app for Google Play Store distribution using the recommended Android App Bundle (AAB) format.

## Step 1: Generate Upload Keystore

### 1.1 Create Keystore File
```bash
# Navigate to android directory
cd android

# Generate keystore (replace with your information)
keytool -genkey -v -keystore voicebridge-upload-key.keystore \
  -alias voicebridge \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# You'll be prompted for:
# - Keystore password (store securely!)
# - Key password (can be same as keystore)
# - Your name and organization details
```

### 1.2 Keystore Security
⚠️ **CRITICAL**: Store your keystore file and passwords securely!
- **Backup** the keystore file in multiple secure locations
- **Never** commit keystore to version control
- **Use** a password manager for passwords
- **Lose this** = unable to update your app FOREVER

## Step 2: Configure Gradle for Signing

### 2.1 Create gradle.properties
Create `android/gradle.properties` (or add to existing):
```properties
# App Signing Configuration
VOICEBRIDGE_UPLOAD_STORE_FILE=voicebridge-upload-key.keystore
VOICEBRIDGE_UPLOAD_STORE_PASSWORD=your_keystore_password
VOICEBRIDGE_UPLOAD_KEY_ALIAS=voicebridge
VOICEBRIDGE_UPLOAD_KEY_PASSWORD=your_key_password
```

⚠️ **Add to .gitignore**:
```gitignore
# Signing keys
android/gradle.properties
android/*.keystore
android/*.jks
```

### 2.2 Update app/build.gradle
Add signing configuration to `android/app/build.gradle`:

```gradle
android {
    compileSdk 34

    defaultConfig {
        applicationId "com.voicebridge"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0-beta.1"
    }

    signingConfigs {
        release {
            if (project.hasProperty('VOICEBRIDGE_UPLOAD_STORE_FILE')) {
                storeFile file(VOICEBRIDGE_UPLOAD_STORE_FILE)
                storePassword VOICEBRIDGE_UPLOAD_STORE_PASSWORD
                keyAlias VOICEBRIDGE_UPLOAD_KEY_ALIAS
                keyPassword VOICEBRIDGE_UPLOAD_KEY_PASSWORD
            }
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            
            // Optimization for smaller APK
            ndk {
                abiFilters 'arm64-v8a', 'armeabi-v7a'
            }
        }
        
        debug {
            applicationIdSuffix ".debug"
            debuggable true
        }
    }
}
```

## Step 3: Configure ProGuard for Release

### 3.1 Update proguard-rules.pro
Add to `android/app/proguard-rules.pro`:

```proguard
# VoiceBridge specific rules
-keep class com.voicebridge.** { *; }

# JNI native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Whisper.cpp JNI
-keep class com.voicebridge.VoiceBridgeNative { *; }

# YAML parsing
-keep class org.yaml.snakeyaml.** { *; }
-dontwarn org.yaml.snakeyaml.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Accessibility services
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# Keep crash reporting classes
-keep class com.voicebridge.crash.** { *; }
-keep class com.voicebridge.telemetry.** { *; }

# Keep skill system classes
-keep class com.voicebridge.skills.** { *; }
-keep class com.voicebridge.language.** { *; }
```

## Step 4: Build Signed App Bundle

### 4.1 Clean and Build
```bash
cd android

# Clean previous builds
./gradlew clean

# Build signed App Bundle (recommended for Play Store)
./gradlew bundleRelease

# Build signed APK (for direct distribution)
./gradlew assembleRelease
```

### 4.2 Verify Outputs
```bash
# Check App Bundle
ls -la app/build/outputs/bundle/release/
# Should show: app-release.aab

# Check APK
ls -la app/build/outputs/apk/release/
# Should show: app-release.apk

# Verify signing
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

## Step 5: Test Signed Build

### 5.1 Install APK for Testing
```bash
# Install on connected device
adb install app/build/outputs/apk/release/app-release.apk

# Or use bundletool for AAB testing
java -jar bundletool.jar build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=voicebridge.apks \
  --connected-device

java -jar bundletool.jar install-apks --apks=voicebridge.apks
```

### 5.2 Verify App Functionality
- ✅ All features work correctly
- ✅ Speech recognition functions
- ✅ OCR scanning works
- ✅ Accessibility features enabled
- ✅ No crashes or ANRs
- ✅ Proper permissions requested

## Step 6: Upload to Play Console

### 6.1 Create Release
1. **Go to**: Play Console → VoiceBridge → Release → Internal testing
2. **Create new release**
3. **Upload**: `app-release.aab`
4. **Add release notes**:

```
VoiceBridge v1.0.0-beta.1 - Initial Release

🎉 Welcome to VoiceBridge - the privacy-first voice form automation app!

✨ Key Features:
• 🎤 Offline voice recognition in 12 languages
• ♿ Full accessibility support (WCAG 2.1 AA)
• 🔒 100% offline operation - no data transmitted
• 📋 Smart form filling with voice commands
• 📄 Document scanning with OCR text extraction

🛠️ What's New:
• Initial public release
• Complete offline AI processing
• Multi-language skill templates
• Professional accessibility compliance
• Privacy-preserving analytics
• Community contribution system

🔒 Privacy & Security:
• All processing happens on your device
• No internet connection required after setup
• Transparent permissions and data handling
• Open source with public code review

Thank you for testing VoiceBridge! Please report any issues or feedback.
```

### 6.2 Internal Testing
1. **Add test users**: Use email addresses
2. **Share test link**: Send to testers
3. **Collect feedback**: Monitor crashes and reviews
4. **Iterate**: Fix issues, upload new versions

## Step 7: Version Management

### 7.1 Version Code Strategy
For each release, increment version code:
```gradle
android {
    defaultConfig {
        versionCode 2  // Increment for each upload
        versionName "1.0.0-beta.2"  // Semantic versioning
    }
}
```

### 7.2 Release Channels
- **Internal**: Team testing (unlimited versions)
- **Closed**: Beta testers (50,000 users max)
- **Open**: Public beta (unlimited users)
- **Production**: Live app (staged rollout recommended)

## Troubleshooting

### Common Signing Issues

**"App not installed" error**:
- Different signing keys between debug/release
- Version conflict with existing app
- Insufficient storage space

**ProGuard issues**:
- Add keep rules for reflection-based libraries
- Test thoroughly after ProGuard changes
- Use mapping files for crash debugging

**AAB upload rejected**:
- Ensure proper signing configuration
- Check target SDK version (must be recent)
- Verify all required native libraries included

### Size Optimization

**Reduce app size**:
```gradle
android {
    buildTypes {
        release {
            shrinkResources true
            minifyEnabled true
            
            // Split APKs by ABI
            splits {
                abi {
                    enable true
                    reset()
                    include 'arm64-v8a', 'armeabi-v7a'
                    universalApk false
                }
            }
        }
    }
}
```

## Security Best Practices

### 7.1 Keystore Security
- **Use** strong passwords (16+ characters)
- **Store** keystore in secure cloud storage with versioning
- **Document** recovery procedures
- **Test** signing process regularly

### 7.2 Code Security
- **Enable** R8 full mode for better obfuscation
- **Remove** debug logs in release builds
- **Validate** all inputs from external sources
- **Use** Android Keystore for sensitive data

## Next Steps

1. **Complete internal testing** with signed builds
2. **Move to closed testing** for broader feedback
3. **Prepare for production** release with staged rollout
4. **Set up crash reporting** monitoring in Play Console
5. **Plan update schedule** for ongoing maintenance

Remember: The signing key is permanent for your app. Treat it like the crown jewels of your application! 👑