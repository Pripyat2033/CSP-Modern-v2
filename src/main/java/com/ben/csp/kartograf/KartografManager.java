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
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("minX", box.getMinX());
        nbt.putInt("minY", box.getMinY());
        nbt.putInt("minZ", box.getMinZ());
        nbt.putInt("maxX", box.getMaxX());
        nbt.putInt("maxY", box.getMaxY());
        nbt.putInt("maxZ", box.getMaxZ());
        return nbt;
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
        String[] keys = {"minX", "minY", "minZ", "maxX", "maxY", "maxZ"};
        for (String key : keys) {
            if (!nbt.contains(key)) {
                return null;
            }
        }
        return new BlockBox(
            nbt.getInt("minX"), nbt.getInt("minY"), nbt.getInt("minZ"),
            nbt.getInt("maxX"), nbt.getInt("maxY"), nbt.getInt("maxZ"));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (lastScanBounds != null) {
            nbt.put("lastScanBounds", serializeBlockBox(lastScanBounds));
        }
        return nbt;
    }

    public static KartografManager fromNbt(NbtCompound nbt) {
        KartografManager manager = new KartografManager();
        if (nbt.contains("lastScanBounds")) {
            manager.lastScanBounds = deserializeBlockBox(nbt.getCompound("lastScanBounds"));
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
