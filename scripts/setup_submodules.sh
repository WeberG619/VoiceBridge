#!/bin/bash

# VoiceBridge Submodule Setup Script
# Sets up git submodules for whisper.cpp and llama.cpp

set -e

echo "🔄 Setting up VoiceBridge submodules..."

# Initialize git repository if not already done
if [ ! -d ".git" ]; then
    echo "📁 Initializing git repository..."
    git init
    git add .
    git commit -m "Initial VoiceBridge commit"
fi

# Add whisper.cpp submodule
if [ ! -d "extern/whisper.cpp" ]; then
    echo "📥 Adding whisper.cpp submodule..."
    git submodule add https://github.com/ggerganov/whisper.cpp.git extern/whisper.cpp
    echo "✅ whisper.cpp submodule added"
else
    echo "✅ whisper.cpp submodule already exists"
fi

# Add llama.cpp submodule
if [ ! -d "extern/llama.cpp" ]; then
    echo "📥 Adding llama.cpp submodule..."
    git submodule add https://github.com/ggerganov/llama.cpp.git extern/llama.cpp
    echo "✅ llama.cpp submodule added"
else
    echo "✅ llama.cpp submodule already exists"
fi

# Initialize and update submodules
echo "🔄 Updating submodules..."
git submodule init
git submodule update --recursive

# Check submodule status
echo "📊 Submodule status:"
git submodule status

echo "🎉 Submodule setup complete!"
echo "📁 Whisper.cpp: extern/whisper.cpp"
echo "📁 LLaMA.cpp: extern/llama.cpp"

# Create CMake integration hints
echo "💡 CMake integration ready - check android/app/src/main/cpp/CMakeLists.txt"