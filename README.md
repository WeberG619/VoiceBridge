# VoiceBridge 🎙️

**Offline Android app for speech-driven document reading and form filling**

VoiceBridge combines Whisper.cpp (speech-to-text) and LLaMA.cpp (language processing) to create a powerful offline assistant that can read documents aloud and fill forms using voice commands.

## 🚀 Features

- **Offline Speech Recognition** - Whisper.cpp integration for privacy-focused STT
- **Intelligent Language Processing** - LLaMA.cpp for understanding and context
- **Form Automation** - YAML-based skill system for automated form filling
- **OCR Integration** - Tesseract for document text extraction
- **Accessibility Services** - Android accessibility framework integration
- **Multi-modal Input** - Voice, camera, and document scanning

## 📱 System Requirements

- **Android 7.0+** (API 24+)
- **4GB RAM** minimum
- **ARM64-v8a** architecture (primary)
- **Microphone** and **Camera** permissions
- **2GB storage** for models and app data

## 🏗️ Architecture

```
Speech Input → Whisper.cpp → Text Processing → LLaMA.cpp → Form Actions
     ↓              ↓              ↓              ↓
Camera/OCR → Tesseract → Text → Skill Engine → Accessibility Service
```

## 🛠️ Development Setup

### Prerequisites

- Android Studio 2023.1+
- Android NDK 25+
- CMake 3.22+
- Git with submodule support

### Quick Start

1. **Clone and setup**:
   ```bash
   git clone <repository-url>
   cd VoiceBridge
   ./scripts/setup_submodules.sh
   ```

2. **Download models**:
   ```bash
   ./scripts/build_models.sh
   ```

3. **Build in Android Studio**:
   - Open `android/` folder in Android Studio
   - Build and run on device/emulator

## 📂 Project Structure

```
VoiceBridge/
├── android/                    # Android Studio project
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── cpp/           # JNI bridge (C++)
│   │   │   ├── java/          # Kotlin source
│   │   │   └── assets/        # ML models
│   │   └── build.gradle
│   └── build.gradle
├── extern/                     # Git submodules
│   ├── whisper.cpp/           # Speech-to-text
│   └── llama.cpp/             # Language processing
├── skills/                     # YAML skill definitions
│   ├── forms/                 # Form automation skills
│   └── documents/             # Document processing skills
├── scripts/                    # Build automation
│   ├── build_models.sh        # Model download
│   └── setup_submodules.sh    # Git submodule setup
├── docs/                       # Documentation
├── IMPLEMENTATION.md           # Development status
└── README.md                  # This file
```

## 🔧 Core Components

### Native Libraries (C++)
- **voicebridge.cpp** - Main JNI bridge
- **audio_processor.cpp** - Audio preprocessing
- **text_processor.cpp** - Text cleaning and formatting

### Android Components (Kotlin)
- **MainActivity.kt** - Main app interface
- **VoiceBridgeNative.kt** - JNI bindings
- **VoiceBridgeAccessibilityService** - Form automation

### Skill System
- **YAML-based** skill definitions
- **Form automation** templates
- **Multi-language** support
- **Validation** and formatting

## 📋 Example Skills

### Florida SNAP Renewal
```yaml
id: florida_snap_renewal
name: Florida SNAP Renewal Form
prompts:
  - field: full_name
    ask: "What is your full legal name?"
    type: name
    required: true
  - field: phone_number
    ask: "What is your phone number?"
    type: phone
    format: "(XXX) XXX-XXXX"
```

## 🔐 Security & Privacy

- **100% Offline** - No data sent to servers
- **Local Processing** - All ML inference on-device
- **Secure Storage** - Encrypted local data storage
- **Permission-based** - Minimal required permissions

## 🚧 Development Status

**Current Phase**: Phase 0 - Foundation Setup

- ✅ Project structure created
- ✅ Android project scaffold
- ✅ Native library integration
- ✅ JNI bridge implementation
- ✅ YAML skill system
- ✅ Build scripts
- ⏳ OCR integration
- ⏳ Form automation
- ⏳ Accessibility services
- ⏳ UI/UX polish

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- [Whisper.cpp](https://github.com/ggerganov/whisper.cpp) - Speech recognition
- [LLaMA.cpp](https://github.com/ggerganov/llama.cpp) - Language processing
- [Tesseract](https://github.com/tesseract-ocr/tesseract) - OCR engine
- [Android NDK](https://developer.android.com/ndk) - Native development

## 📞 Support

For support, please open an issue in the GitHub repository.