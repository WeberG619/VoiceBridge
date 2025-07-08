@echo off
cd /d "D:\013-VoiceBridge\android"
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

echo Building signed Android App Bundle...
gradlew.bat bundleRelease

echo.
echo Build completed! Check app/build/outputs/bundle/release/ for the AAB file.
echo.
echo To generate signed APK instead, run:
echo gradlew.bat assembleRelease
echo.
pause