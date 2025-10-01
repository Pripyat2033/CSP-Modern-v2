#!/bin/bash

# AECS Bridge - Connects the AECS Engine to the VS Code "Cryptographer" (Gemini)
# This script reads the error request, opens the problematic file, and
# programmatically invokes the Gemini "Fix This" command.

set -e

REQUEST_FILE="/workspaces/CSP-Modern/.aecs/request.json"

if [ ! -f "$REQUEST_FILE" ]; then
    echo "AECS Bridge: No request file found. Nothing to do."
    exit 0
fi

# Extract file path and line number from the request JSON
FILE_PATH=$(jq -r '.filePath' "$REQUEST_FILE")
LINE_NUMBER=$(jq -r '.lineNumber' "$REQUEST_FILE")

echo "AECS Bridge: Received request for $FILE_PATH at line $LINE_NUMBER"

# Use the VS Code CLI to orchestrate the fix
# 1. Open the file and go to the specific line and column.
# 2. Execute the Gemini "Fix This" command.
#    Note: This command is context-aware and will use the error information
#    from the Problems panel that VS Code automatically populates.
code --goto "${FILE_PATH}:${LINE_NUMBER}" && code --command "google.cloudcode.gemini.fixThis"

echo "AECS Bridge: Delegated fix to Gemini. The engine will now re-evaluate."

# The 'request.json' file is intentionally NOT deleted here. The main aecs.sh
# script will detect the file has been modified and will re-run the build.
# If the build is successful, aecs.sh will then clean up the file.