package com.ben.csp.rbmk;

import java.util.ArrayList;
import java.util.List;

/**
 * The RBMK-1000 lattice at 1:4: the grid of individual channels through the
 * graphite. Each cell is one Minecraft block (250 mm), and each cell is one
 * channel — the "1,661 small reactors" the RBMK is: independent fuel channels,
 * independent coolant, coupled only through the shared neutron flux.
 *
 * <p>There are three radial zones (docs/RBMK-PLANT-SYSTEMS.md §1a):
 * <ul>
 *   <li>the <b>channel region</b> — the 12.25 m circle (49 blocks, 1,885 cells),
 *       the smallest 250 mm circle that holds the 1,872 channels;</li>
 *   <li>the <b>radial reflector</b> — the outer graphite ring out to the 14 m
 *       stack (56 blocks), no channels;</li>
 *   <li>the active (fuelled) region is 11.8 m inside the channel region.</li>
 * </ul>
 *
 * <p>The channel <em>counts</em> are exact (1,661 fuel + 211 control = 1,872,
 * second generation). The channel <em>map</em> — which exact cells hold AZ / РР /
 * АР / УСП / fuel — is approximated until a core drawing is obtained; the code
 * says so rather than hiding it.
 */
public final class RbmkLattice {

    /** One lattice cell, in block offsets from the core centre. */
    public record Cell(int x, int z) {}

    /** A channel and what it is: a fuel channel (ТК) or a control channel (СУЗ). */
    public record Channel(int x, int z, boolean control) {}

    private RbmkLattice() {}

    /** Every cell inside a circle of the given block diameter, centred on origin. */
    private static List<Cell> circle(int diameterBlocks) {
        double r = diameterBlocks / 2.0;
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

    /** The graphite stack: every cell out to 14 m (56 blocks). */
    public static List<Cell> graphiteCells() {
        return circle(RbmkGeometry.STACK_DIAMETER_BLOCKS);
    }

    /** The reflector: the graphite ring outside the channel region, no channels. */
    public static List<Cell> reflectorCells() {
        List<Cell> out = new ArrayList<>();
        double r = RbmkGeometry.CHANNEL_REGION_DIAMETER_BLOCKS / 2.0;
        for (Cell c : graphiteCells()) {
            if (c.x() * c.x() + c.z() * c.z() > r * r) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * The channels: the {@link RbmkGeometry#TOTAL_CHANNELS} cells of the channel
     * region closest to the centre, the last {@link RbmkGeometry#CONTROL_CHANNELS}
     * of them marked as control channels.
     *
     * <p>The channel region holds 1,885 cells; the 1,872 channels fill all but 13
     * of them. PLACEHOLDER PATTERN: the real control-channel map is TO VERIFY from a
     * core cartogram; until then the counts are exact and the arrangement is regular.
     */
    public static List<Channel> channels() {
        List<Cell> cs = circle(RbmkGeometry.CHANNEL_REGION_DIAMETER_BLOCKS);
        cs.sort((a, b) -> Integer.compare(a.x() * a.x() + a.z() * a.z(),
            b.x() * b.x() + b.z() * b.z()));
        int n = Math.min(cs.size(), RbmkGeometry.TOTAL_CHANNELS);
        List<Channel> out = new ArrayList<>(n);
        int controlFrom = n - Math.min(RbmkGeometry.CONTROL_CHANNELS, n);
        for (int i = 0; i < n; i++) {
            Cell c = cs.get(i);
            out.add(new Channel(c.x(), c.z(), i >= controlFrom));
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
