# AECS v1.0 Test Plan

This document outlines the test plan for validating the `aecs.sh` script, the "Bridge Core" of our Automated Error Correction System.

## Objective

To verify that the `aecs.sh` script can correctly detect, delegate, and learn from Java compilation errors, and that it can apply learned solutions automatically from its knowledge base.

## Prerequisites

1.  The project is in a state where `./gradlew build` completes successfully.
2.  A simple Java file exists to act as a "test subject." For this plan, we will assume the use of a hypothetical `src/main/java/com/ben/csp/util/TestSubject.java`.
3.  The `.aecs/` directory does not exist or is empty before starting the test sequence.

---

## Test Case 1: The "Happy Path"

*   **Goal:** Verify the script exits cleanly when there are no errors.
*   **Setup:** Ensure the project compiles without any errors.
*   **Action:** From the project root, run `./scripts/aecs.sh`.
*   **Expected Outcome:**
    1.  The script runs `./gradlew build`.
    2.  It detects the "BUILD SUCCESSFUL" message.
    3.  It prints a success message like "✅ AECS: Build successful! All errors resolved."
    4.  The script exits with a status code of 0 and cleans up the `.aecs` directory.

---

## Test Case 2: Error Detection & Delegation (The "Bridge Path")

*   **Goal:** Verify the script can detect a new, unknown error and correctly create a request for an external fix.
*   **Setup:**
    1.  Introduce a simple, non-ambiguous syntax error into `TestSubject.java` (e.g., delete a semicolon).
*   **Action:** Run `./scripts/aecs.sh`.
*   **Expected Outcome:**
    1.  The script runs `./gradlew build` and detects the failure.
    2.  It correctly parses the error, identifying the file path, line number, and error message.
    3.  It checks the (empty) knowledge base and finds no solution.
    4.  It creates the file `.aecs/request.json` containing the error details and file content.
    5.  The script enters a waiting loop, printing a message like "Waiting for AECS Bridge to apply fix...".

---

## Test Case 3: Simulating the "Bridge" & Verifying "Learning"

*   **Goal:** Verify the script can recognize a manual fix, confirm its success, and store the solution in its knowledge base.
*   **Setup:** Continue from Test Case 2, where `aecs.sh` is actively waiting.
*   **Action:**
    1.  In a separate terminal, inspect `.aecs/request.json` to confirm its contents are correct.
    2.  Manually fix the error in `TestSubject.java`.
    3.  Manually delete the `.aecs/request.json` file to simulate the "Bridge" completing its task.
*   **Expected Outcome:**
    1.  The `aecs.sh` script detects the file deletion and retries the build.
    2.  The build succeeds.
    3.  The script recognizes the fix was successful and prints a message like "Storing solution in Knowledge Base."
    4.  It updates `.aecs/knowledge_base.json` with a new entry mapping the error's signature to the now-correct file content.
    5.  The script exits cleanly.

---

## Test Case 4: Recalling a Known Solution

*   **Goal:** Verify the script can use its knowledge base to fix a recurring error automatically.
*   **Setup:**
    1.  Continue from the successful completion of Test Case 3. The `.aecs/knowledge_base.json` file now contains a known good solution.
    2.  Re-introduce the *exact same error* into `TestSubject.java`.
*   **Action:** Run `./scripts/aecs.sh`.
*   **Expected Outcome:**
    1.  The script runs `./gradlew build` and detects the failure.
    2.  It generates the error signature and finds a match in `knowledge_base.json`.
    3.  It prints a message like "Found known solution in Knowledge Base. Applying fix directly."
    4.  It does **not** create `request.json`.
    5.  It overwrites `TestSubject.java` with the correct content from the knowledge base.
    6.  It immediately retries the build, which now succeeds.
    7.  The script exits cleanly.