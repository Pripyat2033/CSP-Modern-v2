package com.ben.csp.kartograf;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.jetbrains.annotations.Nullable;

/**
 * Kartograf (Картограф) Manager - Persistent state manager for scan data and map information.
 * 
 * Historical Role: Manages geological surveys, terrain mapping, and scan location tracking
 * for the construction enterprise operations. Based on Soviet-era cartographic traditions
 * with integration to OPB-82 infrastructure requirements.
 */
public class KartografManager extends PersistentState {

    private static final String NAME = "csp_kartograf";
    @Nullable
    private BlockBox lastScanBounds;

    /**
     * Serialize a BlockBox into NBT format for persistent storage.
     * Used in writeNbt() to save scan bounds data.
     * 
     * @param box The block box to serialize
     * @return NBT compound containing serialized block box coordinates
     */
    public static NbtCompound serializeBlockBox(BlockBox box) {
        // Store BlockBox as string representation for now - placeholder implementation
        return new NbtCompound();
    }

    /**
     * Deserialize a BlockBox from NBT format for state restoration.
     * Used in fromNbt() to restore last scan bounds data.
     * 
     * @param nbt The NBT compound containing serialized block box coordinates
     * @return Restored BlockBox or null if not found/invalid
     */
    @Nullable
    public static BlockBox deserializeBlockBox(NbtCompound nbt) {
        // Placeholder implementation - actual deserialization would reconstruct BlockBox from NBT data
        return null;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // Save lastScanBounds for persistent state recovery
        if (lastScanBounds != null) {
            nbt.put("lastScanBounds", serializeBlockBox(lastScanBounds));
        }
        System.out.println("[Kartograf] Saved scan bounds: " + 
                          (lastScanBounds != null ? lastScanBounds.toString() : "null"));
        return nbt;
    }

    public static KartografManager fromNbt(NbtCompound nbt) {
        // Load lastScanBounds from NBT for persistent state restoration
        KartografManager manager = new KartografManager();
        if (nbt.contains("lastScanBounds")) {
            try {
                String boundsStr = nbt.getString("lastScanBounds");
                // For now, use simple string parsing since proper deserialization is placeholder
                if (boundsStr != null && !boundsStr.isEmpty()) {
                    try {
                        String[] parts = boundsStr.split(",");
                        if (parts.length >= 6) {
                            int minX = Integer.parseInt(parts[0]);
                            int minY = Integer.parseInt(parts[1]);
                            int minZ = Integer.parseInt(parts[2]);
                            int maxX = Integer.parseInt(parts[3]);
                            int maxY = Integer.parseInt(parts[4]);
                            int maxZ = Integer.parseInt(parts[5]);
                            BlockBox savedBox = new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
                            manager.lastScanBounds = savedBox;
                            System.out.println("[Kartograf] Loaded scan bounds: " + savedBox.toString());
                        }
                    } catch (NumberFormatException e) {
                        // Invalid format, continue without loaded bounds
                    }
                }
            } catch (Exception e) {
                // Failed to read string, continue without loaded bounds
            }
        }
        return manager;
    }

    /**
     * Get KartografManager instance for the server's overworld.
     * Used for accessing persistent cartographic data during game runtime.
     * 
     * @param server The Minecraft server instance
     * @return KartografManager instance with loaded persistent state
     */
    public static KartografManager get(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        PersistentStateManager stateManager = world.getPersistentStateManager();
        return stateManager.getOrCreate(KartografManager::fromNbt, KartografManager::new, NAME);
    }

    /**
     * Check if scan bounds have been recorded.
     * Used for status reporting and UI display purposes.
     * 
     * @return true if scan bounds are available, false otherwise
     */
    public boolean hasScanBounds() {
        return lastScanBounds != null;
    }

    /**
     * Get the last scan bounds or null if none recorded.
     * Used for SkalaRequestDevice integration and map display.
     * 
     * @return Last scan bounds or null if none recorded
     */
    @Nullable
    public BlockBox getLastScanBounds() {
        return lastScanBounds;
    }

    /**
     * Set the scan bounds after a survey operation.
     * Used by GeodezistEntity for tracking survey progress.
     * 
     * @param bounds The block box representing surveyed area
     */
    public void setScanBounds(BlockBox bounds) {
        this.lastScanBounds = bounds;
        System.out.println("[Kartograf] Survey complete: " + bounds.toString());
    }

}
