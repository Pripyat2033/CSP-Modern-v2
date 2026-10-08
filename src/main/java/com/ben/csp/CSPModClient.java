package com.ben.csp;

import net.fabricmc.api.ClientModInitializer;

/**
 * CSP-Modern Client Mod Initializer
 * Handles client-side mod initialization for Minecraft 1.20.1 compatibility
 */
public class CSPModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Client-specific initialization
        // All agent files are initialized in server mode
        // Blocks, items, and entities register in main mod initializer
    }
}
