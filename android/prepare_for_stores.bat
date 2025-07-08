@echo off
echo ========================================
echo   VoiceBridge Store Preparation
echo ========================================
echo.

cd /d "D:\013-VoiceBridge\android"
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

echo 1. Building signed APK for distribution...
gradlew.bat clean
gradlew.bat assembleRelease

echo.
echo 2. Building Android App Bundle (AAB)...
gradlew.bat bundleRelease

echo.
echo 3. Creating store assets directory...
mkdir ..\store_assets\ready_to_upload 2>nul

echo.
echo 4. Copying release files...
copy app\build\outputs\apk\release\app-release.apk ..\store_assets\ready_to_upload\voicebridge-v1.0.0-beta.1.apk
copy app\build\outputs\bundle\release\app-release.aab ..\store_assets\ready_to_upload\voicebridge-v1.0.0-beta.1.aab

echo.
echo ========================================
echo   Build Complete!
echo ========================================
echo.
echo Ready for upload:
echo - APK: store_assets\ready_to_upload\voicebridge-v1.0.0-beta.1.apk
echo - AAB: store_assets\ready_to_upload\voicebridge-v1.0.0-beta.1.aab
echo.
echo Next steps:
echo 1. Take screenshots using Android Studio emulator
echo 2. Create 512x512 app icon
echo 3. Upload to Samsung Galaxy Store
echo 4. Upload to Amazon Appstore
echo 5. Upload to APKPure
echo.
echo For detailed instructions, see:
echo store_assets\store_upload_checklist.md
echo.
pause