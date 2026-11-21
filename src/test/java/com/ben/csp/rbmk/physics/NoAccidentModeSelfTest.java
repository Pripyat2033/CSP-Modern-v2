package com.ben.csp.rbmk.physics;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The reactor has NO accident mode, and this test enforces it.
 *
 * <p>At Ignalina in 1983 the operators found the power RISING when they inserted
 * the rods, under normal operation — no experiment, no special configuration. The
 * RBMK was a latent positive-feedback device: the same machine that ran at power
 * could be driven to runaway by ordinary operating decisions. There is no "blow the
 * building up" button, so the model must not have one either. A normal state and an
 * accident state are the SAME state.
 *
 * <p>What this test checks, each one a guard against encoding an accident as a mode:
 * <ol>
 *   <li><b>No mode, flag or switch.</b> A reflection scan of the core's fields finds
 *       nothing named accident / mode / experiment / blowup — there is no variable
 *       that selects a behaviour.</li>
 *   <li><b>One core, two behaviours.</b> The SAME object that holds nominal power
 *       is driven to super-critical by ordinary rod moves. Nothing is reconfigured
 *       between the two.</li>
 *   <li><b>The positive scram is a property of the rod geometry at NOMINAL.</b> With
 *       the core at nominal power, k_eff RISES as the rods first go in — the graphite
 *       displacer. It is not a feature of a special "accident flux shape".</li>
 *   <li><b>Ordinary decisions reach the runaway.</b> From nominal, the sequence
 *       withdraw -> reduce power -> xenon pit -> withdraw to the ORM limit ends
 *       super-critical, with only {@code setRodTarget} called. No flag is ever set.</li>
 * </ol>
 */
