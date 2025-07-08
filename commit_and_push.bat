@echo off
echo ========================================
echo   Committing VoiceBridge Changes
echo ========================================
echo.

cd /d "D:\013-VoiceBridge"

echo 1. Checking git status...
git status

echo.
echo 2. Adding all changes...
git add .

echo.
echo 3. Committing with comprehensive message...
git commit -m "Complete production-ready release with legal protection

- Fixed Kotlin version compatibility (2.1.0)
- Added comprehensive legal protection (LICENSE, copyright headers)
- Updated build configuration for signed releases
- Created store distribution assets and checklists
- Added monetization strategy and revenue projections
- Prepared testing distribution package
- Updated README with licensing information
- Resolved all build errors and compatibility issues

Ready for beta testing and store distribution."

echo.
echo 4. Pushing to GitHub...
git push origin main

echo.
echo ========================================
echo   All Changes Pushed to GitHub!
echo ========================================
echo.
echo Recent changes now include:
echo - Legal protection (LICENSE + copyright headers)
echo - Kotlin 2.1.0 compatibility fixes
echo - Store distribution preparation
echo - Testing and monetization strategies
echo - Build system improvements
echo.
echo Ready to create GitHub release!
echo.
pause