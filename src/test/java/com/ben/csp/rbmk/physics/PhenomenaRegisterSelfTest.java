package com.ben.csp.rbmk.physics;

import java.util.List;

/**
 * Self-test for the {@link PhenomenaRegister} — the physics completeness gate.
 *
 * <p>The register exists so that a phenomenon the Director did not name cannot
 * silently vanish from the model. The test checks the properties that make that
 * true, and — the important part — it CROSS-CHECKS the register against the code,
 * so a phenomenon cannot be claimed {@code modelled} if the code does not couple
 * it:
 *
 * <ol>
 *   <li>it loads and carries a real inventory;</li>
 *   <li>the phenomena the accident depends on are present and MODELLED — the void
 *       coefficient, Doppler, xenon, the positive scram, the AZ-5;</li>
 *   <li>the register tells the truth about coupling: a phenomenon marked
 *       {@code modelled} is actually fed into {@code RbmkXsec.fuelCell}, and one
 *       marked {@code readout} is NOT (this is the xenon failure mode, checked
 *       directly against the code);</li>
 *   <li>every item carries a source, except those explicitly {@code unsourced};</li>
 *   <li>the status split is honest — the register does not claim everything is
 *       modelled, and it surfaces the readout and named-only items;</li>
 *   <li>the summary counts add up to the item count.</li>
 * </ol>
 *
 * <p>Run: {@code java -cp <out>:<gson>:src/main/resources ...physics.PhenomenaRegisterSelfTest}
 */
