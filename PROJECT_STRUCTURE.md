# Project Directory Structure

This document provides an overview of the top-level directories in the Chernobyl Scientific Project, explaining the purpose of each one.

---

### Core Project Directories

*   `/src`
    *   **Purpose:** The heart of the project. Contains all Java source code (`main`), test code (`test`), and non-code assets like `fabric.mod.json` (`resources`).

*   `/build`
    *   **Purpose:** The standard output directory for all Gradle build operations. It contains compiled classes, processed resources, and the final packaged `.jar` file for the mod. This directory is temporary and is ignored by version control.

*   `/config`
    *   **Purpose:** Contains all configuration files for our static analysis tools: Checkstyle, PMD, and SpotBugs. Centralizing these files here allows us to consistently enforce the project's coding standards.

*   `/gradle`
    *   **Purpose:** Holds the Gradle Wrapper files (`gradle-wrapper.jar` and `gradle-wrapper.properties`). The wrapper ensures that any contributor can build the project with the correct, consistent version of Gradle without needing to install it manually.

*   `/scripts`
    *   **Purpose:** A home for custom utility and automation scripts. This is where our **Automated Error Correction System** (`aecs.sh`) lives.

*   `/lisp`
    *   **Purpose:** Contains all Common Lisp source code for the AECS-II "Moonshot" project. This includes ASDF system definitions (`.asd`) and Lisp source files (`.lisp`).

### Development Environment Directories

*   `/.devcontainer`
    *   **Purpose:** Defines the configuration for the VS Code Development Container. This includes the base Docker image, required extensions, and commands to run after the container is created, ensuring a consistent development environment for everyone.

*   `/.vscode`
    *   **Purpose:** Holds VS Code workspace-specific settings. This includes recommended extensions, debugger configurations, and custom tasks like our **AECS Bridge** (`tasks.json`).

*   `/run`
    *   **Purpose:** Used by the Fabric Loom toolchain to store the client and server run configurations for development. This is where test worlds, logs, and other game-related files are stored during testing. It is ignored by version control.

### Gradle Cache

*   `/.gradle`
    *   **Purpose:** Gradle's local cache directory. It stores dependency artifacts, build caches, and information for the Gradle Daemon to speed up builds. It is specific to the local machine and is ignored by version control.

---

### Key Root Files

*   `build.gradle`: The main build script for the project. It defines dependencies, plugins (Fabric Loom, Checkstyle, etc.), and tasks required to build and run the mod.

*   `gradlew` & `gradlew.bat`: The Gradle Wrapper scripts for POSIX-based systems (Linux/macOS) and Windows. They ensure anyone can build the project with the correct Gradle version without needing to install it manually.

*   `settings.gradle`: A Gradle script that defines settings for the entire project build, such as the root project's name and the repositories for Gradle plugins.

*   `gradle.properties`: A property file used to configure project-wide settings like the Minecraft version, mod version, and JVM arguments for Gradle.

*   `PROJECT_NOTES.md`: The project's living "logbook." It documents our core philosophies, coding standards, and captures ideas and design decisions.

*   `PROJECT_STRUCTURE.md`: This file. It serves as the formal documentation for the project's directory and file layout.

*   `README.md`: The primary entry point for anyone viewing the project. It provides a brief overview and links to more detailed documentation.

*   `.gitignore`: Tells the Git version control system which files and directories to ignore (e.g., `build`, `.gradle`, `run`).