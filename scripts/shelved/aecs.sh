#!/bin/bash

# Automated Error Correction System (AECS) v1.0 - "The Engine"
# This script implements the "Bombe Philosophy" of relentless problem-solving.
# It runs in a continuous loop, attempting to build the project. When an error
# is found, it uses its knowledge base or delegates to the "Cryptographer" (Gemini)
# to find a fix, then immediately retries.

set -e

AECS_DIR=".aecs"
KB_FILE="$AECS_DIR/knowledge_base.json"
REQUEST_FILE="$AECS_DIR/request.json"
BUILD_LOG="$AECS_DIR/build.log"

# Ensure the .aecs directory exists and is clean
rm -rf "$AECS_DIR"
mkdir -p "$AECS_DIR"
touch "$KB_FILE"

echo "🔥 AECS Engine Started. Beginning build-fix-repeat cycle..."

# --- Pre-flight Check ---
# First, ensure the Gradle wrapper itself is functional. This catches
# configuration or permission errors before attempting a full build.
echo "   [AECS] Performing pre-flight check on Gradle wrapper..."
if ! ./gradlew --version > /dev/null 2>&1; then
    echo "   [AECS] ❌ CRITICAL: Gradle wrapper is not functional. Check permissions or wrapper configuration."
    exit 1
fi

if ! command -v inotifywait > /dev/null; then
    echo "   [AECS] ❌ CRITICAL: 'inotifywait' command not found."
    echo "   [AECS] Please install 'inotify-tools' package (e.g., 'sudo apt-get update && sudo apt-get install -y inotify-tools')."
    exit 1
fi

# --- Function Definitions ---

# Global variables to be set by the parser function
ISSUE_FILE_PATH=""
ISSUE_LINE_NUMBER=""
ISSUE_MESSAGE=""

parse_build_issue() {
    local issue_type=$1 # "error" or "warning"
    local log_file=$2

    # Reset global variables
    ISSUE_FILE_PATH=""
    ISSUE_LINE_NUMBER=""
    ISSUE_MESSAGE=""

    if [ "$issue_type" == "error" ]; then
        # --- Error Parsing Logic ---
        local error_line
        error_line=$(grep -m 1 -E '\[ERROR\].*\.java:[0-9]+:' "$log_file" || true)
        if [ -z "$error_line" ]; then
            error_line=$(grep -m 1 -E ':[0-9]+: error:' "$log_file" || true)
        fi
        if [ -z "$error_line" ]; then
            error_line=$(grep -m 1 -E '> Task :[a-zA-Z]+ FAILED' "$log_file" || true)
        fi

        if [ -n "$error_line" ]; then
            if [[ "$error_line" == *".java:"* ]]; then
                ISSUE_FILE_PATH=$(echo "$error_line" | sed -E 's/^\[ERROR\]\s*//' | cut -d':' -f1 | xargs)
                ISSUE_LINE_NUMBER=$(echo "$error_line" | cut -d':' -f2)
                ISSUE_MESSAGE=$(echo "$error_line" | cut -d':' -f4- | sed 's/\[.*\]//g' | xargs)
                ISSUE_IS_GENERIC=false
            else
                ISSUE_FILE_PATH="build.gradle"
                ISSUE_LINE_NUMBER=1
                ISSUE_MESSAGE=$error_line
                ISSUE_IS_GENERIC=true
            fi
        fi

    elif [ "$issue_type" == "warning" ]; then
        # --- Warning Parsing Logic ---
        local file_path_line
        local warning_line
        file_path_line=$(grep -m 1 -E '^[ /].*\.java$' "$log_file" || true)
        if [ -n "$file_path_line" ]; then
            warning_line=$(grep -A 1 -F "$file_path_line" "$log_file" | grep -m 1 -i -E '^\s+\[[Ww]arn(ing)?\]' || true)
            if [ -n "$warning_line" ]; then
                ISSUE_FILE_PATH=$(echo "$file_path_line" | xargs)
                ISSUE_LINE_NUMBER=$(echo "$warning_line" | sed -E 's/^\s+\[[Ww]arn(ing)?\]\s*//' | cut -d':' -f1)
                ISSUE_MESSAGE=$(echo "$warning_line" | sed -E 's/^\s+\[[Ww]arn(ing)?\]\s*[0-9]+:[0-9]+:\s*//' | sed 's/\s*\[.*\]$//' | xargs)
            fi
        else
            warning_line=$(grep -m 1 -i -E '(\[[Ww]arn(ing)?\]|:[0-9]+: warning:).*\.java:[0-9]+:' "$log_file" || true)
            if [ -n "$warning_line" ]; then
                ISSUE_FILE_PATH=$(echo "$warning_line" | sed -E 's/^\[[Ww]arn(ing)?\]\s*//' | cut -d':' -f1 | xargs)
                ISSUE_LINE_NUMBER=$(echo "$warning_line" | cut -d':' -f2)
                ISSUE_MESSAGE=$(echo "$warning_line" | cut -d':' -f4- | xargs)
            fi
        fi
    fi
}

