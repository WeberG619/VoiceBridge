#!/bin/bash

# VoiceBridge Icon Generation Script
# Generates all required icon sizes from the vector drawable

set -e

echo "Generating VoiceBridge app icons..."

# Define icon sizes for different densities
declare -A ICON_SIZES=(
    ["mdpi"]=48
    ["hdpi"]=72
    ["xhdpi"]=96
    ["xxhdpi"]=144
    ["xxxhdpi"]=192
)

# Define launcher icon sizes
declare -A LAUNCHER_SIZES=(
    ["mdpi"]=48
    ["hdpi"]=72
    ["xhdpi"]=96
    ["xxhdpi"]=144
    ["xxxhdpi"]=192
)

# Output directories
RES_DIR="../../android/app/src/main/res"
STORE_DIR="../../store/graphics/icon"

# Create directories
for density in "${!ICON_SIZES[@]}"; do
    mkdir -p "$RES_DIR/mipmap-$density"
done
mkdir -p "$STORE_DIR"

# Function to create a PNG from the vector drawable
generate_icon() {
    local size=$1
    local output=$2
    local bg_color="#2196F3"
    local fg_color="#FFFFFF"
    
    # Check if ImageMagick is installed
    if ! command -v convert &> /dev/null; then
        echo "Warning: ImageMagick not installed. Creating placeholder: $output"
        touch "$output"
        return
    fi
    
    # Create a layered icon
    # Background layer
    convert -size "${size}x${size}" "xc:$bg_color" \
        -draw "circle $((size/2)),$((size/2)) $((size/2)),0" \
        "${output}.bg.png"
    
    # Foreground microphone shape
    mic_width=$((size/6))
    mic_height=$((size/3))
    mic_x=$((size/2 - mic_width/2))
    mic_y=$((size/2 - mic_height/2))
    
    convert -size "${size}x${size}" "xc:transparent" \
        -fill "$fg_color" \
        -draw "roundrectangle $mic_x,$mic_y $((mic_x + mic_width)),$((mic_y + mic_height)) 10,10" \
        -draw "arc $((size/2 - mic_width)),$((mic_y + mic_height)) $((size/2 + mic_width)),$((mic_y + mic_height + mic_width)) 0,180" \
        -draw "line $((size/2)),$((mic_y + mic_height + mic_width/2)) $((size/2)),$((mic_y + mic_height + mic_width))" \
        -draw "line $((size/2 - mic_width/2)),$((mic_y + mic_height + mic_width)) $((size/2 + mic_width/2)),$((mic_y + mic_height + mic_width))" \
        "${output}.fg.png"
    
    # Add voice waves
    wave_offset=$((size/4))
    convert "${output}.fg.png" \
        -stroke "#03DAC6" -strokewidth 3 -fill none \
        -draw "path 'M $((size/2 - wave_offset)),$((size/2 - 10)) Q $((size/2 - wave_offset - 5)),$((size/2)) $((size/2 - wave_offset)),$((size/2 + 10))'" \
        -draw "path 'M $((size/2 + wave_offset)),$((size/2 - 10)) Q $((size/2 + wave_offset + 5)),$((size/2)) $((size/2 + wave_offset)),$((size/2 + 10))'" \
        "${output}.fg.png"
    
    # Composite layers
    convert "${output}.bg.png" "${output}.fg.png" -composite "$output"
    
    # Clean up temp files
    rm -f "${output}.bg.png" "${output}.fg.png"
    
    echo "Generated: $output"
}

# Generate standard launcher icons
echo "Generating launcher icons..."
for density in "${!LAUNCHER_SIZES[@]}"; do
    size=${LAUNCHER_SIZES[$density]}
    output="$RES_DIR/mipmap-$density/ic_launcher.png"
    generate_icon $size "$output"
    
    # Also generate round version
    output="$RES_DIR/mipmap-$density/ic_launcher_round.png"
    generate_icon $size "$output"
done

# Generate Play Store icon (512x512)
echo "Generating Play Store icon..."
generate_icon 512 "$STORE_DIR/icon-512.png"

# Generate web icon (192x192)
echo "Generating web icon..."
generate_icon 192 "$STORE_DIR/icon-192.png"

# Generate notification icon (white on transparent)
echo "Generating notification icons..."
for density in "${!ICON_SIZES[@]}"; do
    size=${ICON_SIZES[$density]}
    output="$RES_DIR/drawable-$density/ic_notification.png"
    mkdir -p "$(dirname "$output")"
    
    if command -v convert &> /dev/null; then
        # Create white silhouette
        convert -size "${size}x${size}" "xc:transparent" \
            -fill white \
            -draw "roundrectangle $((size/2 - size/12)),$((size/2 - size/6)) $((size/2 + size/12)),$((size/2 + size/6)) 5,5" \
            "$output"
    else
        touch "$output"
    fi
done

# Create icon assets documentation
cat > "$STORE_DIR/README.md" << 'EOF'
# VoiceBridge App Icons

## Icon Design

The VoiceBridge icon features:
- Blue circular background (#2196F3)
- White microphone symbol
- Teal voice waves (#03DAC6)
- Orange accent dots (#FF9800)

## Icon Files

- `icon-512.png` - Play Store listing icon
- `icon-192.png` - Web app icon
- `ic_launcher.png` - App launcher icons (various sizes)
- `ic_launcher_round.png` - Round launcher icons
- `ic_notification.png` - Notification icons (white)

## Adaptive Icon

The app uses Android's adaptive icon system:
- Background: Solid blue color
- Foreground: Microphone with voice waves
- Monochrome: Same as foreground for themed icons

## Design Guidelines

1. Maintain the microphone as the central element
2. Keep voice waves subtle but visible
3. Ensure good contrast on various backgrounds
4. Test visibility at small sizes
5. Follow Material Design guidelines

## Color Palette

- Primary: #2196F3 (Blue)
- Secondary: #03DAC6 (Teal)
- Accent: #FF9800 (Orange)
- Background: #FFFFFF (White)

## Usage

- Never modify the logo proportions
- Maintain minimum clear space
- Use only on approved backgrounds
- Don't add effects or filters
EOF

echo "Icon generation complete!"
echo "Icons saved to: $RES_DIR"
echo "Store icon saved to: $STORE_DIR"
echo "Note: For production, consider using Android Asset Studio or a professional designer"