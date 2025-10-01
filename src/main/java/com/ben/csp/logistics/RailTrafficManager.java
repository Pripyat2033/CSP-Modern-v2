package com.ben.csp.logistics;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Science Grade: A central singleton that manages the state of the rail network,
 * specifically which sidings are occupied and which are free.
 */
public class RailTrafficManager {
    private static final RailTrafficManager INSTANCE = new RailTrafficManager();

    private final List<BlockPos> allSidings = Lists.newArrayList();
    private final Map<BlockPos, UUID> occupiedSidings = Maps.newHashMap();

    private RailTrafficManager() {}

    public static RailTrafficManager getInstance() {
        return INSTANCE;
    }

    /**
     * Called by RailSidingBlocks when they are placed in the world.
     * @param pos The position of the siding block.
     */
    public void registerSiding(BlockPos pos) {
        if (!allSidings.contains(pos)) {
            allSidings.add(pos);
        }
    }

    /**
     * Called by RailSidingBlocks when they are broken.
     * @param pos The position of the siding block.
     */
    public void unregisterSiding(BlockPos pos) {
        allSidings.remove(pos);
        occupiedSidings.remove(pos);
    }

    /**
     * Finds the first available, unoccupied siding.
     * @return The BlockPos of the siding, or null if none are free.
     */
    @Nullable
    public BlockPos findUnoccupiedSiding() {
        return allSidings.stream()
                .filter(pos -> !occupiedSidings.containsKey(pos))
                .findFirst()
                .orElse(null);
    }

    public void occupySiding(BlockPos pos, UUID trainId) {
        occupiedSidings.put(pos, trainId);
    }

    public void releaseSiding(BlockPos pos) {
        occupiedSidings.remove(pos);
    }
}