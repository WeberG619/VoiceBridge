#!/bin/bash
# VoiceBridge Skill Validation Script
# Makes skill-lint easily accessible for CI/CD and developers

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILLS_DIR="${SCRIPT_DIR}/../skills"
SKILL_LINT="${SCRIPT_DIR}/skill-lint.py"

echo "🔍 VoiceBridge Skill Template Validator"
echo "======================================="

# Check if Python is available
if ! command -v python3 &> /dev/null; then
    echo "❌ Error: Python 3 is required but not installed"
    exit 1
fi

# Check if PyYAML is available
if ! python3 -c "import yaml" 2>/dev/null; then
    echo "📦 Installing required dependencies..."
    pip3 install PyYAML
fi

# Make skill-lint executable
chmod +x "$SKILL_LINT"

# Validate all skills by default, or specific file if provided
if [ $# -eq 0 ]; then
    echo "🔍 Validating all skill templates in $SKILLS_DIR"
    python3 "$SKILL_LINT" "$SKILLS_DIR" --validate-all
else
    echo "🔍 Validating $1"
    python3 "$SKILL_LINT" "$1"
fi

echo ""
echo "✨ Validation complete!"