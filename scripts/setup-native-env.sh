#!/bin/bash

# Native Environment Setup Script for CSP-Modern on macOS
# This script verifies the manual installation of the required development
# tools and configures the shell environment.

set -e

# --- Configuration ---
GRADLE_VERSION="8.7"

echo "🚀 Starting CSP-Modern native environment setup for macOS..."

# --- 1. Prerequisites Check ---
if ! xcode-select -p &> /dev/null; then
    echo "❌ Xcode Command Line Tools not found. They are required by Homebrew."
    echo "   Please run 'xcode-select --install' in your terminal, complete the installation, and then re-run this script."
    exit 1
fi

echo "✅ Xcode Command Line Tools found."

# --- 2. Verify Core Dependencies ---
echo "☕ Verifying Java 17 installation..."
if ! java -version 2>&1 | grep -q "17."; then
    echo "❌ Java 17 not found."
    echo "   Please install it manually using a provider like Azul Zulu before running this script."
    echo "   Download for Intel Macs: https://cdn.azul.com/zulu/bin/zulu17.48.15-ca-jdk17.0.10-macosx_x64.dmg"
    exit 1
fi
echo "✅ Java 17 found."

echo "🐘 Verifying Gradle 8.x installation..."
if ! [ -d "$HOME/sdks/gradle-${GRADLE_VERSION}" ]; then
    echo "❌ Gradle ${GRADLE_VERSION} not found in '$HOME/sdks/'."
    echo "   Please download the 'binary-only' zip for version ${GRADLE_VERSION} from https://gradle.org/releases/ and unzip it to '$HOME/sdks/'."
    exit 1
fi
echo "✅ Gradle ${GRADLE_VERSION} found."

# --- 3. Configure Environment ---
# This step adds the necessary paths to your .zshrc file.
echo "⚙️ Configuring shell environment..."
{
    echo ''
    echo '# Manual SDK paths for CSP-Modern'
    echo "export GRADLE_HOME=\"\$HOME/sdks/gradle-${GRADLE_VERSION}\""
    echo 'export PATH="$GRADLE_HOME/bin:$PATH"'
} >> ~/.zshrc

# --- 4. Final Steps ---
echo "🔧 Making project scripts executable..."
chmod +x gradlew
chmod +x scripts/*.sh

echo "✅🎉 Native environment setup complete!"
echo "Please RESTART your terminal or run 'source ~/.zshrc' to apply the changes."
echo "After restarting, you can verify the installations with 'java -version' and 'gradle --version'."
