@echo off
echo Adding copyright headers to source files...
echo.

cd /d "D:\013-VoiceBridge"

echo Adding headers to Kotlin files...
for /r %%f in (*.kt) do (
    echo Processing: %%f
    powershell -Command "
        $header = Get-Content 'COPYRIGHT_HEADER.java' -Raw;
        $content = Get-Content '%%f' -Raw;
        if (-not $content.StartsWith('/*')) {
            $header + \"`n\" + $content | Set-Content '%%f' -NoNewline
        }
    "
)

echo.
echo Adding headers to Java files...
for /r %%f in (*.java) do (
    echo Processing: %%f
    powershell -Command "
        $header = Get-Content 'COPYRIGHT_HEADER.java' -Raw;
        $content = Get-Content '%%f' -Raw;
        if (-not $content.StartsWith('/*')) {
            $header + \"`n\" + $content | Set-Content '%%f' -NoNewline
        }
    "
)

echo.
echo Adding headers to C++ files...
for /r %%f in (*.cpp *.h) do (
    echo Processing: %%f
    powershell -Command "
        $header = Get-Content 'COPYRIGHT_HEADER.java' -Raw;
        $content = Get-Content '%%f' -Raw;
        if (-not $content.StartsWith('/*')) {
            $header + \"`n\" + $content | Set-Content '%%f' -NoNewline
        }
    "
)

echo.
echo ========================================
echo   Copyright protection completed!
echo ========================================
echo.
echo Your intellectual property is now protected:
echo - LICENSE file added to repository
echo - Copyright headers added to all source files
echo - README updated with license information
echo.
echo Next steps:
echo 1. Commit and push these changes to GitHub
echo 2. Consider trademark registration for "VoiceBridge"
echo 3. Set up business entity (LLC/Corp) for additional protection
echo.
pause