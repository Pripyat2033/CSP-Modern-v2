package com.ben.csp.rbmk;

import java.util.List;

/**
 * Standalone self-test for the 1:4 RBMK core geometry and the control-rod
 * mechanism. No mocks: it exercises the real classes.
 *
 * <p>The most important checks are the ones that prove the "positive scram"
 * effect falls out of the rod geometry — because that is the accident, and a
 * core that cannot show it cannot teach it.
 */
public final class RbmkGeometrySelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        // ---- scale: metres -> blocks at 1:4 ----
        eq(47, RbmkGeometry.CORE_DIAMETER_BLOCKS, "active core is 47 blocks across");
        eq(28, RbmkGeometry.CORE_HEIGHT_BLOCKS, "active core is 28 blocks tall");
        eq(56, RbmkGeometry.STACK_DIAMETER_BLOCKS, "graphite stack is 56 blocks across");
        eq(32, RbmkGeometry.STACK_HEIGHT_BLOCKS, "graphite stack is 32 blocks tall");
        eq(49, RbmkGeometry.CHANNEL_REGION_DIAMETER_BLOCKS, "channel region is 49 blocks across");
        eq(68, RbmkGeometry.UBS_DIAMETER_BLOCKS, "UBS is 68 blocks across");
        eq(12, RbmkGeometry.UBS_THICKNESS_BLOCKS, "UBS is 12 blocks thick");
        eq(58, RbmkGeometry.LBS_DIAMETER_BLOCKS, "LBS is 58 blocks across");
        eq(8, RbmkGeometry.LBS_THICKNESS_BLOCKS, "LBS is 8 blocks thick");
        eq(39, RbmkGeometry.VESSEL_HEIGHT_BLOCKS, "vessel is 39 blocks tall");
        eq(1, RbmkGeometry.LATTICE_PITCH_BLOCKS, "one lattice cell is exactly one block");

        // ---- every graphite block is its own block; every channel its own ----
        List<RbmkLattice.Cell> graphite = RbmkLattice.graphiteCells();
        check(graphite.size() > 1000, "the graphite is a stack of individual blocks ("
            + graphite.size() + " of them), not one block");
        List<RbmkLattice.Channel> channels = RbmkLattice.channels();
        eq(1872, channels.size(), "there are 1,872 channels (1,661 fuel + 211 control)");
        eq(211, RbmkLattice.controlCount(), "there are 211 control channels");
        eq(1661, RbmkLattice.fuelCount(), "there are 1,661 fuel channels");
        // The channel region (49 blocks, 1,885 cells) is the smallest 250 mm circle
        // that holds the 1,872 channels; the 14 m stack adds the radial reflector.
        eq(1885, RbmkLattice.graphiteCells().size() - RbmkLattice.reflectorCells().size(),
            "the channel region holds 1,885 cells");
        eq(568, RbmkLattice.reflectorCells().size(),
            "the radial reflector outside it is 568 cells (graphite only)");

        // ---- control must be DISTRIBUTED, not on the rim ----
        // The single worst failure this lattice ever had: every control channel on
        // the outer ring, leaving the centre uncontrolled. These checks fail if
        // that ever comes back.
        check(RbmkLattice.nearestControlToCentre() <= 2.0,
            "there is control at the core centre (nearest control is "
            + String.format("%.2f", RbmkLattice.nearestControlToCentre()) + " blocks away)");
        check(RbmkLattice.worstCoverage() <= 4.0,
            "every channel is close to a control rod (worst distance "
            + String.format("%.2f", RbmkLattice.worstCoverage()) + " blocks)");
        // count control channels by ring, to prove they are not all outermost
        int inner = 0, outer = 0;
        double half = RbmkGeometry.CHANNEL_REGION_DIAMETER_BLOCKS / 4.0;
        for (RbmkLattice.Channel c : channels) {
            if (c.control()) {
                if (Math.hypot(c.x(), c.z()) < half) inner++; else outer++;
            }
        }
        check(inner >= 40, "a substantial share of control rods sit in the inner half ("
            + inner + " inner, " + outer + " outer)");

        // ---- control rod: withdrawn ----
        RbmkGeometry.NodeMaterial[] withdrawn = RbmkGeometry.rodMaterials(0.0);
        eqNode(RbmkGeometry.NodeMaterial.WATER, withdrawn[0], "withdrawn: water at the core bottom");
        eqNode(RbmkGeometry.NodeMaterial.WATER, withdrawn[withdrawn.length - 1],
            "withdrawn: water above the displacer (the telescope)");
        int gMid = count(withdrawn, RbmkGeometry.NodeMaterial.GRAPHITE);
        check(gMid >= 15 && gMid <= 20, "withdrawn: the 4.5 m displacer sits mid-core ("
            + gMid + " graphite nodes)");
        eq(0, count(withdrawn, RbmkGeometry.NodeMaterial.B4C),
            "withdrawn: no absorber inside the core");

        // ---- the positive scram: driving the rod in moves graphite DOWN first ----
        int bottomThird = RbmkGeometry.CORE_HEIGHT_BLOCKS / 3;
        int gBottomOut = countIn(withdrawn, RbmkGeometry.NodeMaterial.GRAPHITE, 0, bottomThird);
        RbmkGeometry.NodeMaterial[] partlyIn = RbmkGeometry.rodMaterials(0.2);
        int gBottomIn = countIn(partlyIn, RbmkGeometry.NodeMaterial.GRAPHITE, 0, bottomThird);
        check(gBottomIn > gBottomOut, "positive scram: pushing the rod in puts MORE graphite at "
            + "the core bottom (" + gBottomOut + " -> " + gBottomIn + ") - reactivity RISES");
        eq(0, countIn(partlyIn, RbmkGeometry.NodeMaterial.B4C, 0, bottomThird),
            "the absorber has not yet reached the bottom of the core");

        // ---- fully inserted: the absorber fills the core ----
        RbmkGeometry.NodeMaterial[] inserted = RbmkGeometry.rodMaterials(1.0);
        eq(RbmkGeometry.CORE_HEIGHT_BLOCKS, count(inserted, RbmkGeometry.NodeMaterial.B4C),
            "fully inserted: B4C fills the whole core");
        eq(0, count(inserted, RbmkGeometry.NodeMaterial.GRAPHITE),
            "fully inserted: the displacer is driven out of the core");

        // ---- B4C in the core only ever grows with insertion (absorber enters top) ----
        int prev = -1;
        boolean monotonic = true;
        for (int i = 0; i <= 10; i++) {
            int b = count(RbmkGeometry.rodMaterials(i / 10.0), RbmkGeometry.NodeMaterial.B4C);
            if (b < prev) {
                monotonic = false;
            }
            prev = b;
        }
        check(monotonic, "absorber in the core rises monotonically as the rod goes in");

        // ---- travel ----
        check(Math.abs(RbmkGeometry.ROD_FULL_TRAVEL_S - 17.5) < 0.2,
            "full rod travel is ~17.5 s at 0.4 m/s (" + RbmkGeometry.ROD_FULL_TRAVEL_S + " s)");
        eq(RbmkGeometry.CONTROL_CHANNELS - RbmkGeometry.RODS_FROM_BELOW, RbmkGeometry.RODS_FROM_ABOVE,
            "187 rods drive from above, 24 (USP) from below");

        System.out.println("RbmkGeometrySelfTest: " + checks + " checks passed.");
    }

    private static int count(RbmkGeometry.NodeMaterial[] a, RbmkGeometry.NodeMaterial m) {
        return countIn(a, m, 0, a.length);
    }

    private static int countIn(RbmkGeometry.NodeMaterial[] a, RbmkGeometry.NodeMaterial m,
                               int from, int to) {
        int n = 0;
        for (int i = from; i < to && i < a.length; i++) {
            if (a[i] == m) {
                n++;
            }
        }
        return n;
    }

    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) {
            throw new AssertionError(what);
        }
    }

    private static void eq(int expected, int actual, String what) {
        check(expected == actual, what + ": expected <" + expected + "> but was <" + actual + ">");
    }

    private static void eqNode(RbmkGeometry.NodeMaterial expected, RbmkGeometry.NodeMaterial actual,
                               String what) {
        check(expected == actual, what + ": expected <" + expected + "> but was <" + actual + ">");
    }
}
