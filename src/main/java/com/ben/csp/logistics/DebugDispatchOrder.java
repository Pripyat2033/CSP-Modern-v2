package com.ben.csp.logistics;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;

/**
 * A temporary, controlled replacement for DispatchOrder to break the debugging cycle.
 * This class has a clear, unambiguous API that we can code against.
 */
public class DebugDispatchOrder {
    private final UUID taskId;
    private final Entity source;
    private final BlockPos destination;
    private final Map<String, Integer> materials;

    public DebugDispatchOrder(UUID taskId, Entity source, BlockPos destination, Map<String, Integer> materials) {
        this.taskId = taskId;
        this.source = source;
        this.destination = destination;
        this.materials = materials;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public Entity getSource() {
        return source;
    }

    public BlockPos getDestination() {
        return destination;
    }

    public Map<String, Integer> materials() { return materials; }
}