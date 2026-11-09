package com.ben.csp;

import net.fabricmc.api.ClientModInitializer;

// ============================================================================
// TEMPORARY STUB - SUPERSEDED by src/client/java/com/ben/csp/CSPModClient.java
//   The real client initializer lives in the client source set, which is not
//   currently compiled (build.gradle declares no source sets). This stub exists
//   only so the 'client' entrypoint in fabric.mod.json resolves at runtime.
//   It shares its fully-qualified name with the real class, so it MUST be
//   renamed/removed in the same commit that enables the client source set.
//   Ask Ben before deleting.
// ============================================================================

/**
 * CSP-Modern Client Mod Initializer
 * Handles client-side mod initialization for Minecraft 1.20.1 compatibility
 */
@Deprecated
public class CSPModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Client-specific initialization
        // All agent files are initialized in server mode
        // Blocks, items, and entities register in main mod initializer
    }
}
