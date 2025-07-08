#!/bin/bash

# Download Tesseract OCR language data files
# This script downloads the required tessdata files for OCR functionality

set -e

TESSDATA_DIR="android/app/src/main/assets/tessdata"
TESSDATA_URL="https://github.com/tesseract-ocr/tessdata/raw/main"

echo "Downloading Tesseract OCR language data files..."

# Create tessdata directory if it doesn't exist
mkdir -p "$TESSDATA_DIR"

# Download English language data (required)
echo "Downloading English language data..."
if [ ! -f "$TESSDATA_DIR/eng.traineddata" ]; then
    curl -L -o "$TESSDATA_DIR/eng.traineddata" "$TESSDATA_URL/eng.traineddata"
    echo "✓ English language data downloaded"
else
    echo "✓ English language data already exists"
fi

# Download Spanish language data (optional)
echo "Downloading Spanish language data..."
if [ ! -f "$TESSDATA_DIR/spa.traineddata" ]; then
    curl -L -o "$TESSDATA_DIR/spa.traineddata" "$TESSDATA_URL/spa.traineddata"
    echo "✓ Spanish language data downloaded"
else
    echo "✓ Spanish language data already exists"
fi

# Download French language data (optional)
echo "Downloading French language data..."
if [ ! -f "$TESSDATA_DIR/fra.traineddata" ]; then
    curl -L -o "$TESSDATA_DIR/fra.traineddata" "$TESSDATA_URL/fra.traineddata"
    echo "✓ French language data downloaded"
else
    echo "✓ French language data already exists"
fi

# Download German language data (optional)
echo "Downloading German language data..."
if [ ! -f "$TESSDATA_DIR/deu.traineddata" ]; then
    curl -L -o "$TESSDATA_DIR/deu.traineddata" "$TESSDATA_URL/deu.traineddata"
    echo "✓ German language data downloaded"
else
    echo "✓ German language data already exists"
fi

echo ""
echo "All Tesseract language data files have been downloaded successfully!"
echo "Total files in tessdata directory:"
ls -la "$TESSDATA_DIR"/*.traineddata 2>/dev/null | wc -l

echo ""
echo "Note: These files are large (~10MB each) and are excluded from git."
echo "Run this script after cloning the repository to download the required files."