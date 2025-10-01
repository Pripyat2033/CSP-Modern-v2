# Use the official universal dev container image as a base.
FROM mcr.microsoft.com/devcontainers/universal:2-linux

# Switch to root user to install system packages
USER root

# Install necessary packages and clean up to keep the image size down.
# - inotify-tools: For the aecs-bridge.sh script to efficiently watch for file changes.
# - leiningen: The build tool for Clojure, needed for the AECS-II "Moonshot".
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends inotify-tools wget \
    # Download and install Leiningen
    && wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -O /usr/local/bin/lein \
    && chmod +x /usr/local/bin/lein \
    # Clean up apt cache to reduce image size
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*