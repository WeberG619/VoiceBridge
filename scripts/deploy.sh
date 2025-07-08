#!/bin/bash

# VoiceBridge Deployment Script
# Handles APK signing, distribution, and deployment

set -e

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$PROJECT_ROOT/android"
DEPLOY_DIR="$PROJECT_ROOT/deploy"
BUILD_TYPE="${1:-release}"
DEPLOY_TARGET="${2:-local}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case "$level" in
        "INFO")  echo -e "${BLUE}[INFO]${NC}  $timestamp - $message" ;;
        "WARN")  echo -e "${YELLOW}[WARN]${NC}  $timestamp - $message" ;;
        "ERROR") echo -e "${RED}[ERROR]${NC} $timestamp - $message" ;;
        "SUCCESS") echo -e "${GREEN}[SUCCESS]${NC} $timestamp - $message" ;;
    esac
}

# Create deployment directory
create_deploy_dir() {
    log "INFO" "Creating deployment directory..."
    mkdir -p "$DEPLOY_DIR"
    mkdir -p "$DEPLOY_DIR/signed"
    mkdir -p "$DEPLOY_DIR/metadata"
}

# Check if APK exists
check_apk() {
    local apk_path="$PROJECT_ROOT/voicebridge-${BUILD_TYPE}.apk"
    
    if [ ! -f "$apk_path" ]; then
        log "ERROR" "APK not found: $apk_path"
        log "ERROR" "Please run build.sh first"
        exit 1
    fi
    
    log "SUCCESS" "APK found: $apk_path"
}

# Generate keystore if needed
generate_keystore() {
    local keystore_path="$DEPLOY_DIR/voicebridge.keystore"
    
    if [ ! -f "$keystore_path" ]; then
        log "INFO" "Generating new keystore..."
        
        # Use environment variables or defaults
        local key_alias="${VOICEBRIDGE_KEY_ALIAS:-voicebridge}"
        local key_password="${VOICEBRIDGE_KEY_PASSWORD:-voicebridge123}"
        local store_password="${VOICEBRIDGE_STORE_PASSWORD:-voicebridge123}"
        
        keytool -genkey -v -keystore "$keystore_path" \
            -alias "$key_alias" \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -storepass "$store_password" \
            -keypass "$key_password" \
            -dname "CN=VoiceBridge, OU=Development, O=VoiceBridge, L=City, S=State, C=US"
        
        log "SUCCESS" "Keystore generated: $keystore_path"
        log "WARN" "Store this keystore securely for production releases!"
    else
        log "INFO" "Using existing keystore: $keystore_path"
    fi
}

# Sign APK
sign_apk() {
    local unsigned_apk="$PROJECT_ROOT/voicebridge-${BUILD_TYPE}.apk"
    local signed_apk="$DEPLOY_DIR/signed/voicebridge-${BUILD_TYPE}-signed.apk"
    local keystore_path="$DEPLOY_DIR/voicebridge.keystore"
    
    log "INFO" "Signing APK..."
    
    # Use environment variables or defaults
    local key_alias="${VOICEBRIDGE_KEY_ALIAS:-voicebridge}"
    local key_password="${VOICEBRIDGE_KEY_PASSWORD:-voicebridge123}"
    local store_password="${VOICEBRIDGE_STORE_PASSWORD:-voicebridge123}"
    
    # Check if jarsigner is available
    if ! command -v jarsigner >/dev/null 2>&1; then
        log "ERROR" "jarsigner not found. Please install Java JDK"
        exit 1
    fi
    
    # Sign the APK
    jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
        -keystore "$keystore_path" \
        -storepass "$store_password" \
        -keypass "$key_password" \
        -signedjar "$signed_apk" \
        "$unsigned_apk" \
        "$key_alias"
    
    # Verify signature
    if jarsigner -verify -verbose -certs "$signed_apk"; then
        log "SUCCESS" "APK signed successfully: $signed_apk"
    else
        log "ERROR" "APK signature verification failed"
        exit 1
    fi
}

