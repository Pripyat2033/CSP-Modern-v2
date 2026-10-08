#!/usr/bin/powershell
# Install Java 25 JDK for Gradle builds (alongside existing Java 21)
# This script downloads and sets up Java 25 in a separate directory

$ErrorActionPreference = "Stop"

$JAVA_25_BASE = "C:/Users/Ben/AppData/Local/Programs/Eclipse Adoptium/jdk-25.0.1"
$DOWNLOAD_PATH = "C:/Users/Ben/Documents/CSP-Modern/tmp/java-downloads"
$MSI_URL = "https://github.com/AdoptOpenJDK/openjdk25-binaries/releases/download/jdk-25%2B36/OpenJDK25U-jdk_x64_windows_hotspot_25.0.1_36.msi"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Java 25 JDK Installation Script" -ForegroundColor Cyan
Write-Host "Target: $JAVA_25_BASE" -ForegroundColor Cyan
Write-Host "Minecraft 26.x requires Java 25 for running the game client" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Cyan

# Create download directory if it doesn't exist
if (!(Test-Path $DOWNLOAD_PATH)) {
    New-Item -ItemType Directory -Force -Path $DOWNLOAD_PATH | Out-Null
}

# Check if Java 25 is already installed
if (Test-Path "$JAVA_25_BASE/bin/java.exe") {
    Write-Host "" -ForegroundColor Green
    Write-Host "Java 25 is already installed at: $JAVA_25_BASE" -ForegroundColor Green
    Write-Host "" -ForegroundColor Cyan
    Write-Host "You can now configure your build environment to use it." -ForegroundColor Cyan
    Write-Host "Set JAVA_HOME before building with Gradle:" -ForegroundColor Cyan
    Write-Host "  export JAVA_HOME=`"$JAVA_25_BASE`"" -ForegroundColor Cyan
    Write-Host "" -ForegroundColor Cyan
    Write-Host "Or create a batch file like:" -ForegroundColor Cyan
    Write-Host "  set JAVA_HOME=C:\Users\Ben\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.1" -ForegroundColor Cyan
    Write-Host "  cd C:\Users\Ben\Documents\CSP-Modern" -ForegroundColor Cyan
    Write-Host "  gradlew build --no-daemon" -ForegroundColor Cyan
    return
}

Write-Host "" -ForegroundColor Yellow
Write-Host "Java 25 is not yet installed. Checking download..." -ForegroundColor Yellow

# Check if MSI exists (may need manual download)
if (!(Test-Path "$DOWNLOAD_PATH\OpenJDK25U-jdk_x64_windows_hotspot_25.0.1_36.msi")) {
    Write-Host "" -ForegroundColor Yellow
    Write-Host "Direct download may fail due to GitHub rate limiting." -ForegroundColor Yellow
    Write-Host "Please manually download Java 25 MSI from:" -ForegroundColor Yellow
    Write-Host "https://github.com/AdoptOpenJDK/openjdk25-binaries/releases" -ForegroundColor Yellow
    Write-Host "" -ForegroundColor Yellow
    Write-Host "Select: OpenJDK25U-jdk_x64_windows_hotspot_25.0.1_36.msi" -ForegroundColor Yellow
    Write-Host "Save to: C:\Users\Ben\Documents\CSP-Modern/tmp/" -ForegroundColor Yellow
    Write-Host "" -ForegroundColor Cyan
    Read-Host "Press Enter when you've downloaded the MSI..."
    
    # Wait for user to download, then copy it
    while (-not (Test-Path "$DOWNLOAD_PATH\OpenJDK25U-jdk_x64_windows_hotspot_25.0.1_36.msi")) {
        Start-Sleep -Seconds 2
    }
}

Write-Host "" -ForegroundColor Green
Write-Host "Found MSI file: OpenJDK25U-jdk_x64_windows_hotspot_25.0.1_36.msi" -ForegroundColor Green

# Uninstall the MSI silently
Write-Host "" -ForegroundColor Cyan
Write-Host "Installing Java 25..." -ForegroundColor Cyan
msiexec /qn /i "$DOWNLOAD_PATH\OpenJDK25U-jdk_x64_windows_hotspot_25.0.1_36.msi" `
    PRODUCTID="Adoptium_JDK_HotSpot_OpenJDK25U_X64" `
    REBOOT="reboot=no" `
    LOGPATH="$DOWNLOAD_PATH\java25-install.log"

if ($LASTEXITCODE -eq 0) {
    Write-Host "" -ForegroundColor Green
    Write-Host "Java 25 installed successfully!" -ForegroundColor Green
    
    # Verify installation
    if (Test-Path "$JAVA_25_BASE/bin/java.exe") {
        Write-Host "" -ForegroundColor Cyan
        Write-Host "Installation verified:" -ForegroundColor Cyan
        & "$JAVA_25_BASE\bin\java.exe" -version 2>&1 | Select-Object -First 2
        
        Write-Host "" -ForegroundColor Green
        Write-Host "=========================================" -ForegroundColor Green
        Write-Host "Java 25 installation complete!" -ForegroundColor Green
        Write-Host "=========================================" -ForegroundColor Green
        Write-Host "" -ForegroundColor Cyan
        Write-Host "Next steps:" -ForegroundColor Cyan
        Write-Host "1. Run Gradle build with Java 25 for builds only:" -ForegroundColor Cyan
        Write-Host "   set JAVA_HOME=C:\Users\Ben\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.1" -ForegroundColor Cyan
        Write-Host "   cd C:\Users\Ben\Documents\CSP-Modern" -ForegroundColor Cyan
        Write-Host "   gradlew build --no-daemon" -ForegroundColor Cyan
        Write-Host "" -ForegroundColor Cyan
        Write-Host "2. Game client still uses your existing Java 21 for launching Minecraft" -ForegroundColor Cyan
        Write-Host "(Minecraft 26.x game runs on Java 25, but Gradle builds use Java 25 too)" -ForegroundColor Cyan
        
    } else {
        throw "Java installation verification failed!"
    }
} else {
    throw "Failed to install Java 25. MSI exit code: $LASTEXITCODE"
}
