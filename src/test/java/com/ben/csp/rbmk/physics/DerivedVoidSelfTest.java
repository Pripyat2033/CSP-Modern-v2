package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkGeometry;

/**
 * Self-test for the DERIVED coolant path ({@link ChannelCooling}) and its closure
 * into the core. It checks the things that make the void "derived, not fitted":
 *
 * <ol>
 *   <li>the channel energy balance reproduces the sourced outlet quality (~15%);</li>
 *   <li>the void is what the ENERGY BALANCE gives — several times the 0.05 the old
 *       fitted line assumed;</li>
 *   <li>more heat at fixed flow means more void; less flow at fixed heat means more
 *       void (the coupling the plant owns);</li>
 *   <li>the whole-core void coefficient, with the derived cells, matches the sourced
 *       INSAG-7 value at the working point;</li>
 *   <li>the core holds nominal power with the derived void closed in — it does not
 *       run away and it does not sit dead;</li>
 *   <li>halving the coolant flow raises the void AND the power — the emergent
 *       flow -> void -> reactivity loop, with no accident flag anywhere.</li>
 * </ol>
 *
 * <p>Run: {@code java -cp <out>:<gson>:src/main/resources ...DerivedVoidSelfTest}
 */
public final class DerivedVoidSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== DerivedVoidSelfTest ===");
        testOutletQuality();
        testVoidMagnitude();
        testHeatRaisesVoid();
        testFlowLowersVoid();
        testVoidCoefficientSourced();
        testCoreHoldsNominal();
        testFlowLossRaisesPower();
        testAxialCouplingIsDefaultAndHolds();
        System.out.printf("%nDerivedVoidSelfTest: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    /** 1. The energy balance must reproduce the sourced ~15% outlet quality. */
    private static void testOutletQuality() {
        ChannelCooling c = new ChannelCooling(RbmkGeometry.CORE_HEIGHT_CELLS);
        c.solve(1.0, ChannelCooling.NOMINAL_FLOW_KGS);
        double x = c.outletQuality();
        check("outlet steam quality is ~15% (sourced, WP §84)",
            x > 0.12 && x < 0.19, String.format("x=%.4f", x));
    }

    /** 2. The derived void is several times the old fitted 0.05 — this is the point. */
    private static void testVoidMagnitude() {
        ChannelCooling c = new ChannelCooling(RbmkGeometry.CORE_HEIGHT_CELLS);
        c.solve(1.0, ChannelCooling.NOMINAL_FLOW_KGS);
        double v = c.meanVoid();
        check("derived mean void is 0.25-0.45 (NOT the fitted 0.05)",
            v > 0.25 && v < 0.45, String.format("mean void=%.4f", v));
        check("void rises up the channel (steam rises)",
            c.voidFraction()[c.voidFraction().length - 1] > c.voidFraction()[0] + 0.1,
            String.format("bottom=%.3f top=%.3f", c.voidFraction()[0],
                c.voidFraction()[c.voidFraction().length - 1]));
    }

    /** 3. More power at fixed flow means more void. */
    private static void testHeatRaisesVoid() {
        ChannelCooling c = new ChannelCooling(RbmkGeometry.CORE_HEIGHT_CELLS);
        c.solve(0.5, ChannelCooling.NOMINAL_FLOW_KGS);
        double lo = c.meanVoid();
        c.solve(1.5, ChannelCooling.NOMINAL_FLOW_KGS);
        double hi = c.meanVoid();
        check("more power -> more void",
            hi > lo + 0.05, String.format("void(0.5 P)=%.4f, void(1.5 P)=%.4f", lo, hi));
    }

    /** 4. Less flow at fixed power means more void. */
    private static void testFlowLowersVoid() {
        ChannelCooling c = new ChannelCooling(RbmkGeometry.CORE_HEIGHT_CELLS);
        c.solve(1.0, ChannelCooling.NOMINAL_FLOW_KGS);
        double full = c.meanVoid();
        c.solve(1.0, ChannelCooling.NOMINAL_FLOW_KGS * 0.5);
        double half = c.meanVoid();
        check("less flow -> more void",
            half > full + 0.05, String.format("void(full flow)=%.4f, void(half flow)=%.4f",
                full, half));
    }

    /** 5. The whole-core void coefficient matches the sourced INSAG-7 value. */
    private static void testVoidCoefficientSourced() {
        double k30 = keff(0.30), k40 = keff(0.40);
        // INSAG-7 Table II-I tabulates the "void coefficient of reactivity" as
        // drho/dvoid, not dk/dvoid. With k near 1.17 the two differ by 17%, and
        // that gap is what pushed this check outside its band - it was comparing
        // dk/dvoid (+0.0314) against a drho/dvoid target (+0.020).
        double drho = (k40 - k30) / (k30 * k40) / 0.1;
        check("void coefficient is POSITIVE at the working point",
            drho > 0, String.format("drho/dvoid=%+.4f", drho));
        check("void coefficient matches INSAG-7 (+0.020) within 50%",
            drho > 0.010 && drho < 0.030, String.format("drho/dvoid=%+.4f vs +0.020", drho));
    }

    /** 6. With the derived void closed in, the core holds nominal power. */
    private static void testCoreHoldsNominal() {
        UnifiedReactorCore c = new UnifiedReactorCore();
        for (int k = 0; k < 6000; k++) {
            c.step(0.005);   // 30 s
        }
        double drift = Math.abs(c.fissionPower() - 1.0) * 100;
        check("holds nominal power with derived void (30 s drift < 5%)",
            drift < 5.0, String.format("fission=%.4f (%.1f%% off), void=%.4f",
                c.fissionPower(), drift, c.voidFraction()));
    }

    /** 7. Halving the flow raises the void AND the power — no accident flag involved. */
    private static void testFlowLossRaisesPower() {
        UnifiedReactorCore c = new UnifiedReactorCore();
        for (int k = 0; k < 2000; k++) {
            c.step(0.005);
        }
        double p0 = c.fissionPower();
        double v0 = c.voidFraction();
        c.setCoolantFlowKgS(UnifiedReactorCore.NOMINAL_FLOW_KGS * 0.5);
        for (int k = 0; k < 4000; k++) {
            c.step(0.005);
        }
        check("flow loss raises void and power (emergent, no flag)",
            c.voidFraction() > v0 + 0.05 && c.fissionPower() > p0 * 1.02,
            String.format("P %.4f->%.4f, void %.4f->%.4f",
                p0, c.fissionPower(), v0, c.voidFraction()));
    }

    /** 8. Axial coupling is the default, the void profile is bottom-heavy, and the
     *  core holds nominal with it — the bug the earlier note mis-attributed. */
    private static void testAxialCouplingIsDefaultAndHolds() {
        UnifiedReactorCore c = new UnifiedReactorCore();
        check("axial (per-node) void coupling is the default", c.axialCoupling(),
            "axialCoupling=" + c.axialCoupling());
        double[] prof = c.cooling().voidFraction();
        check("coupled void profile is bottom-heavy (heat deposited low)",
            prof[prof.length - 1] > prof[0] + 0.1,
            String.format("bottom=%.3f top=%.3f", prof[0], prof[prof.length - 1]));
        double[] shape = c.axialFluxShape();
        int peak = 0;
        for (int i = 1; i < shape.length; i++) {
            if (shape[i] > shape[peak]) {
                peak = i;
            }
        }
        check("flux is bottom-peaked (the RBMK shape the accident needs)",
            peak < shape.length / 2, "peak at node " + peak + "/" + shape.length);
        for (int k = 0; k < 6000; k++) {
            c.step(0.005);   // 30 s
        }
        double drift = Math.abs(c.fissionPower() - 1.0) * 100;
        check("holds nominal with axial coupling (30 s drift < 2%)",
            drift < 2.0, String.format("fission=%.4f (%.1f%% off), void=%.4f",
                c.fissionPower(), drift, c.voidFraction()));
    }

    private static double keff(double voidFraction) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(RbmkSolver3D.SAFE_OMEGA);
        s.setCoreState(voidFraction, 640.0);
        s.setRodInsertion(RbmkTransientSolver.NOMINAL_INSERTION);
        s.solve(2500, 1e-11);
        return s.keff();
    }

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.printf("  [%s] %s%s%n", ok ? "PASS" : "FAIL", name,
            detail.isEmpty() ? "" : "  (" + detail + ")");
    }
}