public final class NoAccidentModeSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== NoAccidentModeSelfTest ===");
        testNoModeField();
        testNoForcingMethod();
        testNoForcingVocabularyInSource();
        testSameCoreBothBehaviours();
        testPositiveScramAtNominal();
        testRunawayByOrdinaryDecisions();
        System.out.printf("%nNoAccidentModeSelfTest: %d checks, %d failures%n", checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    /** 1. No field of the core names a mode, a flag or an experiment. */
    private static void testNoModeField() {
        List<String> offenders = new ArrayList<>();
        for (Field f : UnifiedReactorCore.class.getDeclaredFields()) {
            String n = f.getName().toLowerCase();
            if (n.contains("accident") || n.contains("blowup") || n.contains("experiment")
                || n.contains("mode")) {
                offenders.add(f.getName());
            }
        }
        check("the core has no accident / mode / experiment field",
            offenders.isEmpty(), "found " + offenders);
    }

    /**
     * 2. No PUBLIC METHOD lets a caller WRITE the outcome.
     *
     * <p>This is the sharpest form of the rule. A flag is crude; a setter is worse,
     * because it looks like ordinary API. If the core exposed {@code setReactivity},
     * {@code setPower} or {@code setKeff}, a test could simply write the number it
     * wanted to see and then assert it. The reactor must be driven only through
     * things an operator can physically do — move a rod, set a regulator target, let
     * the plant change the coolant flow — and everything else must be a CONSEQUENCE
     * of the neutron balance.
     *
     * <p>So: the production core may not expose a setter whose name writes power,
     * reactivity, k_eff, fission rate or flux. The only permitted {@code set*} verbs
     * are the operator's own (rod target, regulator, setpoint, coolant flow, physics
     * timestep) and modelling-fidelity switches that do not write the answer.
     */
    private static void testNoForcingMethod() {
        List<String> offenders = new ArrayList<>();
        for (java.lang.reflect.Method m : UnifiedReactorCore.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                continue;
            }
            String n = m.getName().toLowerCase();
            if (n.contains("reactivity") || n.contains("keff") || n.contains("flux")
                || n.contains("fission") || n.contains("xenon") || n.contains("void")
                || n.contains("accident") || n.contains("blowup") || n.contains("explode")) {
                // A public method whose name is one of these is only allowed to READ.
                if (m.getParameterCount() == 0) {
                    continue;
                }
                offenders.add(m.getName() + "/" + m.getParameterCount());
            }
            // A bare "setPower(...)" is a direct write of the answer, even though the
            // legitimate "setPowerSetpoint(...)" contains the same prefix.
            if (n.equals("setpower") || n.equals("setfissionpower") || n.equals("setkeff")) {
                offenders.add(m.getName() + "/" + m.getParameterCount());
            }
        }
        check("no public method can WRITE power, reactivity, k_eff, flux or xenon",
            offenders.isEmpty(), "found " + offenders);
    }

    /**
     * 3. The production physics source carries no forcing vocabulary at all.
     *
     * <p>The field and method scans above only see the compiled core. This one reads
     * the source of every class in {@code com.ben.csp.rbmk.physics} and fails if any
     * of them contains a word that implies the accident is switched on rather than
     * allowed to happen — {@code explode}, {@code detonate}, {@code cheat},
     * {@code blowup}, {@code forceAccident}, {@code accidentMode}. Comments and
     * string literals are stripped first, so an honest explanation of the accident
     * ("this is the accident regime") does not trip it — only live code can.
     *
     * <p>If the sources cannot be found (a packaged build), the check reports that it
     * was skipped rather than silently passing.
     */
    private static void testNoForcingVocabularyInSource() {
        String[] banned = {"explode", "detonate", "blowup", "forceaccident", "accidentmode",
            "cheat", "tripwire"};
        java.io.File dir = new java.io.File("src/main/java/com/ben/csp/rbmk/physics");
        if (!dir.isDirectory()) {
            System.out.println("  [SKIP] source scan: " + dir + " not found (packaged build)");
            return;
        }
        java.io.File[] files = dir.listFiles((d, n) -> n.endsWith(".java"));
        if (files == null) {
            System.out.println("  [SKIP] source scan: could not list " + dir);
            return;
        }
        List<String> offenders = new ArrayList<>();
        for (java.io.File f : files) {
            String code = stripCommentsAndStrings(readFile(f)).toLowerCase();
            for (String w : banned) {
                if (code.contains(w)) {
                    offenders.add(f.getName() + ":" + w);
                }
            }
        }
        check("no physics source contains accident-forcing vocabulary ("
            + files.length + " files scanned)", offenders.isEmpty(), "found " + offenders);
    }

    /** Read a file as text, or "" if it cannot be read. */
    private static String readFile(java.io.File f) {
        try {
            return new String(java.nio.file.Files.readAllBytes(f.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return "";
        }
    }

    /**
     * Remove line comments, block comments and string/char literals, so a scan sees
     * only the code that actually runs. Crude but sufficient: a banned word can only
     * hide in a comment or a literal, and neither can force a reactor anywhere.
     */
    private static String stripCommentsAndStrings(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                while (i < n && s.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) {
                    i++;
                }
                i += 2;
            } else if (c == '"' || c == '\'') {
                char q = c;
                i++;
                while (i < n && s.charAt(i) != q) {
                    if (s.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i++;
                out.append(' ');
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** 4. The same object holds nominal power and is then driven super-critical. */
    private static void testSameCoreBothBehaviours() {
        UnifiedReactorCore c = new UnifiedReactorCore();
        c.setPhysicsDt(0.05);
        run(c, 0.05, 120);
        double nominalP = c.power();
        boolean heldNominal = Math.abs(nominalP - 1.0) < 0.05;
        // The SAME core, no reconfiguration: ordinary withdrawal. Track the PEAK
        // reactivity over the transient — it is super-critical while the power
        // climbs, then re-criticalises at the new, higher power (rho back to ~0).
        c.setRodTarget(0.70);
        double peakRho = c.reactivity();
        for (int k = 0; k < (int) (200 / 0.05); k++) {
            c.step(0.05);
            peakRho = Math.max(peakRho, c.reactivity());
        }
        check("one core both holds nominal AND is driven super-critical by ordinary rod moves",
            heldNominal && c.power() > nominalP * 1.2 && peakRho > 0,
            String.format("nominal P=%.4f, after withdrawal P=%.4f (peak rho %+.6f)",
                nominalP, c.power(), peakRho));
    }

    /**
     * 5. At the NOMINAL state, the derived rod worth is non-monotonic: k_eff rises
     * over the first part of insertion (the graphite displacer replacing water in
     * the bottom of the channel) before it falls. This is the positive scram, and it
     * is present with no xenon pit and no special shape.
     */
    private static void testPositiveScramAtNominal() {
        double k0 = keffAt(0.00, 0.64, 640.0, 1.0);
        double k1 = keffAt(0.20, 0.64, 640.0, 1.0);
        check("the positive scram is present at the NOMINAL state (rod geometry)",
            k1 > k0,
            String.format("k(rod 0.00)=%.5f -> k(rod 0.20)=%.5f, dk=%+.5f", k0, k1, k1 - k0));
    }

    /**
     * 6. From nominal, ordinary rod decisions alone reach the runaway. The only
     * control used is {@code setRodTarget}; no flag, no mode, no special state.
     */
    private static void testRunawayByOrdinaryDecisions() {
        UnifiedReactorCore c = new UnifiedReactorCore();
        c.setPhysicsDt(0.25);
        run(c, 0.25, 120);
        // Ordinary operating sequence, nothing special:
        c.setRodTarget(0.90);            // reduce power
        run(c, 0.25, 600);
        run(c, 0.25, 4 * 3600);          // let the xenon pit build
        c.setRodTarget(0.10);            // withdraw to the ORM limit to fight the xenon
        run(c, 0.25, 120);
        check("ordinary rod moves alone reach the low-margin, super-critical state",
            c.insertion() < 0.2 && c.reactivity() > 0,
            String.format("ins=%.3f rho=%+.6f P=%.4f Xe=%.3fx",
                c.insertion(), c.reactivity(), c.power(), c.xenonRatio()));
    }

    /** k_eff from the static solver at a given rod insertion and core state. */
    private static double keffAt(double insertion, double voidFraction, double fuelC,
                                 double xenonRatio) {
        RbmkSolver3D s = new RbmkSolver3D();
        s.setOmega(RbmkSolver3D.SAFE_OMEGA);
        s.setInnerSweeps(3);
        RbmkXsec cell = RbmkXsec.fuelCell(voidFraction, fuelC, xenonRatio);
        for (int x = -s.extent(); x <= s.extent(); x++) {
            for (int z = -s.extent(); z <= s.extent(); z++) {
                for (int a = 0; a < com.ben.csp.rbmk.RbmkCore.AXIAL_NODES; a++) {
                    if (s.isActive(x, z, a) && !s.isControl(x, z, a)) {
                        s.setCell(x, z, a, cell);
                    }
                }
            }
        }
        s.setRodInsertion(insertion);
        s.solve(2500, 1e-10);
        return s.keff();
    }

    private static void run(ReactorModel core, double dt, double seconds) {
        int n = (int) Math.round(seconds / dt);
        for (int k = 0; k < n; k++) {
            core.step(dt);
        }
    }

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (ok) {
            System.out.println("  [PASS] " + name + "  (" + detail + ")");
        } else {
            failures++;
            System.out.println("  [FAIL] " + name + "  (" + detail + ")");
        }
    }
}