# --- PHASE 1: Error-Fixing Mode ---
# This loop runs until a "BUILD SUCCESSFUL" state is achieved, fixing all blocking errors.
echo "   [AECS] Entering Phase 1: Error-Fixing Mode."
LAST_ERROR_SIGNATURE=""
TRIED_KB_SOLUTION=false

# --- 1. Build Phase ---
echo "   [AECS] Running initial build..."

# Run the build once. The inner loop will handle fixing and re-running.
while ! ./gradlew build > "$BUILD_LOG" 2>&1; do
    echo "   [AECS] Build failed. Analyzing errors..."

    # --- 2. Knowledge Invalidation ---
    # If we just tried a Knowledge Base solution and the build still failed,
    # that knowledge is "poisoned". We must invalidate it.
    if [ "$TRIED_KB_SOLUTION" = true ]; then
        echo "   [AECS] ⚠️ Knowledge Base solution failed to fix the build."
        echo "   [AECS] Invalidating poisoned knowledge for signature: $LAST_ERROR_SIGNATURE"
        jq "del(.\"$LAST_ERROR_SIGNATURE\")" "$KB_FILE" > "$KB_FILE.tmp" && mv "$KB_FILE.tmp" "$KB_FILE"
    fi
    TRIED_KB_SOLUTION=false # Reset the flag for the new loop

    # --- 3. Parse & Analyze Phase (Refactored) ---
    parse_build_issue "error" "$BUILD_LOG"

    if [ -z "$ISSUE_MESSAGE" ]; then
        echo "   [AECS] ⚠️ Could not parse a specific error line. Treating entire build log as the error."
        ISSUE_FILE_PATH="build.gradle"
        ISSUE_LINE_NUMBER=1
        ISSUE_MESSAGE=$(cat "$BUILD_LOG")
    fi

    if [ -z "$ISSUE_MESSAGE" ]; then
        echo "   [AECS] ❌ CRITICAL: Build failed, but the build log is empty. Aborting."
        exit 1
    fi

    # Create a unique signature for this error
    ERROR_SIGNATURE=$(echo "$ISSUE_FILE_PATH:$ISSUE_MESSAGE" | md5sum | cut -d' ' -f1)

    echo "   [AECS] Detected Error: \"$ISSUE_MESSAGE\""
    echo "   [AECS] Location: $ISSUE_FILE_PATH (Line $ISSUE_LINE_NUMBER)"
    echo "   [AECS] Signature: $ERROR_SIGNATURE"

    # --- 4. Knowledge Base Lookup & Loop Detection ---
    # Infinite Loop Detection: If we are seeing the exact same error signature as the last loop,
    # it means our "known solution" is poisoned. We must invalidate it and delegate.
    if [ "$ERROR_SIGNATURE" == "$LAST_ERROR_SIGNATURE" ]; then
        echo "   [AECS] ⚠️ Infinite loop detected! The known solution is poisoned."
        echo "   [AECS] Invalidating knowledge and delegating to Cryptographer for a new solution."
        jq "del(.\"$ERROR_SIGNATURE\")" "$KB_FILE" > "$KB_FILE.tmp" && mv "$KB_FILE.tmp" "$KB_FILE"
        KNOWN_SOLUTION="null" # Explicitly clear the variable to force delegation
    else
        # Only query the knowledge base if we are not in a detected loop
        KNOWN_SOLUTION=$(jq -r --arg sig "$ERROR_SIGNATURE" '.[$sig]' "$KB_FILE")
    fi

    if [ "$KNOWN_SOLUTION" != "null" ]; then
        echo "   [AECS] 🧠 Found known solution in Knowledge Base. Applying fix directly."
        # CRITICAL SAFETY CHECK: Do not apply a blank solution.
        if [ -z "$KNOWN_SOLUTION" ]; then
            echo "   [AECS] ⚠️ KB solution is blank! This is a sign of poisoned knowledge. Invalidating."
            jq "del(.\"$ERROR_SIGNATURE\")" "$KB_FILE" > "$KB_FILE.tmp" && mv "$KB_FILE.tmp" "$KB_FILE"
        else
            echo "$KNOWN_SOLUTION" | base64 -d > "$ISSUE_FILE_PATH"
        fi
        LAST_ERROR_SIGNATURE=$ERROR_SIGNATURE # Store signature to check against on the next loop
        TRIED_KB_SOLUTION=true # Set flag to indicate we used a KB solution
        # Loop immediately to retry the build
        continue
    fi

    # --- 5. Delegate to Cryptographer (Gemini) ---
    echo "   [AECS] 🤖 No known solution. Delegating to AECS Bridge for Gemini intervention."

    # Store the original file content for learning later
    ORIGINAL_CONTENT=$(base64 -w 0 < "$ISSUE_FILE_PATH")

    # Create the request file for the bridge
    jq -n \
      --arg path "$ISSUE_FILE_PATH" \
      --arg line "$ISSUE_LINE_NUMBER" \
      --arg msg "$ISSUE_MESSAGE" \
      '{filePath: $path, lineNumber: $line, errorMessage: $msg}' > "$REQUEST_FILE"

    # --- 6. The "Relentless" Loop ---
    # Call the bridge to trigger the fix, but run it in the background
    # so this script can wait for the result.
    ./scripts/aecs-bridge.sh &

    # Use inotifywait to efficiently wait for Gemini to modify the file.
    # This is far more efficient than a `sleep` loop.
    echo "   [AECS] Waiting for file modification on: $ISSUE_FILE_PATH"
    inotifywait -q -e modify "$ISSUE_FILE_PATH"
    echo "   [AECS] File modified. Re-evaluating build..."

    # After the file is modified by the bridge, we re-run the build to check the fix.
    # The result of this build will be checked at the top of the `while` loop.
    echo "   [AECS] Running build to verify fix..."
    if ./gradlew build > "$BUILD_LOG" 2>&1; then
        # If the build succeeds, learn the solution, but ONLY if the error was not generic.
        if [ "$ISSUE_IS_GENERIC" = false ]; then
            echo "   [AECS] ✅ Build successful after applying fix. Learning solution..."
            NEW_CONTENT=$(base64 -w 0 < "$ISSUE_FILE_PATH")
            jq --arg sig "$ERROR_SIGNATURE" --arg content "$NEW_CONTENT" \
               '. + {($sig): $content}' "$KB_FILE" > "$KB_FILE.tmp" && mv "$KB_FILE.tmp" "$KB_FILE"
        else
            echo "   [AECS] ✅ Build successful, but original error was generic. Skipping learning phase to prevent knowledge poisoning."
        fi
        break # Exit the loop since the build is now successful
    fi
    # If the build failed, the loop continues, and the new error will be parsed from the log we just generated.
