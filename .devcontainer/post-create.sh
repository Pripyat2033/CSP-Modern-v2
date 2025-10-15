#!/bin/bash

# This script runs as the 'postCreateCommand' inside the dev container.
# It's responsible for one-time project setup tasks.

# Exit immediately if a command exits with a non-zero status.
set -e

echo "Performing post-creation setup..."

# --- Leiningen Self-Install ---
# The lein script was installed in the Dockerfile, but it needs to download its
# own JAR file the first time it's run. We also need to add its bin to the PATH.
echo "1. Initializing Leiningen (for AECS-II)..."
lein version > /dev/null
echo "Leiningen is ready."

# --- Fix Script Line Endings ---
# Ensure all shell scripts have Unix-style line endings (LF) instead of
# Windows-style (CRLF), which can cause "bad interpreter" errors.
echo "2. Normalizing script line endings..."
apt-get update && apt-get install -y dos2unix
find ./scripts -name "*.sh" -exec dos2unix {} \;

# Make all our custom scripts executable so the UI buttons can call them.
chmod +x ./scripts/*.sh

echo "Post-creation setup complete."