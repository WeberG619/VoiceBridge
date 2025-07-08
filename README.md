# VoiceBridge 🎙️

[![Release](https://img.shields.io/badge/release-v1.0.0--beta.1-blue.svg)](https://github.com/WeberG619/VoiceBridge/releases)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)](LICENSE)
[![Android](https://img.shields.io/badge/platform-Android%207.0%2B-brightgreen.svg)](https://developer.android.com/about/versions/nougat)
[![Privacy](https://img.shields.io/badge/privacy-100%25%20offline-success.svg)](docs/permissions-and-privacy.html)

**Privacy-first offline Android app for voice-driven form automation and document processing**

VoiceBridge combines advanced AI models (Whisper.cpp, Tesseract OCR) to create a powerful offline assistant that processes documents and fills forms using voice commands. Designed for accessibility, privacy, and complete offline operation.

🌟 **Perfect for**: Accessibility users, privacy-conscious individuals, NGOs, and anyone who needs efficient form filling assistance.

## 🚀 Key Features

### 🎤 **Voice-Driven Form Filling**
- **Offline Speech Recognition** using Whisper.cpp (12 languages supported)
- **Natural Language Processing** for intelligent form field mapping
- **Voice Commands** in multiple languages with customizable triggers

### 📄 **Document Processing**
- **OCR Text Extraction** using Tesseract (12+ languages)
- **Camera Integration** with real-time document scanning
- **Automatic Form Detection** and field identification

### ♿ **Accessibility Excellence**
- **WCAG 2.1 AA Compliant** design and interaction patterns
- **Screen Reader Support** with comprehensive audio feedback
- **Customizable UI** with high contrast, large text, and navigation options
- **Haptic & Audio Feedback** for enhanced user experience

### 🔒 **Privacy & Security**
- **100% Offline Operation** - No data transmission to external servers
- **Local AI Processing** - All ML inference happens on your device
- **Encrypted Local Storage** using Android Keystore
- **Transparent Permissions** with detailed privacy documentation

### 🛠️ **Advanced Skill System**
- **YAML-Based Templates** for different form types (Job applications, Medical intake, Tax forms, Government benefits)
- **Multi-Language Skills** with localized prompts and validation
- **Custom Skill Creation** with built-in validation tools
- **Community Contributions** with skill template sharing

## 📱 System Requirements

### Minimum Requirements
- **Android 7.0+** (API 24+)
- **3GB RAM** (4GB+ recommended)
- **ARM64-v8a** or **ARMv7** architecture
- **2GB free storage** for AI models and app data

### Recommended for Optimal Performance
- **Android 10+** with 6GB+ RAM
- **ARM64-v8a** architecture
- **Hardware-accelerated graphics** (GPU)
- **Good quality microphone** for voice recognition

### Permissions Required
- 🎤 **Microphone** - Voice command processing (required)
- ♿ **Accessibility Service** - Form automation (required)
- 📷 **Camera** - Document scanning (optional)
- 📁 **Storage** - Document access (optional)

[📋 View detailed permissions explanation](docs/permissions-and-privacy.html)

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

## 🚀 Development Status

**Current Version**: `v1.0.0-beta.1` - **Production Ready** 🎉

### ✅ **Phase 1 & 2: Core Development** (100% Complete)
- ✅ **Complete Android Application** with Material Design 3 UI
- ✅ **Native AI Integration** (Whisper.cpp + Tesseract OCR)
- ✅ **Advanced Accessibility System** (WCAG 2.1 AA compliant)
- ✅ **Comprehensive Build Pipeline** with automated testing
- ✅ **Multi-language Support** (12 languages)
- ✅ **Security & Privacy Implementation** 

### ✅ **Phase 3: Production Readiness** (95% Complete)
- ✅ **Documentation Website** deployed and ready
- ✅ **Privacy-Preserving Analytics** with user control
- ✅ **Offline Crash Reporting** system
- ✅ **Advanced Skill Templates** (Job apps, Medical, Tax forms)
- ✅ **Legal Compliance** (EULA, Privacy Policy, Permissions)
- ✅ **Quality Assurance Tools** (Skill validator, CI/CD)

### 🎯 **Ready for Market Deployment**
- ✅ **Google Play Store Assets** complete
- ✅ **Beta Testing Infrastructure** ready
- ✅ **Community Contribution System** established
- 🚧 **Promotional Materials** (video in progress)

[📊 View detailed implementation status](IMPLEMENTATION.md)

## 🤝 Contributing

We welcome contributions from developers, translators, accessibility experts, and skill template creators!

### 🔧 **Code Contributions**
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes following our coding standards
4. Add tests and run the validation suite
5. Submit a pull request with detailed description

### 🌍 **Translation & Localization**
- Add new language support for UI strings
- Create localized skill templates for your region
- Improve existing translations for accuracy

### ♿ **Accessibility Improvements**
- Test with screen readers and accessibility tools
- Suggest WCAG compliance improvements
- Document accessibility best practices

### 📋 **Skill Template Creation**
- Create skill templates for new form types
- Validate your skills using our CLI tool: `./tools/validate-skills.sh your-skill.yaml`
- Submit templates for common government, healthcare, or employment forms

### 🔍 **Quality Assurance**
- Test on different Android devices and API levels
- Report bugs with detailed reproduction steps
- Validate privacy and security features

[🚀 See CONTRIBUTING.md for detailed guidelines](CONTRIBUTING.md)

## 📄 License & Legal

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

### 📋 **Important Legal Documents**
- [📄 End User License Agreement (EULA)](docs/eula.html)
- [🔒 Privacy Policy & Permissions](docs/permissions-and-privacy.html)
- [⚖️ Third-Party Licenses](android/app/src/main/assets/licenses/)

## 🙏 Acknowledgments & Credits

### 🤖 **AI Models & Libraries**
- [**Whisper.cpp**](https://github.com/ggerganov/whisper.cpp) - OpenAI Whisper in C++ (MIT License)
- [**Tesseract OCR**](https://github.com/tesseract-ocr/tesseract) - OCR Engine (Apache 2.0)
- [**Android Jetpack**](https://developer.android.com/jetpack) - Modern Android development

### 🏗️ **Development Tools**
- [**Android NDK**](https://developer.android.com/ndk) - Native development kit
- [**Material Design 3**](https://material.io/design) - UI/UX framework
- [**GitHub Actions**](https://github.com/features/actions) - CI/CD automation

### 🌍 **Community**
- **Accessibility testers** and feedback providers
- **Translation contributors** for multi-language support
- **Beta testers** helping refine the user experience

## 📞 Support & Community

### 🆘 **Get Help**
- [📖 Documentation Website](docs/index.html)
- [🐛 Report Issues](https://github.com/WeberG619/VoiceBridge/issues)
- [💬 Discussions](https://github.com/WeberG619/VoiceBridge/discussions)

### 📧 **Contact**
- **General**: support@voicebridge.app
- **Privacy/Legal**: privacy@voicebridge.app
- **Accessibility**: accessibility@voicebridge.app
- **Development**: dev@voicebridge.app

### 🌟 **Stay Updated**
- [📱 Google Play Store](https://play.google.com/store/apps/details?id=com.voicebridge) *(coming soon)*
- [🐦 Twitter](https://twitter.com/voicebridge) *(development updates)*
- [📧 Newsletter](https://voicebridge.app/newsletter) *(major releases)*

---

## 📄 License & Copyright

**VoiceBridge** is proprietary software owned by **Weber G.**

- ✅ **Personal use permitted** under the VoiceBridge License
- ❌ **Commercial use prohibited** without explicit licensing agreement
- 📧 **Commercial licensing**: weber.voicebridge@gmail.com

See the [LICENSE](LICENSE) file for complete terms and conditions.

### Copyright Notice
```
VoiceBridge - Voice-Driven Form Automation
Copyright (c) 2025 Weber G. All rights reserved.
```

---

**VoiceBridge** - Making digital forms accessible through voice 🎙️✨