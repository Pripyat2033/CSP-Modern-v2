package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkCore;

/**
 * The discrimination test: is the core a <em>simulation</em> or a scripted lookup?
 *
 * <p>A lookup table (or any fitted model) can only reproduce states it was built
 * around. This test drives the live 3D solver with a state it was NEVER built
 * around — a spatially non-uniform core — and checks that the answer is
 * <em>emergent</em>, self-consistent and non-obvious:
 *
 * <ol>
 *   <li><b>Half-core void.</b> Void the right half of the channels, leave the left
 *       half wet. Nothing in the code says "voiding one side moves the flux there".
 *       If the flux nonetheless migrates to the voided side, the behaviour comes
 *       from the neutron diffusion, not from a script.</li>
 *   <li><b>Mirror symmetry.</b> Voiding the LEFT half must give the mirror image of
 *       voiding the RIGHT half (opposite centroid, same magnitude). A fitted or
 *       asymmetric model would not be mirror-consistent.</li>
 *   <li><b>Asymmetric rod pattern.</b> Withdraw the rods on one half only — again a
 *       state the reference case never had — and check the flux tilts toward the
 *       withdrawn side, with the same mirror property.</li>
 *   <li><b>The effect is quantitative, not binary.</b> The tilt must grow with the
 *       voiding asymmetry, so it is a response function, not a flag.</li>
 * </ol>
 */
