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

# --- PHASE 1: Error-Fixing Mode ---
# This loop runs until a "BUILD SUCCESSFUL" state is achieved, fixing all blocking errors.
echo "   [AECS] Entering Phase 1: Error-Fixing Mode."
LAST_ERROR_SIGNATURE=""
TRIED_KB_SOLUTION=false
while true; do
    # --- 1. Build Phase ---
    echo "   [AECS] Running build..."    
    # Redirect both stdout and stderr to the build log to capture all output
    if ./gradlew build > "$BUILD_LOG" 2>&1; then
        echo "   [AECS] ✅ Build successful. All errors resolved."
        echo "   [AECS] Transitioning to Phase 2: Warning-Cleanup Mode."
        break # Exit the error-fixing loop and proceed to the warning-cleanup loop
    fi

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

    # --- 3. Parse & Analyze Phase ---
    # Find the first error line in the log. This now handles multiple common formats, in order of preference:
    # 1. Checkstyle/PMD/SpotBugs error: [ERROR] /path/to/File.java:123: Specific message
    # 2. Standard Java compiler error: /path/to/File.java:123: error: message
    # 3. Generic Gradle task failure: > Task :... FAILED (as a fallback)
    ERROR_LINE=$(grep -m 1 -E '\[ERROR\].*\.java:[0-9]+:' "$BUILD_LOG")
    if [ -z "$ERROR_LINE" ]; then
        ERROR_LINE=$(grep -m 1 -E ':[0-9]+: error:' "$BUILD_LOG" || grep -m 1 -E '> Task :[a-zA-Z]+ FAILED' "$BUILD_LOG")
    fi

    if [ -n "$ERROR_LINE" ]; then # Check if ERROR_LINE is not empty
        # A structured error was found. Extract details.
        if [[ "$ERROR_LINE" == *".java:"* ]]; then
            # For Java or Checkstyle errors, the file path is the first part. For Checkstyle, it's prefixed with [ERROR].
            FILE_PATH=$(echo "$ERROR_LINE" | sed -E 's/^\[ERROR\]\s*//' | cut -d':' -f1 | xargs) # xargs trims whitespace
            LINE_NUMBER=$(echo "$ERROR_LINE" | cut -d':' -f2)
            # The message is everything after the line and column numbers.
            ERROR_MESSAGE=$(echo "$ERROR_LINE" | cut -d':' -f4- | sed 's/\[.*\]//g' | xargs)
        else
            # For generic Gradle errors, the whole line is the message.
            FILE_PATH="build.gradle" # Assume the build script is the most likely culprit
            LINE_NUMBER=1
            ERROR_MESSAGE=$ERROR_LINE
        fi
    else
        echo "   [AECS] ⚠️ Could not parse a specific error line. Treating entire build log as the error."
        # Fallback for unstructured errors (e.g., deep Gradle configuration issues)
        FILE_PATH="build.gradle" # Assume the build script is the most likely culprit
        LINE_NUMBER=1
        ERROR_MESSAGE=$(cat "$BUILD_LOG")
    fi

        # If the build log itself is empty, we can't proceed.
        if [ -z "$ERROR_MESSAGE" ]; then
            echo "   [AECS] ❌ CRITICAL: Build failed, but the build log is empty. Aborting."
            exit 1
        fi

    # Create a unique signature for this error
    ERROR_SIGNATURE=$(echo "$FILE_PATH:$ERROR_MESSAGE" | md5sum | cut -d' ' -f1)

    echo "   [AECS] Detected Error: \"$ERROR_MESSAGE\""
    echo "   [AECS] Location: $FILE_PATH (Line $LINE_NUMBER)"
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
        echo "$KNOWN_SOLUTION" | base64 -d > "$FILE_PATH"
        LAST_ERROR_SIGNATURE=$ERROR_SIGNATURE # Store signature to check against on the next loop
        TRIED_KB_SOLUTION=true # Set flag to indicate we used a KB solution
        # Loop immediately to retry the build
        continue
    fi

    # --- 5. Delegate to Cryptographer (Gemini) ---
    echo "   [AECS] 🤖 No known solution. Delegating to AECS Bridge for Gemini intervention."

    # Store the original file content for learning later
    ORIGINAL_CONTENT=$(base64 -w 0 < "$FILE_PATH")

    # Create the request file for the bridge
    jq -n \
      --arg path "$FILE_PATH" \
      --arg line "$LINE_NUMBER" \
      --arg msg "$ERROR_MESSAGE" \
      '{filePath: $path, lineNumber: $line, errorMessage: $msg}' > "$REQUEST_FILE"

    # --- 6. The "Relentless" Loop ---
    # Call the bridge to trigger the fix, but run it in the background
    # so this script can wait for the result.
    ./scripts/aecs-bridge.sh &

    # Use inotifywait to efficiently wait for Gemini to modify the file.
    # This is far more efficient than a `sleep` loop.
    echo "   [AECS] Waiting for file modification on: $FILE_PATH"
    inotifywait -q -e modify "$FILE_PATH"

    echo "   [AECS] File modified. Re-evaluating build..."

    # --- 7. Learning Phase ---
    # After the file is modified, we re-run the build. If it succeeds, we learn.
    if ./gradlew build > "$BUILD_LOG" 2>&1; then
        echo "   [AECS] ✅ Build successful after applying fix. Learning solution..."
        NEW_CONTENT=$(base64 -w 0 < "$FILE_PATH")

        # Add the new, successful content to the knowledge base
        jq --arg sig "$ERROR_SIGNATURE" --arg content "$NEW_CONTENT" \
           '. + {($sig): $content}' "$KB_FILE" > "$KB_FILE.tmp" && mv "$KB_FILE.tmp" "$KB_FILE"

        echo "   [AECS] ✅ BUILD SUCCESSFUL! AECS cycle complete."
        rm -rf "$AECS_DIR" # Clean up
        exit 0
    else
        # The fix didn't work. The loop will continue, and the new error will be parsed.
        echo "   [AECS] ⚠️ Fix did not resolve the build. Continuing cycle with new error..."
        # Clean up the request file so we don't get stuck on it
        LAST_ERROR_SIGNATURE=$ERROR_SIGNATURE # Store the signature to detect a loop on the next iteration.
        rm -f "$REQUEST_FILE"
    fi
