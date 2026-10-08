@echo off
REM Build Gradle project using Java 25 (required for Minecraft 26.x compatibility)
set JAVA_HOME=C:\Users\Ben\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.1

REM Verify Java 25 is installed and configured correctly
java -version

cd /d "%~dp0"

REM Build the mod using Gradle wrapper
gradlew.bat clean build --no-daemon --info

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==========================================
    echo BUILD SUCCESSFUL WITH JAVA 25!
    echo ==========================================
    echo.
    echo JAR files location:
    dir /b build\libs\*.jar
    
    echo.
    echo Next steps:
    echo 1. Copy csp-modern-0.1.0.jar to your Minecraft mods folder
    echo    (Your game is already set up with Minecraft 26.x)
    echo.
    echo Note: The game client runs on Java 25, so ensure Java 25
    echo      is also available when launching Minecraft 26.x!
) else (
    echo.
    echo ==========================================
    echo BUILD FAILED - Please check the errors above
    echo ==========================================
    echo.
    echo Common fixes:
    echo 1. Make sure Java 25 is installed at JAVA_HOME setting above
    echo 2. Verify you have downloaded and installed the OpenJDK 25 MSI
    echo 3. Try running 'java -version' to confirm Java 25 is accessible
)

pause
