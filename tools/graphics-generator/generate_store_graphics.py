#!/usr/bin/env python3
"""
VoiceBridge Play Store Graphics Generator
Generates all required graphics for Google Play Store submission
"""

import os
import sys
from PIL import Image, ImageDraw, ImageFont
import argparse

# Play Store graphic requirements
STORE_GRAPHICS = {
    'icon': (512, 512),  # High-res icon
    'feature_graphic': (1024, 500),  # Feature graphic
    'phone_screenshots': [(1080, 1920), (1080, 2340)],  # Phone screenshots
    'tablet_screenshots': [(1200, 1920), (2048, 2732)],  # Tablet screenshots
    'tv_banner': (1280, 720),  # Android TV banner
    'tv_screenshot': (1920, 1080)  # Android TV screenshot
}

def create_background_gradient(width, height):
    """Create a modern gradient background"""
    img = Image.new('RGB', (width, height))
    draw = ImageDraw.Draw(img)
    
    # Create vertical gradient from blue to dark blue
    for y in range(height):
        r = int(33 + (13 * y / height))  # 33 -> 46
        g = int(150 + (104 * y / height))  # 150 -> 254
        b = int(243 + (12 * y / height))  # 243 -> 255
        color = (r, g, b)
        draw.line([(0, y), (width, y)], fill=color)
    
    return img

