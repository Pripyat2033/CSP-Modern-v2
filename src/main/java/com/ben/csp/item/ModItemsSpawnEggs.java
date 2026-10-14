package com.ben.csp.item;

import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Spawn egg registration for CSP-Modern entities.
 * 
 * Note: Requires Fabric mixin handling for getMainArm().
 * Currently disabled due to compile-time type incompatibility with abstract entities.
 * Enable after implementing proper entity implementations.
 */
public class ModItemsSpawnEggs {
    /**
     * Spawn egg registration for Tolkach - currently commented out awaiting mixin implementation.
     * Used for developer testing and debugging spawn mechanics once getMainArm() is available.
     */
    public static final Identifier TOLKACH_SPAWN_EGG_ID = Identifier.of("csp-modern", "tolkach_spawn_egg");
    
    /**
     * Tolkach spawn egg metadata for easy reference during development.
     * Used for registry lookup and Skala system integration.
     */
    private static final String TOLKACH_GREEN_COLOR_CODE = "0x00ff00";
    private static final String TOLKACH_BLACK_COLOR_CODE = "0x003300";

    /**
     * Register all spawn eggs for the mod (deprecated - use parameterized version).
     * For backwards compatibility with existing callers.
     */
    @Deprecated
    public static void registerModSpawnEggs() {
        // Spawn egg registration commented out - enable after fixing entity mixins for getMainArm()
        System.out.println("[CSP-Modern] Spawn egg registration disabled - awaiting getMainArm() mixin implementation");
    }
    
    /**
     * Register all spawn eggs for the mod.
     * 
     * @param registries Registry system for item registration
     * @param id The mod ID string prefix  
     */
    public static void registerModSpawnEggs(Registry<net.minecraft.item.Item> registries, String id) {
        // Spawn egg registration commented out - enable after fixing entity mixins for getMainArm()
        System.out.println("[CSP-Modern] Spawn egg registration disabled - awaiting getMainArm() mixin implementation");
    }
    
    /**
     * Check if spawn egg registration is available.
     * Returns false until getMainArm() mixin is properly implemented.
     * Used for conditional initialization and Skala system compatibility checks.
     * 
     * @return true if spawn eggs can be registered, false otherwise
     */
    public static boolean isSpawnEggRegistrationAvailable() {
        // Returns false until getMainArm() mixin is properly implemented
        return false;
    }
    
    /**
     * Get the Tolkach spawn egg identifier.
     * Used for registry lookup and Skala system integration.
     * 
     * @return The Identifier for the Tolkach spawn egg
     */
    public static Identifier getTolkachSpawnEggId() {
        return TOLKACH_SPAWN_EGG_ID;
    }
}

/**
 * Helper class for spawn egg color codes - used during development and Skala integration.
 */
@Environment(EnvType.CLIENT)
class SpawnEggColorHelper {
    /**
     * Tolkach green color code used in spawn egg item.
     */
    public static final String GREEN_COLOR_CODE = "0x00ff00";
    
    /**
     * Tolkach black color code used in spawn egg item.
     */
    public static final String BLACK_COLOR_CODE = "0x003300";
}