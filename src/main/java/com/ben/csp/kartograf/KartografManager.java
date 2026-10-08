package com.ben.csp.kartograf;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.jetbrains.annotations.Nullable;

public class KartografManager extends PersistentState {
    private static final String NAME = "csp_kartograf";
    @Nullable
    private BlockBox lastScanBounds;

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // NBT saving logic would go here
        return nbt;
    }

    public static KartografManager fromNbt(NbtCompound nbt) {
        // NBT loading logic would go here
        return new KartografManager();
    }

    public static KartografManager get(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        PersistentStateManager stateManager = world.getPersistentStateManager();
        return stateManager.getOrCreate(KartografManager::fromNbt, KartografManager::new, NAME);
    }

    @Nullable
    public BlockBox getLastScanBounds() {
        return lastScanBounds;
    }
}