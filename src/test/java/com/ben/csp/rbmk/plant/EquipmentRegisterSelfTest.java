package com.ben.csp.rbmk.plant;

/**
 * Self-test for the {@link EquipmentRegister} — the completeness gate.
 *
 * <p>The register exists so that an item the Director did not name cannot
 * silently vanish from the build. The test checks the properties that make that
 * true, not just that the file parses:
 *
 * <ol>
 *   <li>the register loads and carries a real inventory (not an empty list);</li>
 *   <li>the non-obvious items the Director worried about are present — the
 *       ventilation fans and the САОР (ECCS) accumulators — with sources;</li>
 *   <li>every item carries a source, EXCEPT the ones explicitly marked
 *       {@code unsourced} (which must be visible as such, not hidden);</li>
 *   <li>the statuses are a real split — some built, some named-only — because a
 *       register that claims everything is "modelled" would be lying;</li>
 *   <li>the summary counts add up to the item count.</li>
 * </ol>
 *
 * <p>Run: {@code java -cp <out>:<gson>:src/main/resources ...plant.EquipmentRegisterSelfTest}
 */
public final class EquipmentRegisterSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== EquipmentRegisterSelfTest ===");
        testLoads();
        testNonObviousItemsPresent();
        testSourcesPresent();
        testStatusSplitIsHonest();
        testSummaryAddsUp();
        System.out.printf("%nEquipmentRegisterSelfTest: %d checks, %d failures%n",
            checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    /** 1. It loads, and it is a real inventory. */
    private static void testLoads() {
        check("register resource loaded", EquipmentRegister.available(),
            "items=" + EquipmentRegister.size());
        check("register carries a real inventory (>50 items)",
            EquipmentRegister.size() > 50, "items=" + EquipmentRegister.size());
        check("register spans many systems (>=10)",
            EquipmentRegister.systems().size() >= 10,
            "systems=" + EquipmentRegister.systems().size());
    }

    /**
     * 2. The items the Director specifically worried about are present. If the
     * ventilation fans or the САОР tanks were missing, this is exactly the silent
     * omission the register exists to catch.
     */
    private static void testNonObviousItemsPresent() {
        check("ventilation fans are registered",
            hasIdPrefix("vent.") || hasNameContaining("ventilator"),
            "ventilation entries=" + countWhere(it -> it.system().equals("Ventilation")));
        check("САОР / ECCS accumulators are registered",
            hasIdPrefix("eccs."),
            "eccs entries=" + countWhere(it -> it.system().startsWith("ECCS")));
        check("cranes are registered",
            hasIdPrefix("crane."),
            "crane entries=" + countWhere(it -> it.system().equals("Handling")));
        check("diesel generators are registered",
            hasIdPrefix("elec.diesel"),
            "diesel entries=" + countWhere(it -> it.id().equals("elec.diesel")));
        check("reactor gas circuit is registered",
            hasIdPrefix("gas."), "gas entries=" + countWhere(it -> it.system().equals("Gas circuit")));
    }

    /** 3. Every item is sourced, except the ones openly marked unsourced. */
    private static void testSourcesPresent() {
        long missing = EquipmentRegister.items().stream()
            .filter(it -> it.status() != EquipmentRegister.Status.UNSOURCED)
            .filter(it -> it.source() == null || it.source().isBlank())
            .count();
        check("every non-unsourced item carries a source", missing == 0,
            "items with no source and not marked unsourced=" + missing);
        // The unsourced list is a finding, not a failure — but it must be visible.
        System.out.println("  · " + EquipmentRegister.summary());
        for (EquipmentRegister.Item it : EquipmentRegister.unsourced()) {
            System.out.println("  · UNSOURCED: " + it.describe());
        }
    }

    /** 4. The status split is honest — not everything is claimed as modelled. */
    private static void testStatusSplitIsHonest() {
        check("some equipment is genuinely modelled",
            EquipmentRegister.modelled().size() > 0,
            "modelled=" + EquipmentRegister.modelled().size());
        check("the register does NOT claim everything is modelled",
            EquipmentRegister.namedOnly().size() > 0,
            "named-only=" + EquipmentRegister.namedOnly().size());
    }

    /** 5. The summary counts add up. */
    private static void testSummaryAddsUp() {
        int total = EquipmentRegister.modelled().size()
            + EquipmentRegister.specified().size()
            + EquipmentRegister.namedOnly().size()
            + EquipmentRegister.unsourced().size();
        check("status counts add up to the item count",
            total == EquipmentRegister.size(),
            String.format("counted=%d, size=%d", total, EquipmentRegister.size()));
    }

    private static boolean hasIdPrefix(String p) {
        return countWhere(it -> it.id().startsWith(p)) > 0;
    }

    private static boolean hasNameContaining(String s) {
        String low = s.toLowerCase();
        return countWhere(it -> it.nameEn().toLowerCase().contains(low)) > 0;
    }

    private static long countWhere(java.util.function.Predicate<EquipmentRegister.Item> p) {
        return EquipmentRegister.items().stream().filter(p).count();
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
