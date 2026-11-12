package com.ben.csp.rbmk;

import java.util.HashSet;
import java.util.Set;

/**
 * A set of (x, z) cells to skip when filling a radial region — the channel
 * penetrations through a structural disc.
 *
 * <p>This exists because the upper biological shield ("Схема Е") is <b>not</b> a
 * solid lid: every one of the 1,872 channels passes through it (each channel has
 * its own plug), and the rod drives and steam lines pass through too. The shield
 * is therefore built as the disc <em>minus</em> the channel cells. The same
 * subtraction has to be readable in a test, so the logic lives here, free of any
 * Minecraft type.
 */
public final class HoleMask {

    private final Set<Long> holes;

    /** Build a mask from parallel arrays of x and z (either may be null = empty). */
    public HoleMask(int[] xs, int[] zs) {
        Set<Long> h = new HashSet<>();
        if (xs != null) {
            for (int i = 0; i < xs.length; i++) {
                h.add(key(xs[i], zs[i]));
            }
        }
        this.holes = h;
    }

    /** Pack an (x, z) cell into one key. */
    public static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    public boolean isHole(int x, int z) {
        return holes.contains(key(x, z));
    }

    public int size() {
        return holes.size();
    }

    /**
     * How many cells a disc (innerRadius 0) or annulus of the given outer radius
     * holds after the holes are removed, times the number of y-layers. This is the
     * count the builder places, computed without touching the world.
     */
    public long countDisc(int cx, int cz, int innerRadius, int outerRadius, int layers) {
        long n = 0;
        long rIn2 = (long) innerRadius * innerRadius;
        long rOut2 = (long) outerRadius * outerRadius;
        for (int x = cx - outerRadius; x <= cx + outerRadius; x++) {
            for (int z = cz - outerRadius; z <= cz + outerRadius; z++) {
                long d2 = (long) (x - cx) * (x - cx) + (long) (z - cz) * (z - cz);
                if (d2 < rIn2 || d2 > rOut2) {
                    continue;
                }
                if (isHole(x, z)) {
                    continue;
                }
                n++;
            }
        }
        return n * layers;
    }
}
