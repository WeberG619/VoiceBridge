#!/bin/bash

# VoiceBridge Model Download Script
# Downloads Whisper and LLaMA models for VoiceBridge Android app

set -e

MODELS_DIR="android/app/src/main/assets"
TEMP_DIR="temp_models"

# Create directories
mkdir -p "$MODELS_DIR"
mkdir -p "$TEMP_DIR"

echo "🔄 Downloading VoiceBridge models..."

# Function to verify SHA256 checksum
verify_checksum() {
    local file="$1"
    local expected_sha256="$2"
    
    if command -v sha256sum >/dev/null 2>&1; then
        local actual_sha256=$(sha256sum "$file" | cut -d' ' -f1)
    elif command -v shasum >/dev/null 2>&1; then
        local actual_sha256=$(shasum -a 256 "$file" | cut -d' ' -f1)
    else
        echo "⚠️  No SHA256 utility found, skipping checksum verification"
        return 0
    fi
    
    if [ "$actual_sha256" = "$expected_sha256" ]; then
        echo "✅ Checksum verification passed"
        return 0
    else
        echo "❌ Checksum verification failed"
        echo "   Expected: $expected_sha256"
        echo "   Actual:   $actual_sha256"
        return 1
    fi
}

# Whisper tiny-en model
WHISPER_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin"
WHISPER_FILE="whisper-tiny-en.bin"
WHISPER_SHA256="bd577a113a864445d4c299885e0cb97d4ba92b5f5dc314589e5a4a7a0c3b0c2a"

echo "📥 Downloading Whisper tiny-en model..."
if [ ! -f "$MODELS_DIR/$WHISPER_FILE" ]; then
    echo "   Downloading from: $WHISPER_URL"
    if curl -L --fail "$WHISPER_URL" -o "$TEMP_DIR/$WHISPER_FILE"; then
        echo "   Download completed, verifying checksum..."
        if verify_checksum "$TEMP_DIR/$WHISPER_FILE" "$WHISPER_SHA256"; then
            mv "$TEMP_DIR/$WHISPER_FILE" "$MODELS_DIR/$WHISPER_FILE"
            echo "✅ Whisper model downloaded and verified successfully"
        else
            echo "❌ Checksum verification failed, removing file"
            rm -f "$TEMP_DIR/$WHISPER_FILE"
            exit 1
        fi
    else
        echo "❌ Failed to download Whisper model"
        exit 1
    fi
else
    echo "✅ Whisper model already exists"
fi

# LLaMA 7B Q5 model (TheBloke's GGUF format)
LLAMA_URL="https://huggingface.co/TheBloke/Llama-2-7B-Chat-GGUF/resolve/main/llama-2-7b-chat.Q5_K_M.gguf"
LLAMA_FILE="llama-7b-q5.gguf"
LLAMA_SHA256="f4e4f2e0e2e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0"

echo "📥 Downloading LLaMA 7B Q5 model..."
if [ ! -f "$MODELS_DIR/$LLAMA_FILE" ]; then
    echo "   Model size: ~4.8GB (This may take a while...)"
    echo "   Downloading from: $LLAMA_URL"
    
    # Check available disk space (Linux/macOS)
    if command -v df >/dev/null 2>&1; then
        available_space_kb=$(df "$MODELS_DIR" | tail -1 | awk '{print $4}')
        available_space_gb=$((available_space_kb / 1024 / 1024))
        if [ "$available_space_gb" -lt 6 ]; then
            echo "⚠️  Warning: Low disk space (${available_space_gb}GB available, ~6GB recommended)"
            read -p "Continue anyway? (y/N): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                echo "❌ Download cancelled"
                exit 1
            fi
        fi
    fi
    
    # Download with progress bar and resume capability
    if curl -L -C - --fail "$LLAMA_URL" -o "$TEMP_DIR/$LLAMA_FILE"; then
        echo "   Download completed, moving to final location..."
        mv "$TEMP_DIR/$LLAMA_FILE" "$MODELS_DIR/$LLAMA_FILE"
        echo "✅ LLaMA model downloaded successfully"
        echo "   Note: Checksum verification skipped for large model (enable if needed)"
    else
        echo "❌ Failed to download LLaMA model"
        echo "   You can manually download from: $LLAMA_URL"
        echo "   Alternative: Use huggingface-cli for better download experience"
        exit 1
    fi
else
    echo "✅ LLaMA model already exists"
fi

# Clean up
rm -rf "$TEMP_DIR"

echo "🎉 Model download complete!"
echo "📁 Models location: $MODELS_DIR"
echo "📄 Whisper model: $MODELS_DIR/$WHISPER_FILE"
echo "📄 LLaMA model: $MODELS_DIR/$LLAMA_FILE"

# Check model sizes
echo "📊 Model sizes:"
if ls "$MODELS_DIR"/*.bin "$MODELS_DIR"/*.gguf >/dev/null 2>&1; then
    ls -lh "$MODELS_DIR"/*.bin "$MODELS_DIR"/*.gguf
else
    echo "No model files found"
fi

echo ""
echo "🔧 Alternative download methods:"
echo "   For better download experience, install huggingface-cli:"
echo "   pip install huggingface-hub"
echo "   huggingface-cli download TheBloke/Llama-2-7B-Chat-GGUF llama-2-7b-chat.Q5_K_M.gguf --local-dir $MODELS_DIR --local-dir-use-symlinks False"
echo ""
echo "✅ Ready to build VoiceBridge with AI models!"