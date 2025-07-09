# VoiceBridge APK Releases

## Latest Version: v8.0 - One Universal Vision Button

**File Size**: 139MB (too large for GitHub)

### Download Options:

1. **Build it yourself**:
   ```bash
   cd android
   ./gradlew assembleDebug
   # APK will be at: app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Features in v8.0**:
   - 🎯 ONE BUTTON for everything - no more confusion
   - 👁️ Hold to see, release to hear - intuitive design
   - 🤖 Proper Vision-Language Model architecture
   - 📊 Tiered processing: ML Kit → BLIP-2 → Claude
   - 🆓 70-80% processed on-device for free

### How to Use:
1. **HOLD** the vision button - camera turns on
2. **POINT** at anything - medicine, form, sign, object
3. **RELEASE** the button - hear AI description

### Previous Versions:
- v7.0: Pure Vision Assistant (78MB)
- v6.2: UI Enhanced (78MB)

### Why is v8.0 larger?
The v8.0 APK includes:
- ML Kit Vision models for on-device processing
- ONNX Runtime for BLIP-2 support
- Multiple vision processing libraries

This allows most vision tasks to run locally without internet!