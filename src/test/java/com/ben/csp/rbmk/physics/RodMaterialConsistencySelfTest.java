package com.ben.csp.rbmk.physics;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Self-test for two rule-2 (nothing guessed) fixes:
 *
 * <ol>
 *   <li><b>The live and static solvers build the SAME control-rod materials.</b>
 *       {@link RbmkTransientSolver#setRodInsertion} used the hand-set
 *       {@code RbmkXsec.GRAPHITE/B4C}; {@link RbmkSolver3D#setRodInsertion} used the
 *       DERIVED table. The live B4C cell was ~80x weaker than the one the reference
 *       state was seeded with, so rod worth / positive scram / AZ-5 ran on hand-set
 *       numbers while derived data shipped. This test fails if they ever diverge.</li>
 *   <li><b>The divergence guard catches a huge FINITE k_eff.</b> The old guard only
 *       flagged non-finite or non-positive flux sums, so a blown-up-but-finite
 *       eigenvalue slipped through as "not diverged".</li>
 * </ol>
 */
public final class RodMaterialConsistencySelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== RodMaterialConsistencySelfTest ===");
        System.out.printf("  lattice table source = %s%n", LatticeTable.source());
        testControlMaterialsMatch();
        testDerivedB4cIsStrong();
        testDivergenceCeiling();
        System.out.printf("%nRodMaterialConsistencySelfTest: %d checks, %d failures%n",
            checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    /** The two solvers must put byte-identical cross sections in the control nodes. */
    private static void testControlMaterialsMatch() {
        double insertion = 0.5;
        RbmkTransientSolver live = new RbmkTransientSolver();
        live.setRodInsertion(insertion);
        RbmkSolver3D stat = new RbmkSolver3D();
        stat.setRodInsertion(insertion);

        int ext = live.extent();
        int[] seenByMaterial = new int[3];   // graphite, water, B4C
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                for (int a = 0; a < com.ben.csp.rbmk.RbmkCore.AXIAL_NODES; a++) {
                    if (!live.isControl(x, z, a)) {
                        continue;
                    }
                    RbmkXsec l = live.cellAt(x, z, a);
                    RbmkXsec s = stat.cellAt(x, z, a);
                    String tag = materialTag(l);
                    if (tag.equals("graphite")) {
                        seenByMaterial[0]++;
                    } else if (tag.equals("water")) {
                        seenByMaterial[1]++;
                    } else if (tag.equals("b4c")) {
                        seenByMaterial[2]++;
                    }
                    if (!same(l, s)) {
                        check("live rod cell == static rod cell at (" + x + "," + z + ","
                            + a + ") [" + tag + "]", false,
                            "live sa2=" + l.sa2() + " static sa2=" + s.sa2());
                        return;
                    }
                }
            }
        }
        check("live and static solvers build identical control cells (all nodes)",
            true, "");
        check("all three rod materials are present at insertion 0.5",
            seenByMaterial[0] > 0 && seenByMaterial[1] > 0 && seenByMaterial[2] > 0,
            "graphite=" + seenByMaterial[0] + " water=" + seenByMaterial[1]
                + " b4c=" + seenByMaterial[2]);
    }

    /** The derived B4C cell must be far stronger than the hand-set one (the bug). */
    private static void testDerivedB4cIsStrong() {
        RbmkXsec derived = RbmkXsec.controlCell(RbmkXsec.controlB4C());
        RbmkXsec handSet = RbmkXsec.controlCell(RbmkXsec.B4C);
        check("derived B4C control cell is much stronger than the hand-set one",
            derived.sa2() > 10.0 * handSet.sa2(),
            "derived sa2=" + derived.sa2() + " handset sa2=" + handSet.sa2());
    }

    /** A huge but finite k_eff must flag divergence; a benign one must not. */
    private static void testDivergenceCeiling() throws Exception {
        Field kf = RbmkSolver3D.class.getDeclaredField("keff");
        kf.setAccessible(true);

        RbmkSolver3D hot = new RbmkSolver3D();
        hot.setCoreState(0.0, 300.0);
        hot.setRodInsertion(0.5);
        Arrays.fill(hot.phi1, 1.0);
        Arrays.fill(hot.phi2, 1.0);
        kf.setDouble(hot, 1e100);
        hot.stepOnce();
        check("a huge FINITE k_eff flags divergence", hot.diverged(),
            "keff=" + hot.keff() + " diverged=" + hot.diverged());

        RbmkSolver3D calm = new RbmkSolver3D();
        calm.setCoreState(0.0, 300.0);
        calm.setRodInsertion(0.5);
        Arrays.fill(calm.phi1, 1.0);
        Arrays.fill(calm.phi2, 1.0);
        kf.setDouble(calm, 1.0);
        calm.stepOnce();
        check("a benign k_eff does NOT flag divergence", !calm.diverged(),
            "keff=" + calm.keff() + " diverged=" + calm.diverged());
        check("the ceiling is well above any physical transient",
            RbmkSolver3D.KEFF_CEILING > 2.0, "ceiling=" + RbmkSolver3D.KEFF_CEILING);
    }

    private static String materialTag(RbmkXsec c) {
        // Classify by the cell the node material actually builds: a displacer node is
        // Ø74 mm, an absorber node Ø70 mm, an empty node is water filling the bore.
        if (same(c, RbmkXsec.controlCell(com.ben.csp.rbmk.RbmkGeometry.NodeMaterial.B4C))) {
            return "b4c";
        }
        if (same(c, RbmkXsec.controlCell(com.ben.csp.rbmk.RbmkGeometry.NodeMaterial.GRAPHITE))) {
            return "graphite";
        }
        return "water";
    }

    private static boolean same(RbmkXsec a, RbmkXsec b) {
        return a.d1() == b.d1() && a.sr1() == b.sr1() && a.nsf1() == b.nsf1()
            && a.d2() == b.d2() && a.sa2() == b.sa2() && a.nsf2() == b.nsf2()
            && a.ss12() == b.ss12() && a.ss21() == b.ss21();
    }

    private static void check(String what, boolean ok, String detail) {
        checks++;
        if (ok) {
            System.out.println("  [PASS] " + what + (detail.isEmpty() ? "" : " (" + detail + ")"));
        } else {
            failures++;
            System.out.println("  [FAIL] " + what + "  " + detail);
        }
    }
}
