#!/bin/bash

# VoiceBridge Screenshot Generation Script
# Generates actual app screenshots for Google Play Store

set -e

echo "Generating VoiceBridge app screenshots..."

# Check if we're in the right directory
if [ ! -f "../../android/gradlew" ]; then
    echo "Error: Run this script from tools/screenshot-generator directory"
    exit 1
fi

# Create output directory
OUTPUT_DIR="../../store/graphics/screenshots"
mkdir -p "$OUTPUT_DIR/phone"
mkdir -p "$OUTPUT_DIR/tablet"

# Build screenshot generator
echo "Building screenshot generator..."
cd ../../android
./gradlew :tools:screenshot-generator:assembleDebug

# Run screenshot tests on connected device/emulator
echo "Running screenshot tests..."
./gradlew :tools:screenshot-generator:connectedAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.screenshot=true \
    -Pandroid.testInstrumentationRunnerArguments.outputDir="$OUTPUT_DIR"

# Alternative: Use Android Studio's built-in screenshot tool
cat << 'EOF'

=== Manual Screenshot Capture Instructions ===

If automated screenshots fail, capture manually:

1. Launch VoiceBridge on device/emulator
2. For each screen:
   - Navigate to the feature
   - Press Power + Volume Down (device)
   - Or use Android Studio's screenshot button

3. Recommended screenshots:
   
   Phone (1080x1920 or 9:16 ratio):
   - Main screen with microphone button
   - Voice recording in progress
   - Document scanning camera view
   - OCR results display
   - Form being filled automatically
   - Accessibility settings
   - Dark mode view
   - Onboarding tutorial
   
   Tablet (1920x1080 or 16:9 ratio):
   - Split-screen form filling
   - Document batch processing
   - Multi-column layout
   - Landscape orientation

4. Edit screenshots:
   - Add device frames (optional)
   - Ensure no personal data visible
   - Highlight key features
   - Maintain consistent style

5. Save to store/graphics/screenshots/

EOF

# Generate screenshot metadata
cat > "$OUTPUT_DIR/screenshot_descriptions.txt" << 'EOF'
Screenshot Descriptions for Play Store:

1. Voice Input (Main Feature)
   - Title: "Speak Naturally"
   - Caption: "Fill forms with your voice - no typing needed"

2. Document Scanning
   - Title: "Scan Any Document"
   - Caption: "Extract text instantly with advanced OCR"

3. Form Automation
   - Title: "Smart Form Filling"
   - Caption: "AI understands and fills forms automatically"

4. Accessibility Features
   - Title: "Designed for Everyone"
   - Caption: "Full accessibility support built-in"

5. Dark Mode
   - Title: "Easy on the Eyes"
   - Caption: "Beautiful dark theme for night use"

6. Privacy Dashboard
   - Title: "Your Data, Your Device"
   - Caption: "100% offline - no cloud, no tracking"

7. Multi-Language
   - Title: "Speaks Your Language"
   - Caption: "Support for 10+ languages"

8. Onboarding
   - Title: "Get Started Quickly"
   - Caption: "Simple setup in under 2 minutes"
EOF

echo "Screenshot generation complete!"
echo "Check output at: $OUTPUT_DIR"
echo "Remember to review and edit before uploading to Play Store"