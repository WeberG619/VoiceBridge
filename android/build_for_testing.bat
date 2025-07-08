@echo off
echo ========================================
echo   VoiceBridge Testing Distribution
echo ========================================
echo.

cd /d "D:\013-VoiceBridge\android"
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

echo 1. Building test APK...
gradlew.bat clean
gradlew.bat assembleRelease

echo.
echo 2. Creating testing package...
mkdir ..\testing_distribution 2>nul

echo.
echo 3. Copying files for distribution...
copy app\build\outputs\apk\release\app-release.apk ..\testing_distribution\voicebridge-beta-test.apk

echo.
echo 4. Creating installation instructions...
echo VoiceBridge Beta Test Installation > ..\testing_distribution\INSTALL_INSTRUCTIONS.txt
echo. >> ..\testing_distribution\INSTALL_INSTRUCTIONS.txt
echo 1. Enable "Unknown Sources" in Android Settings >> ..\testing_distribution\INSTALL_INSTRUCTIONS.txt
echo 2. Download voicebridge-beta-test.apk to your device >> ..\testing_distribution\INSTALL_INSTRUCTIONS.txt
echo 3. Tap the APK file to install >> ..\testing_distribution\INSTALL_INSTRUCTIONS.txt
echo 4. Grant all permissions when prompted >> ..\testing_distribution\INSTALL_INSTRUCTIONS.txt
echo 5. Enable VoiceBridge Accessibility Service in Settings >> ..\testing_distribution\INSTALL_INSTRUCTIONS.txt
echo. >> ..\testing_distribution\INSTALL_INSTRUCTIONS.txt
echo Report issues to: weber.voicebridge@gmail.com >> ..\testing_distribution\INSTALL_INSTRUCTIONS.txt

echo.
echo 5. Generating QR code link for easy sharing...
echo [QR CODE] Download: https://github.com/WeberG619/VoiceBridge/releases > ..\testing_distribution\SHARE_LINK.txt

echo.
echo ========================================
echo   Testing Package Ready!
echo ========================================
echo.
echo Files created in: testing_distribution\
echo - voicebridge-beta-test.apk (main install file)
echo - INSTALL_INSTRUCTIONS.txt (user guide)
echo - SHARE_LINK.txt (sharing info)
echo.
echo Next steps:
echo 1. Upload APK to GitHub Releases
echo 2. Test on your own devices first
echo 3. Share with friends/family for testing
echo 4. Upload to APKPure for broader testing
echo.
echo Ready to test? Install on your device:
echo adb install testing_distribution\voicebridge-beta-test.apk
echo.
pause