public final class DiscriminationSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== DiscriminationSelfTest: simulation or script? ===");
        testHalfCoreVoid();
        testMirrorSymmetry();
        testAsymmetricRods();
        testTiltGrowsWithAsymmetry();
        System.out.printf("%nDiscriminationSelfTest: %d checks, %d failures%n",
            checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    /** Void the half-space x >= 0 (right) or x < 0 (left) at a given void fraction. */
    private static Shape halfVoid(boolean rightVoided, double voidFrac) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(RbmkSolver3D.SAFE_OMEGA);
        int ext = s.extent();
        RbmkXsec dry = RbmkXsec.fuelCell(voidFrac, 300.0);
        RbmkXsec wet = RbmkXsec.fuelCell(0.0, 300.0);
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                for (int a = 0; a < RbmkCore.AXIAL_NODES; a++) {
                    if (!s.isActive(x, z, a)) {
                        continue;
                    }
                    boolean onVoidedSide = rightVoided ? (x >= 0) : (x < 0);
                    s.setCell(x, z, a, onVoidedSide ? dry : wet);
                }
            }
        }
        s.solve(4000, 1e-8);
        return measure(s);
    }

    /** Withdraw the rods only on the half-space x >= 0 (right) or x < 0 (left). */
    private static Shape halfRod(double insertion, boolean rightInserted) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(RbmkSolver3D.SAFE_OMEGA);
        s.setCoreState(0.0, 300.0);
        int ext = s.extent();
        // Build the rod material pattern at the requested insertion, then apply it
        // only to the chosen half. The other half keeps its (wet, fuel) reference.
        RbmkSolver3D rods = new RbmkSolver3D();
        rods.setRodInsertion(insertion);
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                for (int a = 0; a < RbmkCore.AXIAL_NODES; a++) {
                    if (!s.isControl(x, z, a)) {
                        continue;
                    }
                    boolean onRodSide = rightInserted ? (x >= 0) : (x < 0);
                    if (onRodSide) {
                        s.setCell(x, z, a, rods.cellAt(x, z, a));
                    }
                }
            }
        }
        s.solve(4000, 1e-8);
        return measure(s);
    }

    /** Flux centroid in x, the L/R flux ratio, and k_eff. */
    private record Shape(double k, double centroidX, double leftOverRight) {}

    private static Shape measure(RbmkSolver3D s) {
        int ext = s.extent();
        int nx = 2 * ext + 1;
        int na = RbmkCore.AXIAL_NODES;
        double total = 0, cx = 0, left = 0, right = 0;
        for (int x = -ext; x <= ext; x++) {
            for (int z = -ext; z <= ext; z++) {
                double col = 0;
                boolean any = false;
                for (int a = 0; a < na; a++) {
                    int i = ((x + ext) * nx + (z + ext)) * na + a;
                    if (s.isActive(x, z, a)) {
                        col += s.phi1[i];
                        any = true;
                    }
                }
                if (!any) {
                    continue;
                }
                total += col;
                cx += x * col;
                if (x < 0) {
                    left += col;
                } else {
                    right += col;
                }
            }
        }
        return new Shape(s.keff(), cx / total, left / right);
    }

    // ------------------------------------------------------------------ checks

    private static void testHalfCoreVoid() {
        Shape uniform = halfVoid(false, 0.0);   // void fraction 0 both sides = uniform
        Shape rightVoid = halfVoid(true, 0.8);
        Shape leftVoid = halfVoid(false, 0.8);

        System.out.printf("  uniform    : k=%.5f centroid=%+.4f L/R=%.4f%n",
            uniform.k(), uniform.centroidX(), uniform.leftOverRight());
        System.out.printf("  right void : k=%.5f centroid=%+.4f L/R=%.4f%n",
            rightVoid.k(), rightVoid.centroidX(), rightVoid.leftOverRight());
        System.out.printf("  left  void : k=%.5f centroid=%+.4f L/R=%.4f%n",
            leftVoid.k(), leftVoid.centroidX(), leftVoid.leftOverRight());

        check("the uniform core is radially symmetric (centroid ~ 0)",
            Math.abs(uniform.centroidX()) < 1.0,
            "centroid=" + fmt(uniform.centroidX()));
        check("voiding the RIGHT half moves the flux to the right",
            rightVoid.centroidX() > 5.0,
            "centroid=" + fmt(rightVoid.centroidX()));
        check("voiding the LEFT half moves the flux to the left",
            leftVoid.centroidX() < -5.0,
            "centroid=" + fmt(leftVoid.centroidX()));
        check("the flux concentrates on the voided side (L/R falls for right void)",
            rightVoid.leftOverRight() < 0.5,
            "L/R=" + fmt(rightVoid.leftOverRight()));
        check("voiding one half raises k_eff (positive void coefficient)",
            rightVoid.k() > uniform.k() && leftVoid.k() > uniform.k(),
            "k uniform=" + fmt(uniform.k()) + " right=" + fmt(rightVoid.k())
                + " left=" + fmt(leftVoid.k()));
    }

    private static void testMirrorSymmetry() {
        Shape rightVoid = halfVoid(true, 0.8);
        Shape leftVoid = halfVoid(false, 0.8);
        check("the half-void response is mirror-symmetric (opposite centroid)",
            Math.abs(rightVoid.centroidX() + leftVoid.centroidX()) < 1.5,
            "right=" + fmt(rightVoid.centroidX()) + " left=" + fmt(leftVoid.centroidX()));
        check("the half-void k_eff is the same for either side",
            Math.abs(rightVoid.k() - leftVoid.k()) < 1e-3,
            "k right=" + fmt(rightVoid.k()) + " left=" + fmt(leftVoid.k()));
    }

    private static void testAsymmetricRods() {
        Shape rightRods = halfRod(0.9, true);
        Shape leftRods = halfRod(0.9, false);
        System.out.printf("  right rods : k=%.5f centroid=%+.4f L/R=%.4f%n",
            rightRods.k(), rightRods.centroidX(), rightRods.leftOverRight());
        System.out.printf("  left  rods : k=%.5f centroid=%+.4f L/R=%.4f%n",
            leftRods.k(), leftRods.centroidX(), leftRods.leftOverRight());

        check("inserting rods on the RIGHT half tilts the flux to the LEFT",
            rightRods.centroidX() < -2.0,
            "centroid=" + fmt(rightRods.centroidX()));
        check("inserting rods on the LEFT half tilts the flux to the RIGHT",
            leftRods.centroidX() > 2.0,
            "centroid=" + fmt(leftRods.centroidX()));
        check("the rod-tilt response is mirror-symmetric",
            Math.abs(rightRods.centroidX() + leftRods.centroidX()) < 2.0,
            "right=" + fmt(rightRods.centroidX()) + " left=" + fmt(leftRods.centroidX())
                + " (one channel of 1872 cannot be paired: 1885 disc - 1872 = 13 is odd)");
    }

    private static void testTiltGrowsWithAsymmetry() {
        double c2 = halfVoid(true, 0.2).centroidX();
        double c5 = halfVoid(true, 0.5).centroidX();
        double c8 = halfVoid(true, 0.8).centroidX();
        System.out.printf("  tilt vs void: 0.2->%+.3f  0.5->%+.3f  0.8->%+.3f%n", c2, c5, c8);
        check("the tilt grows monotonically with the voiding asymmetry",
            c2 < c5 && c5 < c8,
            "c(0.2)=" + fmt(c2) + " c(0.5)=" + fmt(c5) + " c(0.8)=" + fmt(c8));
        check("the tilt is a graded response, not a binary flag",
            c5 > 0 && c5 < c8,
            "c(0.5)=" + fmt(c5) + " c(0.8)=" + fmt(c8));
    }

    private static String fmt(double v) {
        return String.format("%.4f", v);
    }

    private static void check(String what, boolean ok, String detail) {
        checks++;
        if (ok) {
            System.out.println("  [PASS] " + what + "  (" + detail + ")");
        } else {
            failures++;
            System.out.println("  [FAIL] " + what + "  " + detail);
        }
    }
}
