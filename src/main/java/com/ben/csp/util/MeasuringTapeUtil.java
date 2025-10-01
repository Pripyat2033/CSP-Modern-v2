package com.ben.csp.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MeasuringTapeUtil {

    private MeasuringTapeUtil() {
        // This is a utility class and is not meant to be instantiated.
    }

    /**
     * Simulates stretching a measuring tape in a straight line along one axis.
     * Checks every block along the path for obstructions.
     * @return true if the path is clear, false if it is obstructed.
     */
    public static boolean isAxisAlignedLineClear(World world, BlockPos start, BlockPos end) {
        // This simplified check works for axis-aligned lines, which is what the edges of our bounding box are.
        for (BlockPos pos : BlockPos.iterate(start, end)) {
            // We don't need to check the start position itself, but iterate includes it.
            // A real check would be more complex, but for this simulation, we assume if the
            // block is not replaceable, it's an obstruction.
            if (!pos.equals(start) && !world.getBlockState(pos).isReplaceable()) {
                return false;
            }
        }
        return true;
    }
}