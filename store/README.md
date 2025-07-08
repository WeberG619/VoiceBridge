# VoiceBridge Google Play Store Assets

This directory contains all assets required for Google Play Store submission.

## Directory Structure

```
store/
├── README.md                  # This file
├── listings/                  # Store listing content
│   ├── en-US/                # English (US) listing
│   │   ├── title.txt         # App title (30 chars max)
│   │   ├── short-description.txt  # Short description (80 chars max)
│   │   ├── full-description.txt   # Full description (4000 chars max)
│   │   └── changelog.txt     # Release notes
│   └── [other-languages]/    # Additional language listings
├── graphics/                  # Visual assets
│   ├── icon/                 # App icons
│   │   ├── icon-512.png      # Hi-res icon (512x512)
│   │   └── adaptive-icon/    # Adaptive icon assets
│   ├── feature-graphic/      # Feature graphic
│   │   └── feature-graphic.png  # (1024x500)
│   ├── screenshots/          # App screenshots
│   │   ├── phone/           # Phone screenshots
│   │   └── tablet/          # Tablet screenshots (optional)
│   └── promo/               # Promotional assets
│       ├── promo-graphic.png # Promo graphic (180x120)
│       └── tv-banner.png     # TV banner (1280x720)
├── metadata/                 # App metadata
│   ├── contact-details.txt   # Developer contact info
│   ├── privacy-policy-url.txt # Privacy policy URL
│   ├── category.txt          # App category
│   └── tags.txt             # App tags/keywords
└── release/                  # Release configuration
    ├── release-notes.txt     # Current version release notes
    └── app-signing.md        # App signing instructions
```

## Asset Requirements

### App Icons
- **Hi-res icon**: 512x512 PNG, 32-bit color
- **Adaptive icon**: Foreground and background layers
- No alpha channel for launcher icon

### Feature Graphic
- **Size**: 1024x500 pixels
- Displayed at top of store listing
- No embedded text (localization support)

### Screenshots
- **Phone**: Min 2, max 8 screenshots
- **Size**: Min 320px, max 3840px on longest side
- **Aspect ratio**: 16:9 or 9:16 recommended
- Showcase key features

### Promotional Assets
- **Promo graphic**: 180x120 (optional)
- **TV banner**: 1280x720 (optional)

## Compliance Requirements

### Privacy Policy
- Required for apps with accessibility services
- Must explain data collection and usage
- Hosted on public URL

### Accessibility
- Detailed explanation of accessibility service usage
- WCAG 2.1 AA compliance statement
- Clear permission justifications

### Content Rating
- Complete content rating questionnaire
- VoiceBridge target rating: Everyone

## Release Process

1. **Prepare APK/AAB**
   ```bash
   ./scripts/deploy.sh --release
   ```

2. **Sign Release Build**
   - Use upload key for Play App Signing
   - Keep keystore secure

3. **Upload to Play Console**
   - Create new release in Production track
   - Upload signed AAB file
   - Fill in release notes

4. **Complete Store Listing**
   - Upload all graphics
   - Add descriptions in supported languages
   - Set pricing (Free)
   - Select countries for distribution

5. **Submit for Review**
   - Review time: 2-3 hours typically
   - Address any policy violations

## Marketing Guidelines

### Key Features to Highlight
1. Offline functionality - no internet required
2. Privacy-focused - all processing on device
3. Accessibility-first design
4. Voice-controlled form filling
5. Document scanning with OCR
6. Multi-language support
7. Free with no ads or IAP

### Target Audience
- Users with disabilities
- Elderly users
- Non-tech savvy users
- Privacy-conscious users
- Professionals needing form automation

## Support Information

### Required URLs
- Privacy Policy: [To be added]
- Terms of Service: [To be added]
- Support Website: [To be added]
- Support Email: support@voicebridge.app

### Developer Information
- Developer Name: VoiceBridge Team
- Developer Website: [To be added]
- Physical Address: [Required for payments]