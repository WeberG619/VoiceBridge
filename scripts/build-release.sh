#!/bin/bash
# VoiceBridge Release Build Script
# Builds production-ready APK and AAB for Google Play Store

set -e

echo "🏗️ VoiceBridge Production Build"
echo "=============================="

# Check if we're in the right directory
if [ ! -f "android/app/build.gradle" ]; then
    echo "❌ Error: Run this script from the VoiceBridge root directory"
    exit 1
fi

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ]; then
    echo "❌ Error: ANDROID_HOME environment variable not set"
    echo "Please install Android Studio and set ANDROID_HOME"
    exit 1
fi

cd android

echo "🧹 Cleaning previous builds..."
./gradlew clean

echo "🔧 Building release APK..."
./gradlew assembleRelease

echo "📦 Building release AAB (App Bundle)..."
./gradlew bundleRelease

echo "✅ Build complete!"
echo ""
echo "📁 Output files:"
echo "APK: android/app/build/outputs/apk/release/app-release.apk"
echo "AAB: android/app/build/outputs/bundle/release/app-release.aab"
echo ""
echo "⚠️  Note: These are unsigned builds. You'll need to sign them for distribution."
echo "   See docs/signing-guide.md for signing instructions."

# Create output summary
echo "📊 Build Summary:"
echo "================"
APK_SIZE=$(du -h app/build/outputs/apk/release/app-release.apk 2>/dev/null | cut -f1 || echo "N/A")
AAB_SIZE=$(du -h app/build/outputs/bundle/release/app-release.aab 2>/dev/null | cut -f1 || echo "N/A")
echo "APK Size: $APK_SIZE"
echo "AAB Size: $AAB_SIZE"
echo "Version: $(grep versionName app/build.gradle | awk '{print $2}' | tr -d '"')"
echo "Build Date: $(date)"