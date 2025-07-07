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

# Whisper tiny-en model
WHISPER_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin"
WHISPER_FILE="whisper-tiny-en.bin"
WHISPER_SHA256="921e4cf5b5dbce7b5e5e5e0b1b6b8e8e8e8e8e8e8e8e8e8e8e8e8e8e8e8e8e8e8"

echo "📥 Downloading Whisper tiny-en model..."
if [ ! -f "$MODELS_DIR/$WHISPER_FILE" ]; then
    curl -L "$WHISPER_URL" -o "$TEMP_DIR/$WHISPER_FILE"
    
    # Verify download (skip checksum for now as it's a placeholder)
    if [ -f "$TEMP_DIR/$WHISPER_FILE" ]; then
        mv "$TEMP_DIR/$WHISPER_FILE" "$MODELS_DIR/$WHISPER_FILE"
        echo "✅ Whisper model downloaded successfully"
    else
        echo "❌ Failed to download Whisper model"
        exit 1
    fi
else
    echo "✅ Whisper model already exists"
fi

# LLaMA 7B Q5 model (using a smaller test model for development)
LLAMA_URL="https://huggingface.co/microsoft/DialoGPT-medium/resolve/main/pytorch_model.bin"
LLAMA_FILE="llama-7b-q5.gguf"

echo "📥 Downloading LLaMA model..."
if [ ! -f "$MODELS_DIR/$LLAMA_FILE" ]; then
    # For development, we'll create a placeholder file
    # In production, you'd download the actual model
    echo "⚠️  Creating placeholder LLaMA model (replace with actual model in production)"
    dd if=/dev/zero of="$TEMP_DIR/$LLAMA_FILE" bs=1M count=1
    mv "$TEMP_DIR/$LLAMA_FILE" "$MODELS_DIR/$LLAMA_FILE"
    echo "✅ Placeholder LLaMA model created"
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
ls -lh "$MODELS_DIR"/*.bin "$MODELS_DIR"/*.gguf 2>/dev/null || echo "No model files found"

echo "⚠️  Note: Replace placeholder models with actual production models before release"