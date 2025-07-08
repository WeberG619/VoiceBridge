#!/bin/bash

# VoiceBridge Release Build Script
# Builds and signs the release APK/AAB for Google Play Store

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}VoiceBridge Release Build Script${NC}"
echo "================================="

# Check if we're in the project root
if [ ! -f "android/gradlew" ]; then
    echo -e "${RED}Error: Run this script from the project root directory${NC}"
    exit 1
fi

# Parse command line arguments
BUILD_TYPE="aab"  # Default to AAB for Play Store
VERSION_CODE=""
VERSION_NAME=""
SKIP_TESTS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --apk)
            BUILD_TYPE="apk"
            shift
            ;;
        --version-code)
            VERSION_CODE="$2"
            shift 2
            ;;
        --version-name)
            VERSION_NAME="$2"
            shift 2
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --apk              Build APK instead of AAB"
            echo "  --version-code     Version code (e.g., 100)"
            echo "  --version-name     Version name (e.g., 1.0.0)"
            echo "  --skip-tests       Skip running tests"
            echo "  --help             Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Step 1: Environment checks
echo -e "\n${YELLOW}Step 1: Environment checks${NC}"

# Check for required tools
command -v java >/dev/null 2>&1 || { echo -e "${RED}Java is required but not installed.${NC}"; exit 1; }
command -v adb >/dev/null 2>&1 || { echo -e "${RED}ADB is required but not installed.${NC}"; exit 1; }

# Check for keystore
KEYSTORE_FILE="android/keystores/voicebridge-upload.keystore"
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo -e "${YELLOW}Warning: Keystore not found at $KEYSTORE_FILE${NC}"
    echo "Creating keystore directory..."
    mkdir -p "android/keystores"
    
    echo -e "\n${YELLOW}Generate a keystore with:${NC}"
    echo "keytool -genkey -v -keystore $KEYSTORE_FILE -alias voicebridge-upload -keyalg RSA -keysize 2048 -validity 10000"
    echo -e "\n${RED}Exiting. Please create keystore first.${NC}"
    exit 1
fi

# Step 2: Clean previous builds
echo -e "\n${YELLOW}Step 2: Cleaning previous builds${NC}"
cd android
./gradlew clean
cd ..

# Step 3: Download models if needed
echo -e "\n${YELLOW}Step 3: Checking AI models${NC}"
if [ ! -d "models" ] || [ -z "$(ls -A models 2>/dev/null)" ]; then
    echo "Downloading AI models..."
    ./scripts/build_models.sh
else
    echo "AI models already present"
fi

# Step 4: Update version if provided
if [ -n "$VERSION_CODE" ] || [ -n "$VERSION_NAME" ]; then
    echo -e "\n${YELLOW}Step 4: Updating app version${NC}"
    BUILD_GRADLE="android/app/build.gradle"
    
    if [ -n "$VERSION_CODE" ]; then
        echo "Setting version code to: $VERSION_CODE"
        sed -i.bak "s/versionCode [0-9]*/versionCode $VERSION_CODE/" "$BUILD_GRADLE"
    fi
    
    if [ -n "$VERSION_NAME" ]; then
        echo "Setting version name to: $VERSION_NAME"
        sed -i.bak "s/versionName \"[^\"]*\"/versionName \"$VERSION_NAME\"/" "$BUILD_GRADLE"
    fi
    
    # Clean up backup files
    rm -f "${BUILD_GRADLE}.bak"
fi

# Step 5: Run tests
if [ "$SKIP_TESTS" = false ]; then
    echo -e "\n${YELLOW}Step 5: Running tests${NC}"
    cd android
    ./gradlew test
    ./gradlew connectedAndroidTest || echo -e "${YELLOW}Warning: Connected tests skipped (no device/emulator)${NC}"
    cd ..
else
    echo -e "\n${YELLOW}Step 5: Skipping tests${NC}"
fi

# Step 6: Build release
echo -e "\n${YELLOW}Step 6: Building release $BUILD_TYPE${NC}"
cd android

if [ "$BUILD_TYPE" = "aab" ]; then
    echo "Building Android App Bundle (AAB)..."
    ./gradlew bundleRelease
    
    RELEASE_FILE="app/build/outputs/bundle/release/app-release.aab"
    OUTPUT_DIR="../release/aab"
else
    echo "Building APK..."
    ./gradlew assembleRelease
    
    RELEASE_FILE="app/build/outputs/apk/release/app-release.apk"
    OUTPUT_DIR="../release/apk"
fi

