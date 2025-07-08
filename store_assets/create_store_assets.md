# Creating Store Assets

## 📸 Screenshots (Required for all stores)

### Take Screenshots
1. **Install app** on device or emulator
2. **Open app** and navigate through key features
3. **Take screenshots** of:
   - Main interface
   - Voice recording screen
   - Form detection/filling
   - Settings page
   - Permissions/onboarding

### Screenshot Specifications
- **Samsung Galaxy Store**: 1080x1920 (portrait) or 1920x1080 (landscape)
- **Amazon Appstore**: 1080x1920 recommended
- **APKPure**: 1080x1920 or 1920x1080

### Using Android Studio
```
1. Start emulator
2. Install app: adb install app-release.apk
3. Use built-in screenshot tool
4. Save to store_assets/screenshots/
```

## 🎨 App Icon (512x512)

Your current app already has icons in the mipmap folders. To create the required 512x512 version:

### Extract Current Icon
```cmd
# Navigate to your project
cd D:\013-VoiceBridge\android\app\src\main\res

# Your largest icon is in mipmap-xxxhdpi (192x192)
# You'll need to upscale or recreate at 512x512
```

### Options:
1. **Use existing 192x192** and upscale with online tool
2. **Recreate** using the same design at 512x512
3. **Use icon generator** like Android Asset Studio

## 🖼️ Feature Graphic (1024x500)

Create a banner showcasing your app:

### Design Elements
- App name: "VoiceBridge"
- Tagline: "Voice-Driven Form Automation"
- Key features: "Offline • Private • Accessible"
- App icon
- Screenshot preview

### Tools
- **Canva** (free templates)
- **GIMP** (free)
- **Online banner makers**

## 🚀 Quick Asset Generation

### Minimum Viable Assets
1. **Screenshots**: 3-4 key screens
2. **App Icon**: Upscale existing 192x192 to 512x512
3. **Feature Graphic**: Simple text-based design

### Professional Assets (Later)
- Custom designed feature graphic
- Video preview/trailer
- Localized screenshots for each language
- App preview videos

## 📁 File Organization

```
store_assets/
├── app_description.md
├── store_upload_checklist.md
├── icons/
│   └── icon_512x512.png
├── screenshots/
│   ├── main_screen.png
│   ├── voice_recording.png
│   ├── form_detection.png
│   └── settings.png
└── feature_graphics/
    ├── samsung_1024x500.png
    ├── amazon_1024x500.png
    └── apkpure_1024x500.png
```

## ⚡ Quick Start Commands

```cmd
# Build release APK
cd D:\013-VoiceBridge\android
build_signed_bundle.bat

# Install on device for screenshots
adb install app\build\outputs\apk\release\app-release.apk

# Create assets folder
mkdir store_assets\screenshots
mkdir store_assets\icons
mkdir store_assets\feature_graphics
```

Once you have these basic assets, you'll be ready to upload to Samsung Galaxy Store and other platforms!