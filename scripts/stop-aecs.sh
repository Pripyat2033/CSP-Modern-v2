#!/bin/bash
#
# Stops all running AECS processes and cleans the workspace.

echo "AECS: Received stop signal. Terminating all processes..."

# Use pkill to find and kill the scripts by name.
# The -f flag matches against the full command line.
# The || true ensures the command doesn't fail if no processes are found.
pkill -f 'aecs.sh|aecs-bridge.sh' || true

# Clean up the .aecs directory
rm -rf /workspaces/CSP-Modern/.aecs

echo "AECS: All processes terminated and workspace cleaned."