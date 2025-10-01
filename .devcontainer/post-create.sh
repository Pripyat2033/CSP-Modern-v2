#!/bin/bash

# This script runs as the 'postCreateCommand' inside the dev container.
# It's responsible for installing all necessary packages and tools for the project.

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Caching Logic ---
# Check for a marker file to see if this is the first time the script is running.
# If the marker exists, we can skip the setup process, making rebuilds much faster.
if [ -f "$HOME/.csp_setup_complete" ]; then
    echo "Project environment already set up. Skipping post-create setup."
    exit 0
fi

echo "Performing first-time project setup..."

# --- Leiningen Self-Install ---
# The lein script was installed in the Dockerfile, but it needs to download its
# own JAR file the first time it's run. We also need to add its bin to the PATH.
# The Dockerfile places lein in /usr/local/bin, which is already in the PATH.
# We just need to run it once to trigger its self-install.
echo "1. Initializing Leiningen..."
lein version > /dev/null
echo "Leiningen is ready."

# --- Pre-warm Gradle Wrapper ---
# The VS Code Java/Gradle extensions can be unreliable when downloading the Gradle
# distribution. By running a simple gradlew command here, we force the download
# in a controlled environment before the extensions start. This is safe because
# "waitFor: postCreateCommand" is set.
echo "2. Pre-warming Gradle wrapper..."
chmod +x ./gradlew

MAX_RETRIES=3
RETRY_COUNT=0
until ./gradlew --version > /dev/null 2>&1; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ "$RETRY_COUNT" -ge "$MAX_RETRIES" ]; then
        echo "ERROR: Gradle wrapper failed to initialize after $MAX_RETRIES attempts. Aborting."
        exit 1
    fi
    echo "WARNING: Gradle setup failed. This can happen due to a partial download."
    echo "Cleaning up and retrying in 5 seconds... (Attempt ${RETRY_COUNT}/${MAX_RETRIES})"
    rm -rf "$HOME/.gradle/wrapper/dists/"
    sleep 5
done

echo "Gradle is ready."

# The pre-warming command may leave a Gradle daemon running. We stop it to free up
# resources and prevent conflicts with the VS Code Gradle extension.
echo "Stopping any running Gradle daemons..."
./gradlew --stop > /dev/null 2>&1 || true

# Make all our custom scripts executable so the UI buttons can call them.
chmod +x ./scripts/*.sh
chmod +x ./scripts/aecs-bridge.sh

# --- Create the marker file ---
# This file signals that the setup is complete and can be skipped on subsequent rebuilds.
touch "$HOME/.csp_setup_complete"

echo "Post-creation setup complete."