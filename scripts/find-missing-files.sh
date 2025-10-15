#!/bin/bash

# This script scans the project for file path references and reports any that are "dangling"
# (i.e., the referenced file does not actually exist). This is useful for cleaning up
# configuration files, scripts, and source code that point to old or deleted files.

set -e

PROJECT_ROOT=$(git rev-parse --show-toplevel)
cd "$PROJECT_ROOT"

echo "🔍 Starting scan for missing file references in project..."

# Use grep to find potential file paths in all tracked files.
# The regex looks for strings that look like paths (e.g., 'path/to/file.ext').
# We exclude directories that contain generated or temporary content.
grep -r -E --exclude-dir={.git,.gradle,build,run,.devcontainer,node_modules} --exclude="*.{jar,zip,class,png,jpg,bin}" "([a-zA-Z0-9_.-]+/)+[a-zA-Z0-9_.-]+" . | \
while IFS=: read -r file_with_ref matched_line; do
    # Extract the potential file path from the matched line.
    # This is a bit tricky as grep gives us the whole line. We'll re-grep the line.
    potential_path=$(echo "$matched_line" | grep -o -E "([a-zA-Z0-9_.-]+/)+[a-zA-Z0-9_.-]+")

    # Iterate over each potential path found in the line
    while IFS= read -r path; do
        # Check if the path is a file, a directory, or doesn't exist.
        # We only care about things that don't exist at all.
        if [ ! -e "$path" ]; then
            # Before reporting, filter out some common false positives.
            # - Java package names (e.g., com/ben/csp/util)
            # - URLs or Gradle dependency strings
            if [[ "$path" != *'/'* || $(basename "$path") != *.* ]]; then
                continue # Skip things that don't look like full file paths
            fi
            echo "-----------------------------------------------------------------"
            echo "❗️ Missing File Detected:"
            echo "   - Referenced in: $file_with_ref"
            echo "   - Missing Path:    $path"
        fi
    done <<< "$potential_path"
done

echo "✅ Scan complete."