done

# --- PHASE 2: Warning-Cleanup Mode ---
# This loop runs after all errors are fixed, focusing on eliminating warnings.
while ./gradlew build > "$BUILD_LOG" 2>&1; do
    echo "   [AECS] Running build to check for warnings..."

    # Use the refactored parsing function for warnings
    parse_build_issue "warning" "$BUILD_LOG"

    # If no warning line is found, we are done.
    if [ -z "$ISSUE_MESSAGE" ]; then
        # The build succeeded and no warnings were found. This is the "PERFECT 0" state.
        break # Exit the warning-cleanup loop
    fi

    # If we are here, the build succeeded, but we found a warning to fix.

    echo "   [AECS] Warning found. Analyzing..."

    # Create a unique signature for this warning
    WARNING_SIGNATURE=$(echo "$ISSUE_FILE_PATH:$ISSUE_MESSAGE" | md5sum | cut -d' ' -f1)

    echo "   [AECS] Detected Warning: \"$ISSUE_MESSAGE\""
    echo "   [AECS] Location: $ISSUE_FILE_PATH (Line $ISSUE_LINE_NUMBER)"
    echo "   [AECS] Signature: $WARNING_SIGNATURE"

    # Check the knowledge base for a known solution
    KNOWN_SOLUTION=$(jq -r --arg sig "$WARNING_SIGNATURE" '.[$sig]' "$KB_FILE")

    if [ "$KNOWN_SOLUTION" != "null" ]; then
        echo "   [AECS] 🧠 Found known solution in Knowledge Base. Applying fix directly."
        echo "$KNOWN_SOLUTION" | base64 -d > "$ISSUE_FILE_PATH"
        continue # Loop immediately to re-check for warnings
    fi

    # Delegate to the Cryptographer (Gemini)
    echo "   [AECS] 🤖 No known solution. Delegating to AECS Bridge for Gemini intervention."

    # Create the request file for the bridge
    jq -n \
      --arg path "$ISSUE_FILE_PATH" \
      --arg line "$ISSUE_LINE_NUMBER" \
      --arg msg "$ISSUE_MESSAGE" \
      '{filePath: $path, lineNumber: $line, errorMessage: $msg}' > "$REQUEST_FILE"

    # Call the bridge and wait for the file to be modified
    ./scripts/aecs-bridge.sh &
    echo "   [AECS] Waiting for file modification on: $ISSUE_FILE_PATH"
    inotifywait -q -e modify "$ISSUE_FILE_PATH"

    # After the fix, re-run the build to check if the specific warning is gone.
    echo "   [AECS] File modified. Re-running build to verify warning fix..."
    ./gradlew build > "$BUILD_LOG.verify" 2>&1
    if ! grep -qF "$ISSUE_MESSAGE" "$BUILD_LOG.verify"; then
        echo "   [AECS] ✅ Warning resolved after applying fix. Learning solution..."
        NEW_CONTENT=$(base64 -w 0 < "$ISSUE_FILE_PATH")

        # Add the new, successful content to the knowledge base
        jq --arg sig "$WARNING_SIGNATURE" --arg content "$NEW_CONTENT" \
           '. + {($sig): $content}' "$KB_FILE" > "$KB_FILE.tmp" && mv "$KB_FILE.tmp" "$KB_FILE"
        # The loop will continue to find the next warning.
    else
        echo "   [AECS] ⚠️ Fix did not resolve the warning. Continuing cycle..."
    fi
    rm -f "$REQUEST_FILE" "$BUILD_LOG.verify"
done

# After the loop, check the exit code of the last build attempt.
if [ $? -ne 0 ]; then
    echo "   [AECS] ❌ Build failed during warning cleanup. Restarting error-fixing cycle..."
    exec "$0" "$@" # Restart the script from the beginning.
fi

echo "✅ PERFECT 0 ACHIEVED! No errors or warnings found. AECS cycle complete."
rm -rf "$AECS_DIR" # Clean up on final success
