package com.ben.csp.rbmk;

import java.util.ArrayList;
import java.util.List;

/**
 * The RBMK-1000 lattice at 1:4: the grid of individual channels through the
 * graphite. Each cell is one Minecraft block, and each cell is one channel —
 * this is the "1,661 small reactors" the RBMK is: independent fuel channels,
 * independent coolant, coupled only through the shared neutron flux.
 *
 * <p>The graphite is not one block either: it is a stack of 250 mm blocks, one
 * per cell, each pierced by its own channel. {@link #graphiteCells()} is that
 * stack; {@link #channels()} is the pierced cells.
 */
public final class RbmkLattice {

    /** One lattice cell, in block offsets from the core centre. */
    public record Cell(int x, int z) {}

    /** A channel and what it is: a fuel channel (ТК) or a control channel (СУЗ). */
    public record Channel(int x, int z, boolean control) {}

    private RbmkLattice() {}

    /**
     * Every lattice cell inside the active core circle, at 250 mm pitch (one cell
     * per block). The radius is the stated active diameter; the count this yields
     * is the honest geometric count for that circle, which the geometry note
     * compares against the recorded 1,872 channels (they do not exactly agree —
     * see docs/RBMK-CORE-GEOMETRY.md §1a; the recorded figure is kept as the
     * target and the discrepancy is reported, not hidden).
     */
    public static List<Cell> cells() {
        double r = RbmkGeometry.CORE_DIAMETER_BLOCKS / 2.0;
        List<Cell> out = new ArrayList<>();
        int lim = (int) Math.ceil(r);
        for (int x = -lim; x <= lim; x++) {
            for (int z = -lim; z <= lim; z++) {
                if (x * x + z * z <= r * r) {
                    out.add(new Cell(x, z));
                }
            }
        }
        return out;
    }

    /** Every graphite block in the core: one per lattice cell. */
    public static List<Cell> graphiteCells() {
        return cells();
    }

    /**
     * The channels: {@code min(cells, totalChannels)} cells, the closest to the
     * centre, the last {@link RbmkGeometry#CONTROL_CHANNELS} of them marked as
     * control channels.
     *
     * <p>PLACEHOLDER PATTERN: the real control-channel map (which cells hold AZ /
     * РР / АР / УСП rods) is TO VERIFY from a core drawing; until then this marks a
     * contiguous set so the code has the right <em>count</em> and the right
     * <em>coupling</em>, which is what the performance test needs.
     */
    public static List<Channel> channels() {
        List<Cell> cs = cells();
        cs.sort((a, b) -> Integer.compare(a.x * a.x + a.z * a.z, b.x * b.x + b.z * b.z));
        int n = Math.min(cs.size(), RbmkGeometry.TOTAL_CHANNELS);
        List<Channel> out = new ArrayList<>(n);
        int controlFrom = n - Math.min(RbmkGeometry.CONTROL_CHANNELS, n);
        for (int i = 0; i < n; i++) {
            Cell c = cs.get(i);
            out.add(new Channel(c.x, c.z, i >= controlFrom));
        }
        return out;
    }

    public static int fuelCount() {
        return (int) channels().stream().filter(c -> !c.control()).count();
    }

    public static int controlCount() {
        return (int) channels().stream().filter(Channel::control).count();
    }
}
