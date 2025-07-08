#!/bin/bash

# VoiceBridge Store Asset Generation Script
# Generates placeholder assets for Google Play Store

set -e

echo "Generating VoiceBridge store assets..."

# Create directories
mkdir -p graphics/icon
mkdir -p graphics/feature-graphic
mkdir -p graphics/screenshots/phone
mkdir -p graphics/screenshots/tablet
mkdir -p graphics/promo

# Function to create placeholder image with ImageMagick
create_placeholder() {
    local size=$1
    local output=$2
    local text=$3
    local bg_color=$4
    local text_color=$5
    
    # Check if ImageMagick is installed
    if ! command -v convert &> /dev/null; then
        echo "Warning: ImageMagick not installed. Creating empty file: $output"
        touch "$output"
        return
    fi
    
    convert -size "$size" "xc:$bg_color" \
        -gravity center \
        -pointsize 48 \
        -fill "$text_color" \
        -annotate +0+0 "$text" \
        "$output"
    
    echo "Created: $output"
}

# App Icon (512x512)
create_placeholder "512x512" "graphics/icon/icon-512.png" \
    "VB" "#2196F3" "white"

# Feature Graphic (1024x500)
create_placeholder "1024x500" "graphics/feature-graphic/feature-graphic.png" \
    "VoiceBridge\nVoice Form Filler" "#2196F3" "white"

# Phone Screenshots (1080x1920)
screenshot_titles=(
    "Voice Input"
    "Document Scan"
    "Form Filling"
    "Accessibility"
    "Dark Mode"
    "Skills"
)

for i in "${!screenshot_titles[@]}"; do
    create_placeholder "1080x1920" "graphics/screenshots/phone/screenshot-$((i+1)).png" \
        "${screenshot_titles[$i]}" "#FFFFFF" "#2196F3"
done

# Tablet Screenshots (1920x1080)
for i in {1..4}; do
    create_placeholder "1920x1080" "graphics/screenshots/tablet/tablet-screenshot-$i.png" \
        "Tablet View $i" "#FFFFFF" "#2196F3"
done

# Promo Graphic (180x120)
create_placeholder "180x120" "graphics/promo/promo-graphic.png" \
    "VB" "#2196F3" "white"

# TV Banner (1280x720)
create_placeholder "1280x720" "graphics/promo/tv-banner.png" \
    "VoiceBridge for Android TV" "#2196F3" "white"

# Create asset descriptions
cat > graphics/README.md << 'EOF'
# VoiceBridge Store Graphics

## Generated Placeholder Assets

These are placeholder assets for Google Play Store submission.
Replace with actual screenshots and graphics before publishing.

### Icon Requirements
- icon-512.png: 512x512px, 32-bit PNG
- No alpha channel for Play Store

### Screenshot Requirements
- Phone: 1080x1920 (or similar 9:16 ratio)
- Tablet: 1920x1080 (or similar 16:9 ratio)
- Min 2, max 8 screenshots per device type

### Feature Graphic
- 1024x500px
- Displayed at top of store listing
- Should showcase app purpose

### Promotional Assets
- Promo graphic: 180x120px (optional)
- TV banner: 1280x720px (for Android TV)

## Design Guidelines

### Brand Colors
- Primary: #2196F3 (Blue)
- Secondary: #03DAC6 (Teal)
- Accent: #FF9800 (Orange)
- Error: #B00020 (Red)
- Success: #4CAF50 (Green)

### Typography
- Headlines: Sans-serif, bold
- Body: Sans-serif, regular
- Maintain high contrast for accessibility

### Content Guidelines
1. Show actual app UI, not mockups
2. Highlight key features clearly
3. Include accessibility features
4. Show diverse use cases
5. Avoid excessive text
6. Ensure readability at small sizes

## Screenshot Suggestions

1. **Voice Input Screen**
   - Show microphone button active
   - Display transcribed text
   - Include volume indicator

2. **Document Scanning**
   - Camera viewfinder
   - OCR result overlay
   - Success confirmation

3. **Form Filling Demo**
   - Show form being filled
   - Highlight automated fields
   - Success message

4. **Accessibility Features**
   - High contrast mode
   - Large text display
   - TalkBack indicators

5. **Dark Mode**
   - Full app in dark theme
   - Comfortable night viewing

6. **Skills Library**
   - List of available skills
   - Skill configuration screen

## Tools for Creating Final Assets

- **Screenshots**: Android Studio emulator, real devices
- **Graphics**: Figma, Sketch, Adobe XD
- **Icon**: Android Asset Studio
- **Optimization**: TinyPNG, ImageOptim

Remember to test all assets on different screen sizes!
EOF

echo "Store asset generation complete!"
echo "Note: These are placeholder assets. Replace with actual screenshots before publishing."
echo "See graphics/README.md for design guidelines."