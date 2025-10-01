package com.ben.csp.logistics;

import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import java.util.HashMap;

import java.util.Map;
import java.util.UUID;

/**
 * A definitive, standard class representing a single logistics order for a truck.
 * This class is the single source of truth for dispatch orders, created to resolve
 * persistent build issues.
 */
public final class DispatchOrder {
    private final UUID taskId;
    private final BlockPos source;
    private final BlockPos destination;
    private final Map<String, Integer> materials;

    public DispatchOrder(@Nullable UUID taskId, BlockPos source, BlockPos destination, Map<String, Integer> materials) {
        this.taskId = taskId;
        this.source = source;
        this.destination = destination;
        this.materials = materials;
    }

    @Nullable public UUID getTaskId() { return taskId; }
    public BlockPos getSource() { return source; }
    public BlockPos getDestination() { return destination; }
    public Map<String, Integer> materials() { return materials; }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        if (this.taskId != null) {
            nbt.putUuid("TaskId", this.taskId);
        }
        nbt.put("Source", NbtHelper.fromBlockPos(this.source));
        nbt.put("Destination", NbtHelper.fromBlockPos(this.destination));
        NbtCompound materialsNbt = new NbtCompound();
        this.materials.forEach((item, count) -> materialsNbt.putInt(item, count));
        nbt.put("Materials", materialsNbt);
        return nbt;
    }

    public static DispatchOrder fromNbt(NbtCompound nbt) {
        UUID taskId = nbt.containsUuid("TaskId") ? nbt.getUuid("TaskId") : null;
        BlockPos source = NbtHelper.toBlockPos(nbt.getCompound("Source"));
        BlockPos destination = NbtHelper.toBlockPos(nbt.getCompound("Destination"));
        NbtCompound materialsNbt = nbt.getCompound("Materials");
        Map<String, Integer> materials = new HashMap<>();
        for (String key : materialsNbt.getKeys()) {
            materials.put(key, materialsNbt.getInt(key));
        }
        return new DispatchOrder(taskId, source, destination, materials);
    }
}