def add_logo_and_text(img, title, subtitle="", logo_size=None):
    """Add VoiceBridge logo and text to an image"""
    draw = ImageDraw.Draw(img)
    width, height = img.size
    
    # Try to load a font, fallback to default
    try:
        title_font = ImageFont.truetype("/System/Library/Fonts/Arial.ttf", 48)
        subtitle_font = ImageFont.truetype("/System/Library/Fonts/Arial.ttf", 32)
    except:
        try:
            title_font = ImageFont.truetype("arial.ttf", 48)
            subtitle_font = ImageFont.truetype("arial.ttf", 32)
        except:
            title_font = ImageFont.load_default()
            subtitle_font = ImageFont.load_default()
    
    # Calculate logo position
    if logo_size is None:
        logo_size = min(width // 4, height // 4)
    
    logo_x = (width - logo_size) // 2
    logo_y = height // 3 - logo_size // 2
    
    # Draw microphone icon
    mic_color = (255, 255, 255, 255)
    mic_center_x = logo_x + logo_size // 2
    mic_center_y = logo_y + logo_size // 2
    mic_size = logo_size // 2
    
    # Microphone body
    mic_body_width = mic_size // 3
    mic_body_height = mic_size // 2
    mic_body_x = mic_center_x - mic_body_width // 2
    mic_body_y = mic_center_y - mic_body_height // 2
    
    draw.rounded_rectangle([
        (mic_body_x, mic_body_y),
        (mic_body_x + mic_body_width, mic_body_y + mic_body_height)
    ], mic_body_width // 4, fill=mic_color)
    
    # Microphone stand
    stand_width = 3
    stand_height = logo_size // 8
    stand_x = mic_center_x - stand_width // 2
    stand_y = mic_center_y + mic_body_height // 4
    
    draw.rectangle([
        (stand_x, stand_y),
        (stand_x + stand_width, stand_y + stand_height)
    ], fill=mic_color)
    
    # Sound waves
    for i in range(2):
        wave_radius = mic_size // 3 + (i + 1) * logo_size // 12
        wave_box = [
            (mic_center_x - wave_radius, mic_center_y - wave_radius),
            (mic_center_x + wave_radius, mic_center_y + wave_radius)
        ]
        draw.arc(wave_box, start=-30, end=30, fill=mic_color, width=3)
        draw.arc(wave_box, start=150, end=210, fill=mic_color, width=3)
    
    # Add title text
    title_bbox = draw.textbbox((0, 0), title, font=title_font)
    title_width = title_bbox[2] - title_bbox[0]
    title_x = (width - title_width) // 2
    title_y = logo_y + logo_size + 40
    
    draw.text((title_x, title_y), title, fill=(255, 255, 255), font=title_font)
    
    # Add subtitle if provided
    if subtitle:
        subtitle_bbox = draw.textbbox((0, 0), subtitle, font=subtitle_font)
        subtitle_width = subtitle_bbox[2] - subtitle_bbox[0]
        subtitle_x = (width - subtitle_width) // 2
        subtitle_y = title_y + 60
        
        draw.text((subtitle_x, subtitle_y), subtitle, fill=(255, 255, 255, 200), font=subtitle_font)

def generate_high_res_icon():
    """Generate 512x512 high-resolution icon"""
    size = 512
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Background color
    bg_color = (33, 150, 243, 255)
    corner_radius = size // 8
    draw.rounded_rectangle([(0, 0), (size, size)], corner_radius, fill=bg_color)
    
    # Microphone icon (larger and more detailed)
    mic_center_x = size // 2
    mic_center_y = size // 2
    mic_size = size // 2
    
    # Microphone body
    mic_body_width = mic_size // 2
    mic_body_height = mic_size // 1.5
    mic_body_x = mic_center_x - mic_body_width // 2
    mic_body_y = mic_center_y - mic_body_height // 2
    
    draw.rounded_rectangle([
        (mic_body_x, mic_body_y),
        (mic_body_x + mic_body_width, mic_body_y + mic_body_height)
    ], mic_body_width // 3, fill=(255, 255, 255, 255))
    
    # Microphone details
    detail_size = mic_body_width // 8
    for i in range(3):
        detail_y = mic_body_y + mic_body_height // 4 + i * (mic_body_height // 6)
        draw.ellipse([
            (mic_center_x - detail_size, detail_y),
            (mic_center_x + detail_size, detail_y + detail_size)
        ], fill=(33, 150, 243, 255))
    
    # Microphone stand
    stand_width = 6
    stand_height = size // 8
    stand_x = mic_center_x - stand_width // 2
    stand_y = mic_center_y + mic_body_height // 2
    
    draw.rectangle([
        (stand_x, stand_y),
        (stand_x + stand_width, stand_y + stand_height)
    ], fill=(255, 255, 255, 255))
    
    # Microphone base
    base_width = size // 4
    base_height = 8
    base_x = mic_center_x - base_width // 2
    base_y = stand_y + stand_height
    
    draw.rounded_rectangle([
        (base_x, base_y),
        (base_x + base_width, base_y + base_height)
    ], base_height // 2, fill=(255, 255, 255, 255))
    
    # Sound waves
    wave_color = (255, 255, 255, 200)
    for i in range(4):
        wave_radius = mic_size // 2 + (i + 1) * size // 16
        wave_box = [
            (mic_center_x - wave_radius, mic_center_y - wave_radius),
            (mic_center_x + wave_radius, mic_center_y + wave_radius)
        ]
        draw.arc(wave_box, start=-45, end=45, fill=wave_color, width=4)
        draw.arc(wave_box, start=135, end=225, fill=wave_color, width=4)
    
    return img

def generate_feature_graphic():
    """Generate 1024x500 feature graphic"""
    width, height = 1024, 500
    img = create_background_gradient(width, height)
    
    add_logo_and_text(
        img,
        "VoiceBridge",
        "Fill Forms with Your Voice Using AI",
        logo_size=120
    )
    
    return img

def generate_phone_screenshot():
    """Generate phone screenshot mockup"""
    width, height = 1080, 1920
    img = create_background_gradient(width, height)
    
    # Add phone UI mockup
    draw = ImageDraw.Draw(img)
    
    # Status bar
    draw.rectangle([(0, 0), (width, 80)], fill=(0, 0, 0, 100))
    
    # Main content area
    content_margin = 60
    content_y = 200
    
    # App title
    try:
        title_font = ImageFont.truetype("arial.ttf", 40)
        text_font = ImageFont.truetype("arial.ttf", 28)
    except:
        title_font = ImageFont.load_default()
        text_font = ImageFont.load_default()
    
    draw.text((content_margin, content_y), "VoiceBridge", fill=(255, 255, 255), font=title_font)
    
    # Feature list
    features = [
        "🎤 Voice-controlled form filling",
        "📱 Document scanning with OCR",
        "🤖 AI-powered automation",
        "🔒 Privacy-focused design",
        "♿ Accessibility features"
    ]
    
    y_offset = content_y + 100
    for feature in features:
        draw.text((content_margin, y_offset), feature, fill=(255, 255, 255), font=text_font)
        y_offset += 60
    
    # Microphone button mockup
    button_size = 120
    button_x = (width - button_size) // 2
    button_y = height - 300
    
    draw.ellipse([
        (button_x, button_y),
        (button_x + button_size, button_y + button_size)
    ], fill=(255, 87, 34, 255))  # Orange accent
    
    # Microphone icon in button
    mic_size = 40
    mic_x = button_x + (button_size - mic_size) // 2
    mic_y = button_y + (button_size - mic_size) // 2
    
    draw.rounded_rectangle([
        (mic_x, mic_y),
        (mic_x + mic_size, mic_y + mic_size)
    ], mic_size // 4, fill=(255, 255, 255))
    
    return img

def generate_tablet_screenshot():
    """Generate tablet screenshot mockup"""
    width, height = 1200, 1920
    img = create_background_gradient(width, height)
    
    # Similar to phone but with more content
    add_logo_and_text(
        img,
        "VoiceBridge for Tablets",
        "Enhanced productivity for larger screens",
        logo_size=150
    )
    
    return img

def main():
    """Generate all Play Store graphics"""
    print("Generating Play Store graphics for VoiceBridge...")
    
    # Create output directory
    output_dir = "store/graphics/generated"
    os.makedirs(output_dir, exist_ok=True)
    
    # Generate high-res icon
    print("Generating high-resolution icon...")
    icon = generate_high_res_icon()
    icon.save(os.path.join(output_dir, "ic_launcher_512.png"), 'PNG')
    
    # Generate feature graphic
    print("Generating feature graphic...")
    feature_graphic = generate_feature_graphic()
    feature_graphic.save(os.path.join(output_dir, "feature_graphic.png"), 'PNG')
    
    # Generate phone screenshots
    print("Generating phone screenshots...")
    phone_screenshot = generate_phone_screenshot()
    phone_screenshot.save(os.path.join(output_dir, "phone_screenshot_1.png"), 'PNG')
    
    # Generate tablet screenshot
    print("Generating tablet screenshot...")
    tablet_screenshot = generate_tablet_screenshot()
    tablet_screenshot.save(os.path.join(output_dir, "tablet_screenshot_1.png"), 'PNG')
    
    print("All Play Store graphics generated successfully!")
    print(f"Graphics saved to: {output_dir}")

if __name__ == "__main__":
    main()