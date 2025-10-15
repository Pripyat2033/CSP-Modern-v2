# Project Environment Setup

This document explains how to set up a native development environment for the Chernobyl Scientific Project on macOS. We have moved away from a Docker-based workflow to improve performance and simplify the setup process.

## Purpose

The primary goal is to install the correct versions of Java and Gradle directly on your machine. Due to challenges with Homebrew on older versions of macOS, the recommended approach is a manual installation.

## Native Setup Process

The setup involves two main parts: manually installing the required SDKs and then running a script to verify and configure the environment.

### 1. Manual Installation

#### Java (OpenJDK 17)
1.  Download a Java 17 installer from a trusted provider. For Intel Macs, Azul Zulu is recommended.
    *   **Download Link (Intel Mac):** [Azul Zulu JDK 17 DMG](https://cdn.azul.com/zulu/bin/zulu17.48.15-ca-jdk17.0.10-macosx_x64.dmg)
2.  Open the downloaded `.dmg` file and run the `.pkg` installer inside.

#### Gradle (Version 8.7)
1.  Download the "binary-only" `.zip` file for Gradle 8.7 from the official releases page:
    *   **Download Link:** Gradle 8.7 Binary-Only ZIP
2.  Create a directory for your SDKs in your home folder: `mkdir -p ~/sdks`
3.  Unzip the Gradle file into that directory. You can do this from the terminal (after moving the file from Downloads) or using the Finder.
    *   `unzip ~/gradle-8.7-bin.zip -d ~/sdks/`

### 2. Run Verification Script

After manually installing Java and Gradle, a script will verify the setup and configure your shell.

1.  **Open your Terminal** and navigate to the project directory.
2.  **Run the script:** `./scripts/setup-native-env.sh`

#### What the Script Does

1.  **Verifies Java:** Confirms that a Java 17 JDK is installed and available.
2.  **Verifies Gradle:** Confirms that Gradle 8.7 is located in the `~/sdks/` directory.
3.  **Configures Shell:** Adds the path to your Gradle installation to your `.zshrc` file so the `gradle` command works correctly from anywhere.
4.  **Sets Permissions:** Makes the core Gradle wrapper (`gradlew`) and other utility scripts executable.

After running the script, **restart your terminal** or run `source ~/.zshrc`. You will now have a fully functional native Java and Gradle environment, and you can build the project by simply running `./gradlew build` from the integrated terminal.

The `.devcontainer` directory can remain for now, as it's a useful reference, but it is no longer the primary way to develop.