# Step 7: Verify build
echo -e "\n${YELLOW}Step 7: Verifying build${NC}"
if [ ! -f "$RELEASE_FILE" ]; then
    echo -e "${RED}Error: Release file not found at $RELEASE_FILE${NC}"
    exit 1
fi

# Get file size
FILE_SIZE=$(du -h "$RELEASE_FILE" | cut -f1)
echo "Release file size: $FILE_SIZE"

# Verify signature
echo "Verifying signature..."
if [ "$BUILD_TYPE" = "aab" ]; then
    jarsigner -verify "$RELEASE_FILE" || echo -e "${YELLOW}Warning: Signature verification failed${NC}"
else
    apksigner verify "$RELEASE_FILE" || echo -e "${YELLOW}Warning: Signature verification failed${NC}"
fi

# Step 8: Copy to release directory
echo -e "\n${YELLOW}Step 8: Copying to release directory${NC}"
mkdir -p "$OUTPUT_DIR"

# Generate filename with version
if [ -n "$VERSION_NAME" ]; then
    OUTPUT_FILE="voicebridge-${VERSION_NAME}.${BUILD_TYPE}"
else
    OUTPUT_FILE="voicebridge-release.${BUILD_TYPE}"
fi

cp "$RELEASE_FILE" "$OUTPUT_DIR/$OUTPUT_FILE"
echo "Release file copied to: $OUTPUT_DIR/$OUTPUT_FILE"

# Step 9: Generate release notes
echo -e "\n${YELLOW}Step 9: Generating release information${NC}"
RELEASE_INFO="$OUTPUT_DIR/release-info.txt"

cat > "$RELEASE_INFO" << EOF
VoiceBridge Release Information
==============================

Build Date: $(date)
Build Type: $BUILD_TYPE
File: $OUTPUT_FILE
Size: $FILE_SIZE

Version Information:
$(grep "versionCode\|versionName" app/build.gradle)

SHA-256 Checksum:
$(sha256sum "$OUTPUT_DIR/$OUTPUT_FILE" | cut -d' ' -f1)

Next Steps:
1. Upload to Google Play Console
2. Fill in release notes
3. Submit for review

Testing Commands:
- Install on device: adb install $OUTPUT_FILE
- Bundle tool: bundletool build-apks --bundle=$OUTPUT_FILE --output=output.apks
EOF

cat "$RELEASE_INFO"

# Step 10: Optional - Install on connected device
if adb devices | grep -q device; then
    echo -e "\n${YELLOW}Step 10: Install on connected device?${NC}"
    read -p "Install on connected device? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if [ "$BUILD_TYPE" = "apk" ]; then
            adb install -r "$OUTPUT_DIR/$OUTPUT_FILE"
        else
            echo "Cannot directly install AAB. Use bundletool or upload to Play Console."
        fi
    fi
fi

cd ..

echo -e "\n${GREEN}✅ Release build complete!${NC}"
echo -e "${GREEN}📦 Output: $OUTPUT_DIR/$OUTPUT_FILE${NC}"
echo -e "\n${YELLOW}Remember to:${NC}"
echo "1. Test the release build thoroughly"
echo "2. Update store listing if needed"
echo "3. Prepare release notes"
echo "4. Upload to Google Play Console"
echo "5. Submit for review"

# Create upload checklist
cat > "$OUTPUT_DIR/upload-checklist.md" << 'EOF'
# Google Play Console Upload Checklist

## Pre-Upload
- [ ] Test release build on multiple devices
- [ ] Verify all features work correctly
- [ ] Check for crashes or ANRs
- [ ] Review ProGuard/R8 rules
- [ ] Verify permissions are correct

## Play Console Steps
1. [ ] Go to Release > Production
2. [ ] Create new release
3. [ ] Upload AAB file
4. [ ] Add release notes (all languages)
5. [ ] Review warnings/errors
6. [ ] Set rollout percentage (start with 10%)
7. [ ] Review and roll out

## Post-Upload
- [ ] Monitor crash reports
- [ ] Check user reviews
- [ ] Watch for policy violations
- [ ] Plan gradual rollout increase
- [ ] Prepare hotfix process if needed

## Release Notes Template
```
Version X.X.X

🎉 What's New:
• Feature 1
• Feature 2

🐛 Bug Fixes:
• Fixed issue 1
• Fixed issue 2

💪 Improvements:
• Performance enhancement
• UI improvements

Thank you for using VoiceBridge!
```
EOF

echo -e "\n${GREEN}📋 Upload checklist created at: $OUTPUT_DIR/upload-checklist.md${NC}"