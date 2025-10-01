package com.ben.csp.qc;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

public class EnvironmentalStressManager {
    private static final EnvironmentalStressManager INSTANCE = new EnvironmentalStressManager();

    private static final double WEATHERING_STRESS_FACTOR = 0.00001; // Extra wear for exposed blocks

    private EnvironmentalStressManager() {}

    public static EnvironmentalStressManager getInstance() {
        return INSTANCE;
    }

    public double getEnvironmentalStress(ServerWorld world, BlockPos pos) {
        double stress = 0.0;

        // Weathering: Is the block exposed to the sky during rain?
        if (world.isRaining() && world.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY()) {
            stress += WEATHERING_STRESS_FACTOR;
        }

        return stress;
    }
}