done

# --- PHASE 2: Warning-Cleanup Mode ---
# This loop runs after all errors are fixed, focusing on eliminating warnings.
while true; do
    echo "   [AECS] Running build to check for warnings..."
    ./gradlew build > "$BUILD_LOG" 2>&1

    # Find the first warning line in the log.
    WARNING_LINE=$(grep -m 1 -E ':[0-9]+: warning:' "$BUILD_LOG")

    if [ -z "$WARNING_LINE" ]; then
        echo "✅ PERFECT 0 ACHIEVED! No errors or warnings found. AECS cycle complete."
        rm -rf "$AECS_DIR" # Clean up on final success
        exit 0
    fi

    echo "   [AECS] Warning found. Analyzing..."

    # Extract details from the warning line
    FILE_PATH=$(echo "$WARNING_LINE" | cut -d':' -f1)
    LINE_NUMBER=$(echo "$WARNING_LINE" | cut -d':' -f2)
    WARNING_MESSAGE=$(echo "$WARNING_LINE" | cut -d':' -f4- | sed 's/\[.*\]//g' | xargs)

    # Create a unique signature for this warning
    WARNING_SIGNATURE=$(echo "$FILE_PATH:$WARNING_MESSAGE" | md5sum | cut -d' ' -f1)

    echo "   [AECS] Detected Warning: \"$WARNING_MESSAGE\""
    echo "   [AECS] Location: $FILE_PATH (Line $LINE_NUMBER)"
    echo "   [AECS] Signature: $WARNING_SIGNATURE"

    # Check the knowledge base for a known solution
    KNOWN_SOLUTION=$(jq -r --arg sig "$WARNING_SIGNATURE" '.[$sig]' "$KB_FILE")

    if [ "$KNOWN_SOLUTION" != "null" ]; then
        echo "   [AECS] 🧠 Found known solution in Knowledge Base. Applying fix directly."
        echo "$KNOWN_SOLUTION" | base64 -d > "$FILE_PATH"
        continue # Loop immediately to re-check for warnings
    fi

    # Delegate to the Cryptographer (Gemini)
    echo "   [AECS] 🤖 No known solution. Delegating to AECS Bridge for Gemini intervention."

    # Create the request file for the bridge
    jq -n \
      --arg path "$FILE_PATH" \
      --arg line "$LINE_NUMBER" \
      --arg msg "$WARNING_MESSAGE" \
      '{filePath: $path, lineNumber: $line, errorMessage: $msg}' > "$REQUEST_FILE"

    # Call the bridge and wait for the file to be modified
    ./scripts/aecs-bridge.sh &
    echo "   [AECS] Waiting for file modification on: $FILE_PATH"
    inotifywait -q -e modify "$FILE_PATH"

    echo "   [AECS] File modified. Re-evaluating warnings..."

    # After the file is modified, we re-run the build. If the warning is gone, we learn.
    ./gradlew build > "$BUILD_LOG" 2>&1
    if ! grep -q "$WARNING_MESSAGE" "$BUILD_LOG"; then
        echo "   [AECS] ✅ Warning resolved after applying fix. Learning solution..."
        NEW_CONTENT=$(base64 -w 0 < "$FILE_PATH")

        # Add the new, successful content to the knowledge base
        jq --arg sig "$WARNING_SIGNATURE" --arg content "$NEW_CONTENT" \
           '. + {($sig): $content}' "$KB_FILE" > "$KB_FILE.tmp" && mv "$KB_FILE.tmp" "$KB_FILE"
    else
        echo "   [AECS] ⚠️ Fix did not resolve the warning. Continuing cycle..."
        rm -f "$REQUEST_FILE"
    fi
done
