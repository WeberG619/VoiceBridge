#!/bin/bash

# VoiceBridge Complete Build Script
# Builds the entire VoiceBridge Android application

set -e

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$PROJECT_ROOT/android"
SCRIPTS_DIR="$PROJECT_ROOT/scripts"
BUILD_TYPE="${1:-debug}"
CLEAN_BUILD="${2:-false}"

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

# Check if running in project root
check_project_root() {
    if [ ! -f "$PROJECT_ROOT/IMPLEMENTATION.md" ] || [ ! -d "$PROJECT_ROOT/android" ]; then
        log "ERROR" "Must be run from VoiceBridge project root directory"
        exit 1
    fi
}

# Check prerequisites
check_prerequisites() {
    log "INFO" "Checking build prerequisites..."
    
    # Check Java
    if ! command -v java >/dev/null 2>&1; then
        log "ERROR" "Java not found. Please install Java JDK 17 or later"
        exit 1
    fi
    
    local java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 17 ]; then
        log "ERROR" "Java 17 or later required. Found version: $java_version"
        exit 1
    fi
    
    # Check Android SDK
    if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
        log "ERROR" "ANDROID_HOME or ANDROID_SDK_ROOT not set"
        log "ERROR" "Please set up Android SDK environment variables"
        exit 1
    fi
    
    # Check NDK
    local ndk_path=""
    if [ -n "$ANDROID_NDK_HOME" ]; then
        ndk_path="$ANDROID_NDK_HOME"
    elif [ -n "$ANDROID_HOME" ]; then
        ndk_path="$ANDROID_HOME/ndk-bundle"
        if [ ! -d "$ndk_path" ]; then
            ndk_path="$ANDROID_HOME/ndk/25.2.9519653"  # Common NDK version
        fi
    fi
    
    if [ ! -d "$ndk_path" ]; then
        log "ERROR" "Android NDK not found. Please install Android NDK 25+"
        exit 1
    fi
    
    # Check CMake
    if ! command -v cmake >/dev/null 2>&1; then
        log "ERROR" "CMake not found. Please install CMake 3.22+"
        exit 1
    fi
    
    local cmake_version=$(cmake --version | head -n1 | cut -d' ' -f3)
    local cmake_major=$(echo "$cmake_version" | cut -d'.' -f1)
    local cmake_minor=$(echo "$cmake_version" | cut -d'.' -f2)
    
    if [ "$cmake_major" -lt 3 ] || ([ "$cmake_major" -eq 3 ] && [ "$cmake_minor" -lt 22 ]); then
        log "ERROR" "CMake 3.22+ required. Found version: $cmake_version"
        exit 1
    fi
    
    log "SUCCESS" "All prerequisites satisfied"
}

# Setup git submodules
setup_submodules() {
    log "INFO" "Setting up git submodules..."
    
    if [ -x "$SCRIPTS_DIR/setup_submodules.sh" ]; then
        cd "$PROJECT_ROOT"
        "$SCRIPTS_DIR/setup_submodules.sh"
    else
        log "ERROR" "setup_submodules.sh not found or not executable"
        exit 1
    fi
}

# Download models
download_models() {
    log "INFO" "Downloading AI models..."
    
    if [ -x "$SCRIPTS_DIR/build_models.sh" ]; then
        cd "$PROJECT_ROOT"
        "$SCRIPTS_DIR/build_models.sh"
    else
        log "ERROR" "build_models.sh not found or not executable"
        exit 1
    fi
}

# Build native libraries
build_native() {
    log "INFO" "Building native libraries..."
    
    cd "$ANDROID_DIR"
    
    # Check if external libraries exist
    if [ ! -d "$PROJECT_ROOT/extern/whisper.cpp" ] || [ ! -d "$PROJECT_ROOT/extern/llama.cpp" ]; then
        log "ERROR" "Missing external libraries. Run setup_submodules.sh first"
        exit 1
    fi
    
    # Build with gradlew
    local gradle_cmd="./gradlew"
    if [ "$CLEAN_BUILD" = "true" ]; then
        log "INFO" "Performing clean build..."
        $gradle_cmd clean
    fi
    
    # Build native libraries specifically
    $gradle_cmd app:buildCMakeDebug
    
    log "SUCCESS" "Native libraries built successfully"
}

# Build Android APK
build_apk() {
    log "INFO" "Building Android APK..."
    
    cd "$ANDROID_DIR"
    
    local gradle_cmd="./gradlew"
    local build_task=""
    
    case "$BUILD_TYPE" in
        "debug")
            build_task="assembleDebug"
            ;;
        "release")
            build_task="assembleRelease"
            ;;
        *)
            log "ERROR" "Invalid build type: $BUILD_TYPE (use 'debug' or 'release')"
            exit 1
            ;;
    esac
    
    log "INFO" "Building $BUILD_TYPE APK..."
    $gradle_cmd $build_task
    
    # Find and report APK location
    local apk_path=""
    if [ "$BUILD_TYPE" = "debug" ]; then
        apk_path="app/build/outputs/apk/debug/app-debug.apk"
    else
        apk_path="app/build/outputs/apk/release/app-release.apk"
    fi
    
    if [ -f "$apk_path" ]; then
        local apk_size=$(ls -lh "$apk_path" | awk '{print $5}')
        log "SUCCESS" "APK built successfully: $apk_path ($apk_size)"
        
        # Copy to project root for easy access
        cp "$apk_path" "$PROJECT_ROOT/voicebridge-${BUILD_TYPE}.apk"
        log "INFO" "APK copied to project root: voicebridge-${BUILD_TYPE}.apk"
    else
        log "ERROR" "APK not found at expected location: $apk_path"
        exit 1
    fi
}

