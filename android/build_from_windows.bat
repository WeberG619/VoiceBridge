@echo off
cd /d "D:\013-VoiceBridge\android"
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
gradlew.bat clean
gradlew.bat assembleRelease
pause