# Align APK (zipalign)
align_apk() {
    local signed_apk="$DEPLOY_DIR/signed/voicebridge-${BUILD_TYPE}-signed.apk"
    local aligned_apk="$DEPLOY_DIR/signed/voicebridge-${BUILD_TYPE}-aligned.apk"
    
    log "INFO" "Aligning APK..."
    
    # Find zipalign tool
    local zipalign_path=""
    if [ -n "$ANDROID_HOME" ]; then
        zipalign_path="$ANDROID_HOME/build-tools/*/zipalign"
        zipalign_path=$(ls $zipalign_path 2>/dev/null | head -n1)
    fi
    
    if [ -z "$zipalign_path" ] || [ ! -f "$zipalign_path" ]; then
        log "ERROR" "zipalign not found. Please install Android SDK build-tools"
        exit 1
    fi
    
    # Align the APK
    "$zipalign_path" -v 4 "$signed_apk" "$aligned_apk"
    
    if [ -f "$aligned_apk" ]; then
        log "SUCCESS" "APK aligned successfully: $aligned_apk"
        # Replace signed APK with aligned version
        mv "$aligned_apk" "$signed_apk"
    else
        log "ERROR" "APK alignment failed"
        exit 1
    fi
}

# Generate APK metadata
generate_metadata() {
    local signed_apk="$DEPLOY_DIR/signed/voicebridge-${BUILD_TYPE}-signed.apk"
    local metadata_file="$DEPLOY_DIR/metadata/apk-info.json"
    
    log "INFO" "Generating APK metadata..."
    
    # Get APK info using aapt if available
    local aapt_path=""
    if [ -n "$ANDROID_HOME" ]; then
        aapt_path="$ANDROID_HOME/build-tools/*/aapt"
        aapt_path=$(ls $aapt_path 2>/dev/null | head -n1)
    fi
    
    local apk_size=$(ls -lh "$signed_apk" | awk '{print $5}')
    local apk_md5=$(md5sum "$signed_apk" | cut -d' ' -f1 2>/dev/null || echo "unavailable")
    local apk_sha256=$(sha256sum "$signed_apk" | cut -d' ' -f1 2>/dev/null || echo "unavailable")
    
    # Create metadata JSON
    cat > "$metadata_file" << EOF
{
    "app_name": "VoiceBridge",
    "version": "1.0.0",
    "build_type": "$BUILD_TYPE",
    "build_date": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "apk_file": "$(basename "$signed_apk")",
    "apk_size": "$apk_size",
    "md5": "$apk_md5",
    "sha256": "$apk_sha256",
    "min_sdk": 24,
    "target_sdk": 34,
    "permissions": [
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.BIND_ACCESSIBILITY_SERVICE"
    ]
}
EOF
    
    log "SUCCESS" "Metadata generated: $metadata_file"
}

# Deploy locally
deploy_local() {
    log "INFO" "Deploying locally..."
    
    # Create local deployment package
    local deploy_package="$DEPLOY_DIR/voicebridge-${BUILD_TYPE}-$(date +%Y%m%d-%H%M%S).zip"
    
    cd "$DEPLOY_DIR"
    zip -r "$deploy_package" signed/ metadata/
    
    log "SUCCESS" "Local deployment package created: $deploy_package"
}

# Deploy to device
deploy_device() {
    local signed_apk="$DEPLOY_DIR/signed/voicebridge-${BUILD_TYPE}-signed.apk"
    
    log "INFO" "Deploying to connected device..."
    
    # Check if adb is available
    if ! command -v adb >/dev/null 2>&1; then
        log "ERROR" "adb not found. Please install Android SDK platform-tools"
        exit 1
    fi
    
    # Check if device is connected
    if ! adb devices | grep -q "device$"; then
        log "ERROR" "No Android device connected"
        log "INFO" "Please connect a device and enable USB debugging"
        exit 1
    fi
    
    # Install APK
    log "INFO" "Installing APK on device..."
    if adb install -r "$signed_apk"; then
        log "SUCCESS" "APK installed successfully on device"
    else
        log "ERROR" "Failed to install APK on device"
        exit 1
    fi
}

