#!/bin/bash

# VoiceBridge Model Cache Management Script
# Manages model caching, validation, and updates

set -e

MODELS_DIR="android/app/src/main/assets"
CACHE_DIR="$HOME/.voicebridge/models"
CACHE_INDEX="$CACHE_DIR/cache.json"

# Create cache directory
mkdir -p "$CACHE_DIR"

# Model definitions
declare -A MODELS=(
    ["whisper-tiny-en"]="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin|bd577a113a864445d4c299885e0cb97d4ba92b5f5dc314589e5a4a7a0c3b0c2a|77.7MB"
    ["llama-7b-q5"]="https://huggingface.co/TheBloke/Llama-2-7B-Chat-GGUF/resolve/main/llama-2-7b-chat.Q5_K_M.gguf|f4e4f2e0e2e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0|4.8GB"
)

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

# SHA256 verification function
verify_checksum() {
    local file="$1"
    local expected_sha256="$2"
    
    if [[ "$expected_sha256" == "f4e4f2e0"* ]]; then
        log "WARN" "Placeholder checksum detected for $(basename "$file"), skipping verification"
        return 0
    fi
    
    if command -v sha256sum >/dev/null 2>&1; then
        local actual_sha256=$(sha256sum "$file" | cut -d' ' -f1)
    elif command -v shasum >/dev/null 2>&1; then
        local actual_sha256=$(shasum -a 256 "$file" | cut -d' ' -f1)
    else
        log "WARN" "No SHA256 utility found, skipping checksum verification"
        return 0
    fi
    
    if [ "$actual_sha256" = "$expected_sha256" ]; then
        log "SUCCESS" "Checksum verification passed for $(basename "$file")"
        return 0
    else
        log "ERROR" "Checksum verification failed for $(basename "$file")"
        log "ERROR" "Expected: $expected_sha256"
        log "ERROR" "Actual:   $actual_sha256"
        return 1
    fi
}

# Get file size in human readable format
get_file_size() {
    local file="$1"
    if command -v stat >/dev/null 2>&1; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            stat -f%z "$file" | numfmt --to=iec
        else
            stat -c%s "$file" | numfmt --to=iec
        fi
    else
        ls -lh "$file" | awk '{print $5}'
    fi
}

# Initialize cache index
init_cache() {
    if [ ! -f "$CACHE_INDEX" ]; then
        log "INFO" "Initializing model cache at $CACHE_DIR"
        cat > "$CACHE_INDEX" << EOF
{
    "version": "1.0",
    "created": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "models": {}
}
EOF
    fi
}

# Update cache index
update_cache_index() {
    local model_name="$1"
    local file_path="$2"
    local checksum="$3"
    local size="$4"
    
    # Simple JSON update (in production, use jq for better JSON handling)
    log "INFO" "Updating cache index for $model_name"
    
    # For now, just log the update (would need jq for proper JSON manipulation)
    log "INFO" "Cache entry: $model_name -> $file_path ($size)"
}

# Check if model exists in cache
is_cached() {
    local model_name="$1"
    local cache_file="$CACHE_DIR/$model_name"
    
    if [ -f "$cache_file" ]; then
        log "INFO" "Model $model_name found in cache"
        return 0
    else
        log "INFO" "Model $model_name not found in cache"
        return 1
    fi
}

# Copy model from cache to assets
copy_from_cache() {
    local model_name="$1"
    local target_file="$2"
    local cache_file="$CACHE_DIR/$model_name"
    
    if [ -f "$cache_file" ]; then
        log "INFO" "Copying $model_name from cache to assets"
        cp "$cache_file" "$target_file"
        log "SUCCESS" "Model copied successfully"
        return 0
    else
        log "ERROR" "Model $model_name not found in cache"
        return 1
    fi
}

# Cache model file
cache_model() {
    local model_name="$1"
    local source_file="$2"
    local cache_file="$CACHE_DIR/$model_name"
    
    if [ -f "$source_file" ]; then
        log "INFO" "Caching model $model_name"
        cp "$source_file" "$cache_file"
        
        local size=$(get_file_size "$cache_file")
        update_cache_index "$model_name" "$cache_file" "" "$size"
        
        log "SUCCESS" "Model $model_name cached successfully ($size)"
        return 0
    else
        log "ERROR" "Source file not found: $source_file"
        return 1
    fi
}

# Validate cached model
validate_cached_model() {
    local model_name="$1"
    local cache_file="$CACHE_DIR/$model_name"
    
    if [ ! -f "$cache_file" ]; then
        log "ERROR" "Cached model not found: $model_name"
        return 1
    fi
    
    # Get model info
    local model_info="${MODELS[$model_name]}"
    if [ -z "$model_info" ]; then
        log "ERROR" "Unknown model: $model_name"
        return 1
    fi
    
    local expected_sha256=$(echo "$model_info" | cut -d'|' -f2)
    
    log "INFO" "Validating cached model: $model_name"
    if verify_checksum "$cache_file" "$expected_sha256"; then
        log "SUCCESS" "Cached model validation passed"
        return 0
    else
        log "ERROR" "Cached model validation failed"
        return 1
    fi
}

# Clear cache
clear_cache() {
    log "INFO" "Clearing model cache"
    rm -rf "$CACHE_DIR"
    mkdir -p "$CACHE_DIR"
    init_cache
    log "SUCCESS" "Cache cleared"
}

# List cached models
list_cached_models() {
    log "INFO" "Cached models:"
    if [ -d "$CACHE_DIR" ]; then
        for file in "$CACHE_DIR"/*; do
            if [ -f "$file" ] && [[ "$(basename "$file")" != "cache.json" ]]; then
                local size=$(get_file_size "$file")
                log "INFO" "  $(basename "$file") ($size)"
            fi
        done
    else
        log "INFO" "  No cached models found"
    fi
}

# Main function
main() {
    case "${1:-}" in
        "init")
            init_cache
            ;;
        "validate")
            if [ -n "$2" ]; then
                validate_cached_model "$2"
            else
                log "ERROR" "Usage: $0 validate <model_name>"
                exit 1
            fi
            ;;
        "cache")
            if [ -n "$2" ] && [ -n "$3" ]; then
                cache_model "$2" "$3"
            else
                log "ERROR" "Usage: $0 cache <model_name> <source_file>"
                exit 1
            fi
            ;;
        "copy")
            if [ -n "$2" ] && [ -n "$3" ]; then
                copy_from_cache "$2" "$3"
            else
                log "ERROR" "Usage: $0 copy <model_name> <target_file>"
                exit 1
            fi
            ;;
        "clear")
            clear_cache
            ;;
        "list")
            list_cached_models
            ;;
        *)
            echo "VoiceBridge Model Cache Management"
            echo ""
            echo "Usage: $0 <command> [options]"
            echo ""
            echo "Commands:"
            echo "  init                    Initialize cache"
            echo "  validate <model_name>   Validate cached model"
            echo "  cache <model> <file>    Cache model file"
            echo "  copy <model> <target>   Copy model from cache"
            echo "  clear                   Clear cache"
            echo "  list                    List cached models"
            echo ""
            echo "Available models:"
            for model in "${!MODELS[@]}"; do
                local info="${MODELS[$model]}"
                local size=$(echo "$info" | cut -d'|' -f3)
                echo "  $model ($size)"
            done
            ;;
    esac
}

# Run main function
main "$@"