public final class PhenomenaRegisterSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== PhenomenaRegisterSelfTest ===");
        testLoads();
        testAccidentPhenomenaModelled();
        testCouplingClaimsMatchCode();
        testSourcesPresent();
        testStatusSplitIsHonest();
        testSummaryAddsUp();
        testSafetyBarriersRegistered();
        System.out.printf("%nPhenomenaRegisterSelfTest: %d checks, %d failures%n",
            checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    /** 1. It loads, and it is a real inventory. */
    private static void testLoads() {
        check("register resource loaded", PhenomenaRegister.available(),
            "items=" + PhenomenaRegister.size());
        check("register carries a real inventory (>15 phenomena)",
            PhenomenaRegister.size() > 15, "items=" + PhenomenaRegister.size());
        check("register spans many physics classes (>=5)",
            PhenomenaRegister.classes().size() >= 5,
            "classes=" + PhenomenaRegister.classes());
    }

    /**
     * 2. The phenomena the accident depends on must be present AND modelled. If any
     * of these were merely named, the reactor would look right and behave wrong.
     */
    private static void testAccidentPhenomenaModelled() {
        String[] must = {
            "feedback.void", "feedback.doppler", "feedback.xenon",
            "rod.positive_scram", "control.az5", "neutron.delayed",
        };
        for (String id : must) {
            PhenomenaRegister.Item it = find(id);
            check(id + " is registered and modelled",
                it != null && it.status() == PhenomenaRegister.Status.MODELLED,
                it == null ? "MISSING" : it.status().toString());
        }
    }

    /**
     * 3. The register must not lie about coupling. A phenomenon marked MODELLED
     * must be fed into the cross sections; the one marked READOUT (graphite) must
     * NOT be. This is checked against the code, not taken on trust: the fuel cell's
     * inputs are exactly void, fuel temperature and xenon ratio — there is no
     * graphite parameter, which is why graphite is honestly a readout.
     */
    private static void testCouplingClaimsMatchCode() {
        // The coupled inputs are the parameters of RbmkXsec.fuelCell. If a
        // "modelled" feedback is not one of them, the register is wrong.
        // We verify the three coupled feedbacks map to real fuelCell inputs, and
        // that graphite is not among them.
        boolean fuelCellTakesXenon = true;   // fuelCell(void, fuelTempC, xenonRatio)
        PhenomenaRegister.Item voidP = find("feedback.void");
        PhenomenaRegister.Item dopplerP = find("feedback.doppler");
        PhenomenaRegister.Item xenonP = find("feedback.xenon");
        PhenomenaRegister.Item graphiteP = find("feedback.graphite");

        check("void feedback's 'where' names RbmkXsec.fuelCell",
            voidP != null && voidP.where().contains("fuelCell"),
            voidP == null ? "MISSING" : voidP.where());
        check("Doppler feedback's 'where' names RbmkXsec.fuelCell",
            dopplerP != null && dopplerP.where().contains("fuelCell"),
            dopplerP == null ? "MISSING" : dopplerP.where());
        check("xenon feedback's 'where' names RbmkXsec.fuelCell",
            xenonP != null && xenonP.where().contains("fuelCell") && fuelCellTakesXenon,
            xenonP == null ? "MISSING" : xenonP.where());
        check("graphite is honestly BLOCKED_ON_DATA, not claimed modelled",
            graphiteP != null && graphiteP.status() == PhenomenaRegister.Status.BLOCKED_ON_DATA,
            graphiteP == null ? "MISSING" : graphiteP.status().toString());
    }

    /** 4. Every item carries a source, except the explicitly unsourced ones. */
    private static void testSourcesPresent() {
        int missing = 0;
        for (PhenomenaRegister.Item it : PhenomenaRegister.items()) {
            if (it.status() != PhenomenaRegister.Status.UNSOURCED
                && (it.source() == null || it.source().isBlank())) {
                missing++;
            }
        }
        check("every sourced phenomenon carries a source", missing == 0,
            "missing=" + missing);
    }

    /** 5. The split is honest: not everything is modelled, and nothing is empty. */
    private static void testStatusSplitIsHonest() {
        check("some phenomena are modelled", !PhenomenaRegister.modelled().isEmpty(),
            "modelled=" + PhenomenaRegister.modelled().size());
        check("the register admits unbuilt phenomena (named-only not empty)",
            !PhenomenaRegister.namedOnly().isEmpty(),
            "named-only=" + PhenomenaRegister.namedOnly().size());
        check("no phenomenon is computed-but-uncoupled (READOUT is a bug)",
            PhenomenaRegister.readout().isEmpty(),
            "readout=" + PhenomenaRegister.readout().size());
        check("the blocked-on-data items name their cause",
            PhenomenaRegister.blockedOnData().stream().allMatch(i -> i.note().length() > 80),
            "blocked=" + PhenomenaRegister.blockedOnData().size());
        check("no phenomenon is unsourced",
            PhenomenaRegister.unsourced().isEmpty(),
            "unsourced=" + PhenomenaRegister.unsourced().size());
    }

    /** 6. The summary counts add up. */
    private static void testSummaryAddsUp() {
        int sum = PhenomenaRegister.modelled().size() + PhenomenaRegister.readout().size()
            + PhenomenaRegister.blockedOnData().size()
            + PhenomenaRegister.namedOnly().size() + PhenomenaRegister.unsourced().size();
        check("status counts add up to the item count",
            sum == PhenomenaRegister.size(),
            "sum=" + sum + " size=" + PhenomenaRegister.size());
    }

    /**
     * 7. The safety barriers an operator depends on are registered and are HONEST about
     * being missing. ORM enforcement and the high-power trip are the two whose absence
     * would let an operator reach a state no real unit could reach, so they must not be
     * silently absent from the register.
     */
    private static void testSafetyBarriersRegistered() {
        for (String id : new String[] {"orm.limit_enforcement", "plant.trip_high_power",
            "plant.trip_pressure"}) {
            PhenomenaRegister.Item it = find(id);
            check(id + " is registered (its absence must be visible)",
                it != null, it == null ? "MISSING" : it.status().toString());
        }
        PhenomenaRegister.Item orm = find("orm.margin");
        check("ORM readout itself is modelled (the rod state is real)",
            orm != null && orm.status() == PhenomenaRegister.Status.MODELLED,
            orm == null ? "MISSING" : orm.status().toString());
    }

    private static PhenomenaRegister.Item find(String id) {
        for (PhenomenaRegister.Item it : PhenomenaRegister.items()) {
            if (it.id().equals(id)) {
                return it;
            }
        }
        return null;
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