# Deploy to Play Store (placeholder)
deploy_playstore() {
    log "INFO" "Preparing Play Store deployment..."
    
    # This is a placeholder for Play Store deployment
    # In practice, you would use Google Play Console or fastlane
    
    local signed_apk="$DEPLOY_DIR/signed/voicebridge-${BUILD_TYPE}-signed.apk"
    local play_store_dir="$DEPLOY_DIR/playstore"
    
    mkdir -p "$play_store_dir"
    
    # Copy APK to Play Store directory
    cp "$signed_apk" "$play_store_dir/"
    
    # Create Play Store metadata
    cat > "$play_store_dir/release-notes.txt" << EOF
VoiceBridge Release Notes

New Features:
- Offline speech recognition with Whisper.cpp
- Intelligent form filling with LLaMA.cpp
- OCR document scanning with Tesseract
- Accessibility service integration
- YAML-based skill configuration

Bug Fixes:
- Improved audio processing stability
- Enhanced error handling
- Better memory management

Technical Details:
- Build Type: $BUILD_TYPE
- Build Date: $(date)
- Minimum Android Version: 7.0 (API 24)
- Target Android Version: 14 (API 34)
EOF
    
    log "SUCCESS" "Play Store deployment prepared: $play_store_dir"
    log "INFO" "Manual upload to Google Play Console required"
}

# Show usage
show_usage() {
    echo "VoiceBridge Deployment Script"
    echo ""
    echo "Usage: $0 [build_type] [deploy_target]"
    echo ""
    echo "Arguments:"
    echo "  build_type      'debug' or 'release' (default: release)"
    echo "  deploy_target   'local', 'device', or 'playstore' (default: local)"
    echo ""
    echo "Examples:"
    echo "  $0                      # Sign and deploy release locally"
    echo "  $0 debug device         # Deploy debug to connected device"
    echo "  $0 release playstore    # Prepare release for Play Store"
    echo ""
    echo "Environment Variables:"
    echo "  VOICEBRIDGE_KEY_ALIAS         Keystore alias (default: voicebridge)"
    echo "  VOICEBRIDGE_KEY_PASSWORD      Key password (default: voicebridge123)"
    echo "  VOICEBRIDGE_STORE_PASSWORD    Store password (default: voicebridge123)"
}

# Main deployment function
main() {
    # Handle help
    if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
        show_usage
        exit 0
    fi
    
    log "INFO" "Starting VoiceBridge deployment process..."
    log "INFO" "Build type: $BUILD_TYPE"
    log "INFO" "Deploy target: $DEPLOY_TARGET"
    
    # Step 1: Setup
    create_deploy_dir
    check_apk
    
    # Step 2: Generate keystore for release builds
    if [ "$BUILD_TYPE" = "release" ]; then
        generate_keystore
    fi
    
    # Step 3: Sign APK
    sign_apk
    
    # Step 4: Align APK
    align_apk
    
    # Step 5: Generate metadata
    generate_metadata
    
    # Step 6: Deploy based on target
    case "$DEPLOY_TARGET" in
        "local")
            deploy_local
            ;;
        "device")
            deploy_device
            ;;
        "playstore")
            deploy_playstore
            ;;
        *)
            log "ERROR" "Invalid deploy target: $DEPLOY_TARGET"
            log "ERROR" "Valid targets: local, device, playstore"
            exit 1
            ;;
    esac
    
    log "SUCCESS" "VoiceBridge deployment completed successfully!"
    log "INFO" "Deployment artifacts available in: $DEPLOY_DIR"
}

# Run main function
main "$@"