package com.ben.csp.rbmk.plant;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Validates a machine's shell: every cell the {@link MachineSpec} says is casing
 * must really be that casing in the world. This is what makes the machine a
 * multiblock and not a decorated cube — the machine is "formed" only when the
 * real structure is present.
 *
 * <p>The controller cell may hold the controller block instead of the casing, so
 * the shell the builder places (casing everywhere) still validates.
 */
public final class MachineValidator {

    private MachineValidator() {}

    /** The result of a validation: formed or not, and how much is missing. */
    public static final class Result {
        public final boolean formed;
        public final long missing;
        public final long required;

        Result(boolean formed, long missing, long required) {
            this.formed = formed;
            this.missing = missing;
            this.required = required;
        }

        public String describe() {
            if (formed) {
                return "formed";
            }
            return String.format("incomplete — %d of %d shell blocks missing",
                missing, required);
        }
    }

    public static Result validate(World world, MachineSpec spec, BlockPos min,
                                  BlockPos controller) {
        long required = 0;
        long missing = 0;
        for (int dx = 0; dx < spec.sizeX; dx++) {
            for (int dy = 0; dy < spec.sizeY; dy++) {
                for (int dz = 0; dz < spec.sizeZ; dz++) {
                    if (!spec.isShell(dx, dy, dz)) {
                        continue;
                    }
                    required++;
                    BlockPos p = min.add(dx, dy, dz);
                    BlockState st = world.getBlockState(p);
                    if (p.equals(controller)) {
                        continue; // the controller stands in for one casing cell
                    }
                    if (st.getBlock() != spec.casing) {
                        missing++;
                    }
                }
            }
        }
        return new Result(missing == 0, missing, required);
    }

    /** The number of casing blocks the shell needs (the controller counts as one). */
    public static long shellCount(MachineSpec spec) {
        long n = 0;
        for (int dx = 0; dx < spec.sizeX; dx++) {
            for (int dy = 0; dy < spec.sizeY; dy++) {
                for (int dz = 0; dz < spec.sizeZ; dz++) {
                    if (spec.isShell(dx, dy, dz)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }
}
