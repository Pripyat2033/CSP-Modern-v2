# Dev Container Post-Creation Setup (`post-create.sh`)

This document explains the steps performed by the `post-create.sh` script. This script is configured in `devcontainer.json` to run automatically after the development container is created, ensuring the environment is fully prepared for development.

## Purpose

The primary goal of this script is to prepare the development environment by installing and configuring necessary tools that are not part of the base Docker image. It is designed to be robust and idempotent, meaning it can be run multiple times without causing issues.

## Execution Flow

The script follows a specific sequence of operations:

### 1. Caching Mechanism

To dramatically speed up container rebuilds (e.g., after changing the `Dockerfile`), the script first checks for a marker file.

*   **File:** `~/.csp_setup_complete`
*   **Logic:** If this file exists, it signifies that the one-time setup has already been completed. The script will print a confirmation message and exit immediately, skipping all subsequent steps.

```bash
if [ -f "$HOME/.csp_setup_complete" ]; then
    echo "Project environment already set up. Skipping installation."
    exit 0
fi
```

### 2. Leiningen Initialization

This step prepares the Leiningen build tool, which is required for the Clojure-based AECS-II "Moonshot" project.

*   **Action:** The script runs the `lein version` command.
*   **Purpose:** Although the `lein` script itself is placed in the container by the `Dockerfile`, the first time it is executed, it triggers a self-installation process to download its own standalone JAR file. We run it here to ensure it's ready before the developer starts working. The output is redirected to `/dev/null` to keep the setup log clean.

```bash
echo "1. Initializing Leiningen..."
export PATH=$PATH:$HOME/bin && lein version > /dev/null
echo "Leiningen is ready."
```

### 3. Gradle Wrapper Pre-warming

This is a critical step to prevent common and frustrating issues with VS Code's Java and Gradle extensions.

*   **Problem:** The extensions can be unreliable when downloading the large Gradle distribution for the first time. This can lead to corrupted downloads, checksum failures, and file-locking timeouts.
*   **Solution:** The script forces the download in this controlled, robust environment *before* the VS Code extensions initialize.

The process includes:
1.  Making the Gradle wrapper script executable (`chmod +x ./gradlew`).
2.  Attempting to download and verify the distribution by running `./gradlew --version`.
3.  **Retry Logic:** If the download fails (e.g., due to a network hiccup leading to a corrupted file), the script will:
    *   Delete the entire download directory (`~/.gradle/wrapper/dists/`) to ensure a clean slate.
    *   Wait for 5 seconds.
    *   Retry the download.
    *   This loop will run up to 3 times before aborting the setup to prevent an infinite loop.

```bash
echo "2. Pre-warming Gradle wrapper to prevent extension download issues..."
cd "${WORKSPACE_FOLDER:-/workspaces/CSP-Modern}"

chmod +x ./gradlew

MAX_RETRIES=3
RETRY_COUNT=0
until ./gradlew --version; do
    # ... retry logic ...
done
echo "Gradle is ready."
```

### 4. Finalization

Once all setup steps are complete, the script performs final cleanup and creates the marker file.

1.  **Stop Gradle Daemon:** The pre-warming command may leave a Gradle daemon process running in the background. `./gradlew --stop` is called to terminate it, freeing up system resources.
2.  **Create Marker File:** The script creates the empty `~/.csp_setup_complete` file. This is the signal that all future container startups can skip this entire process.

```bash
# The pre-warming command may leave a Gradle daemon running. We stop it to free up resources.
./gradlew --stop

# This file signals that the setup is complete and can be skipped on subsequent rebuilds.
touch "$HOME/.csp_setup_complete"
```