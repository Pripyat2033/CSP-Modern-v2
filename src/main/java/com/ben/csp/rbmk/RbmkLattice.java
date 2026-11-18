package com.ben.csp.rbmk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The RBMK-1000 lattice at 1:4: the grid of individual channels through the
 * graphite. Each cell is one Minecraft block (250 mm), and each cell is one
 * channel — the "1,661 small reactors" the RBMK is: independent fuel channels,
 * independent coolant, coupled only through the shared neutron flux.
 *
 * <p>There are three radial zones:
 * <ul>
 *   <li>the <b>channel region</b> — the 12.25 m circle (49 blocks, 1,885 cells),
 *       the smallest 250 mm circle that holds the 1,872 channels;</li>
 *   <li>the <b>radial reflector</b> — the outer graphite ring out to the 14 m
 *       stack (56 blocks), no channels;</li>
 *   <li>the active (fuelled) region is 11.8 m inside the channel region.</li>
 * </ul>
 *
 * <p>The channel <em>counts</em> are exact (1,661 fuel + 211 control = 1,872,
 * second generation). The control channels are <b>distributed through the core</b>
 * on a regular sub-lattice — about one control per eight fuel, reaching the
 * centre and covering the whole core, as the real reactor's control rods do. The
 * exact cartogram (which cells are AZ / РР / АР / УСП) is still TO VERIFY from a
 * primary drawing; the code says so rather than hiding it. What is <em>not</em>
 * a placeholder is the requirement that control be spread through the core: a
 * core whose rods sit only on the rim cannot be controlled, and the geometry
 * test now fails if that ever happens again.
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

    private static double radius(Cell c) {
        return Math.sqrt(c.x() * c.x() + c.z() * c.z());
    }

    /** The graphite stack: every cell out to 14 m (56 blocks). */
    public static List<Cell> graphiteCells() {
        return circle(RbmkGeometry.STACK_DIAMETER_CELLS);
    }

    /** The reflector: the graphite ring outside the channel region, no channels. */
    public static List<Cell> reflectorCells() {
        List<Cell> out = new ArrayList<>();
        double r = RbmkGeometry.CHANNEL_REGION_DIAMETER_CELLS / 2.0;
        for (Cell c : graphiteCells()) {
            if (c.x() * c.x() + c.z() * c.z() > r * r) {
                out.add(c);
            }
        }
        return out;
    }

    /** Spacing of the control sub-lattice, in lattice pitches. */
    private static final int CONTROL_PITCH = 3;

    /**
     * The channels: 1,872 of the channel-region cells, split into 1,661 fuel and
     * 211 control.
     *
     * <p>The control channels sit on a sub-lattice every {@value #CONTROL_PITCH}
     * cells — roughly one control per nine, about the RBMK's control-to-fuel
     * ratio — which places control <b>through the centre and across the whole
     * core</b>, as a controllable reactor requires. Fuel fills the remaining
     * inner cells; the outermost 13 cells of the region carry no channel.
     */
    public static List<Channel> channels() {
        List<Cell> region = circle(RbmkGeometry.CHANNEL_REGION_DIAMETER_CELLS);
        region.sort((a, b) -> Double.compare(radius(a), radius(b)));

        // 1. control cells: the distributed sub-lattice
        Set<Long> control = new HashSet<>();
        for (Cell c : region) {
            if (Math.floorMod(c.x(), CONTROL_PITCH) == 0
                && Math.floorMod(c.z(), CONTROL_PITCH) == 0) {
                control.add(key(c));
            }
        }

        // 2. make the control count exactly right, without breaking the spread:
        //    add the innermost spare cells, or drop the outermost controls
        int want = RbmkGeometry.CONTROL_CHANNELS;
        if (control.size() < want) {
            for (Cell c : region) { // region is centre-first
                if (control.size() >= want) {
                    break;
                }
                control.add(key(c));
            }
        } else if (control.size() > want) {
            List<Cell> drop = new ArrayList<>();
            for (Cell c : region) {
                if (control.contains(key(c))) {
                    drop.add(c);
                }
            }
            drop.sort((a, b) -> Double.compare(radius(b), radius(a))); // outermost first

            // Drop whole SYMMETRY ORBITS, never a single cell. The pitch-3 base set
            // is symmetric under x->-x and z->-z, but the count is 213, two over
            // 211. Removing cells one at a time by radius breaks that symmetry: the
            // tie at r=24.19 drops both x=-24 rods and leaves x=+24 unmatched. A
            // one-sided absorber pattern is not the RBMK, and against the reflective
            // radial boundary it excites a near-degenerate tilt mode, so the 3D
            // solver settles on a flux tilted ~12x to one side. Dropping an orbit at
            // a time (a cell together with its mirrors) keeps the pattern symmetric;
            // the only orbit that fits 213->211 is a 2-cell on-axis pair such as
            // {(-24,0),(24,0)} — the 4-cell corner orbits would overshoot.
            Set<Long> dropped = new HashSet<>();
            for (Cell c : drop) {
                if (control.size() <= want) {
                    break;
                }
                if (dropped.contains(key(c))) {
                    continue;
                }
                List<Cell> orbit = new ArrayList<>();
                for (Cell o : new Cell[]{c, new Cell(-c.x(), c.z()),
                        new Cell(c.x(), -c.z()), new Cell(-c.x(), -c.z())}) {
                    if (control.contains(key(o)) && !dropped.contains(key(o))
                        && !orbit.contains(o)) {
                        orbit.add(o);
                    }
                }
                if (control.size() - orbit.size() < want) {
                    continue; // this orbit would overshoot; try a smaller one
                }
                for (Cell o : orbit) {
                    control.remove(key(o));
                    dropped.add(key(o));
                }
            }
            // Fallback: if no orbit fit exactly, finish one cell at a time so the
            // count is always right (the symmetry is best-effort beyond this point).
            for (Cell c : drop) {
                if (control.size() <= want) {
                    break;
                }
                if (control.remove(key(c))) {
                    dropped.add(key(c));
                }
            }
        }

        // 3. fuel: the inner non-control cells, up to the fuel count. The region
        //    is a disc of 1885 cells and the core has 1872 channels, so 13 of the
        //    non-control cells are spare. Drop them from the outside in, but in
        //    whole x-mirror ORBITS, not one at a time: dropping by radius alone
        //    broke the symmetry (it left (-23,-8) and (-8,-23) with no mirror —
        //    the same failure mode as the control count-fit, and the cause of the
        //    ~4% residual tilt in DiscriminationSelfTest). 13 is ODD and no rim
        //    cell lies on the x=0 axis, so a fully symmetric drop (always an even
        //    number of cells) is impossible — exactly ONE cell must stay unpaired.
        //    This keeps the pattern symmetric down to that last unavoidable cell.
        Set<Long> spareKeys = new HashSet<>();
        List<Cell> spare = new ArrayList<>();
        for (Cell c : region) {
            if (!control.contains(key(c))) {
                spare.add(c);
                spareKeys.add(key(c));
            }
        }
        spare.sort((a, b) -> Double.compare(radius(b), radius(a))); // outermost first
        int need = spare.size() - RbmkGeometry.FUEL_CHANNELS;
        Set<Long> noChannel = new HashSet<>();
        for (Cell c : spare) {
            if (noChannel.size() >= need) {
                break;
            }
            if (noChannel.contains(key(c))) {
                continue;
            }
            Cell mirror = new Cell(-c.x(), c.z());
            if (!mirror.equals(c) && spareKeys.contains(key(mirror))
                && noChannel.size() + 2 <= need) {
                noChannel.add(key(c));
                noChannel.add(key(mirror));
            } else {
                noChannel.add(key(c));
            }
        }

        List<Channel> out = new ArrayList<>(RbmkGeometry.TOTAL_CHANNELS);
        for (Cell c : region) {
            boolean isControl = control.contains(key(c));
            if (!isControl && noChannel.contains(key(c))) {
                continue; // the outermost spare cells carry no channel
            }
            out.add(new Channel(c.x(), c.z(), isControl));
        }
        return out;
    }

    private static long key(Cell c) {
        return ((long) c.x() << 32) ^ (c.z() & 0xffffffffL);
    }

    public static int fuelCount() {
        return (int) channels().stream().filter(c -> !c.control()).count();
    }

    public static int controlCount() {
        return (int) channels().stream().filter(Channel::control).count();
    }

    /** Distance from the core centre to the nearest control channel, in blocks. */
    public static double nearestControlToCentre() {
        double best = Double.MAX_VALUE;
        for (Channel c : channels()) {
            if (c.control()) {
                best = Math.min(best, Math.hypot(c.x(), c.z()));
            }
        }
        return best;
    }

    /** The worst case, over all channels, of the distance to the nearest control. */
    public static double worstCoverage() {
        List<Channel> chs = channels();
        double worst = 0;
        for (Channel a : chs) {
            double best = Double.MAX_VALUE;
            for (Channel b : chs) {
                if (b.control()) {
                    best = Math.min(best, Math.hypot(a.x() - b.x(), a.z() - b.z()));
                }
            }
            worst = Math.max(worst, best);
        }
        return worst;
    }
}