# Run tests
run_tests() {
    log "INFO" "Running tests..."
    
    cd "$ANDROID_DIR"
    
    # Run unit tests
    ./gradlew test
    
    # Run instrumentation tests if available
    if ./gradlew tasks | grep -q "connectedAndroidTest"; then
        log "INFO" "Running instrumentation tests..."
        ./gradlew connectedAndroidTest
    else
        log "INFO" "No instrumentation tests configured"
    fi
    
    log "SUCCESS" "All tests passed"
}

# Lint check
run_lint() {
    log "INFO" "Running lint checks..."
    
    cd "$ANDROID_DIR"
    ./gradlew lint
    
    log "SUCCESS" "Lint checks completed"
}

# Generate build report
generate_report() {
    log "INFO" "Generating build report..."
    
    local report_file="$PROJECT_ROOT/build-report.txt"
    
    cat > "$report_file" << EOF
VoiceBridge Build Report
=====================

Build Date: $(date)
Build Type: $BUILD_TYPE
Project Root: $PROJECT_ROOT

Prerequisites:
- Java Version: $(java -version 2>&1 | head -n1 | cut -d'"' -f2)
- CMake Version: $(cmake --version | head -n1 | cut -d' ' -f3)
- Android SDK: ${ANDROID_HOME:-${ANDROID_SDK_ROOT:-"Not set"}}
- NDK Path: ${ANDROID_NDK_HOME:-"Default"}

Build Artifacts:
EOF
    
    # List APK files
    for apk in "$PROJECT_ROOT"/*.apk; do
        if [ -f "$apk" ]; then
            local apk_size=$(ls -lh "$apk" | awk '{print $5}')
            echo "- $(basename "$apk") ($apk_size)" >> "$report_file"
        fi
    done
    
    # List model files
    echo "" >> "$report_file"
    echo "Models:" >> "$report_file"
    local models_dir="$ANDROID_DIR/app/src/main/assets"
    if [ -d "$models_dir" ]; then
        for model in "$models_dir"/*.bin "$models_dir"/*.gguf; do
            if [ -f "$model" ]; then
                local model_size=$(ls -lh "$model" | awk '{print $5}')
                echo "- $(basename "$model") ($model_size)" >> "$report_file"
            fi
        done
    fi
    
    log "SUCCESS" "Build report generated: $report_file"
}

# Show usage
show_usage() {
    echo "VoiceBridge Build Script"
    echo ""
    echo "Usage: $0 [build_type] [clean]"
    echo ""
    echo "Arguments:"
    echo "  build_type    'debug' or 'release' (default: debug)"
    echo "  clean         'true' to perform clean build (default: false)"
    echo ""
    echo "Examples:"
    echo "  $0                    # Debug build"
    echo "  $0 release           # Release build"
    echo "  $0 debug true        # Clean debug build"
    echo "  $0 release true      # Clean release build"
    echo ""
    echo "Environment Variables:"
    echo "  ANDROID_HOME         Path to Android SDK"
    echo "  ANDROID_NDK_HOME     Path to Android NDK"
    echo "  SKIP_MODELS          Skip model download (default: false)"
    echo "  SKIP_TESTS           Skip test execution (default: false)"
}

# Main build function
main() {
    # Handle help
    if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
        show_usage
        exit 0
    fi
    
    log "INFO" "Starting VoiceBridge build process..."
    log "INFO" "Build type: $BUILD_TYPE"
    log "INFO" "Clean build: $CLEAN_BUILD"
    
    # Step 1: Check prerequisites
    check_project_root
    check_prerequisites
    
    # Step 2: Setup submodules
    setup_submodules
    
    # Step 3: Download models (unless skipped)
    if [ "${SKIP_MODELS:-false}" != "true" ]; then
        download_models
    else
        log "INFO" "Skipping model download (SKIP_MODELS=true)"
    fi
    
    # Step 4: Build native libraries
    build_native
    
    # Step 5: Build APK
    build_apk
    
    # Step 6: Run tests (unless skipped)
    if [ "${SKIP_TESTS:-false}" != "true" ]; then
        run_tests
    else
        log "INFO" "Skipping tests (SKIP_TESTS=true)"
    fi
    
    # Step 7: Run lint
    run_lint
    
    # Step 8: Generate report
    generate_report
    
    log "SUCCESS" "VoiceBridge build completed successfully!"
    log "INFO" "Build artifacts available in project root"
}

# Run main function
main "$@"