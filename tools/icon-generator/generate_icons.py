#!/usr/bin/env python3
"""
VoiceBridge App Icon Generator
Generates app icons for all Android density folders
"""

import os
import sys
from PIL import Image, ImageDraw, ImageFont
import argparse

# Android icon sizes for different densities
ICON_SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

def create_voice_bridge_icon(size, output_path):
    """Create a VoiceBridge app icon with microphone and sound waves"""
    
    # Create a new image with rounded corners
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Background color - Material Design Blue
    bg_color = (33, 150, 243, 255)  # Blue 500
    
    # Draw rounded rectangle background
    corner_radius = size // 8
    draw.rounded_rectangle([(0, 0), (size, size)], corner_radius, fill=bg_color)
    
    # Microphone icon in the center
    mic_center_x = size // 2
    mic_center_y = size // 2
    mic_size = size // 3
    
    # Microphone body (rounded rectangle)
    mic_body_width = mic_size // 2
    mic_body_height = mic_size
    mic_body_x = mic_center_x - mic_body_width // 2
    mic_body_y = mic_center_y - mic_body_height // 2
    
    # Draw microphone body
    draw.rounded_rectangle([
        (mic_body_x, mic_body_y),
        (mic_body_x + mic_body_width, mic_body_y + mic_body_height)
    ], mic_body_width // 4, fill=(255, 255, 255, 255))
    
    # Microphone stand
    stand_width = 2
    stand_height = size // 6
    stand_x = mic_center_x - stand_width // 2
    stand_y = mic_center_y + mic_body_height // 2
    
    draw.rectangle([
        (stand_x, stand_y),
        (stand_x + stand_width, stand_y + stand_height)
    ], fill=(255, 255, 255, 255))
    
    # Microphone base
    base_width = size // 6
    base_height = 3
    base_x = mic_center_x - base_width // 2
    base_y = stand_y + stand_height
    
    draw.rectangle([
        (base_x, base_y),
        (base_x + base_width, base_y + base_height)
    ], fill=(255, 255, 255, 255))
    
    # Sound waves (arcs around microphone)
    wave_color = (255, 255, 255, 180)  # Semi-transparent white
    wave_thickness = max(1, size // 48)
    
    for i in range(3):
        wave_radius = mic_size // 2 + (i + 1) * size // 12
        wave_box = [
            (mic_center_x - wave_radius, mic_center_y - wave_radius),
            (mic_center_x + wave_radius, mic_center_y + wave_radius)
        ]
        
        # Draw partial arcs for sound waves
        draw.arc(wave_box, start=-45, end=45, fill=wave_color, width=wave_thickness)
        draw.arc(wave_box, start=135, end=225, fill=wave_color, width=wave_thickness)
    
    # Save the icon
    img.save(output_path, 'PNG')
    print(f"Generated icon: {output_path} ({size}x{size})")

def generate_all_icons():
    """Generate app icons for all Android density folders"""
    
    # Base directory for Android resources
    base_dir = "android/app/src/main/res"
    
    # Create icons for each density
    for density, size in ICON_SIZES.items():
        # Create directory if it doesn't exist
        icon_dir = os.path.join(base_dir, f"mipmap-{density}")
        os.makedirs(icon_dir, exist_ok=True)
        
        # Generate icon file
        icon_path = os.path.join(icon_dir, "ic_launcher.png")
        create_voice_bridge_icon(size, icon_path)
        
        # Also create round icon (same for now)
        round_icon_path = os.path.join(icon_dir, "ic_launcher_round.png")
        create_voice_bridge_icon(size, round_icon_path)

def main():
    """Main function"""
    parser = argparse.ArgumentParser(description='Generate VoiceBridge app icons')
    parser.add_argument('--size', type=int, help='Generate single icon of specified size')
    parser.add_argument('--output', type=str, help='Output file path for single icon')
    
    args = parser.parse_args()
    
    if args.size and args.output:
        # Generate single icon
        create_voice_bridge_icon(args.size, args.output)
    else:
        # Generate all icons
        print("Generating VoiceBridge app icons for all densities...")
        generate_all_icons()
        print("All icons generated successfully!")

if __name__ == "__main__":
    main()