package com.ben.csp.rbmk.plant;

/**
 * Self-test for the {@link MachineryRegister} — the performance conformance gate.
 *
 * <p>The register exists so that a machine cannot quietly behave unlike the real
 * machine. This test checks that it actually delivers that, and — the important part
 * — that the drift-detector is not vacuous:
 *
 * <ol>
 *   <li>it loads and carries a real inventory of performance quantities;</li>
 *   <li>the machines the Director named are present — the MCP flow, the feedwater
 *       flow, the turbine;</li>
 *   <li>EVERY recorded value equals the live code constant (conformance) — this is
 *       the test that fails if someone edits {@code PlantPhysics} and the register is
 *       not updated to a new sourced value;</li>
 *   <li>the MCP flow is the real 8,000 m3/h, checked as an absolute figure;</li>
 *   <li>the drift-detector WORKS: a deliberately wrong recorded value is reported as
 *       non-conformant (so the test cannot pass by being a no-op);</li>
 *   <li>every non-placeholder item carries a source; the summary counts add up.</li>
 * </ol>
 *
 * <p>Run: {@code java -cp <out>:<gson>:src/main/resources ...plant.MachineryRegisterSelfTest}
 */
public final class MachineryRegisterSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== MachineryRegisterSelfTest ===");
        testLoads();
        testNamedMachinesPresent();
        testAllConformant();
        testMcpFlowIsReal();
        testDriftDetectorWorks();
        testSourcesAndSummary();
        System.out.printf("%nMachineryRegisterSelfTest: %d checks, %d failures%n",
            checks, failures);
        if (failures > 0) {
            throw new AssertionError(failures + " check(s) failed");
        }
    }

    /** 1. It loads, and it is a real inventory. */
    private static void testLoads() {
        check("register resource loaded", MachineryRegister.available(),
            "items=" + MachineryRegister.size());
        check("register carries a real inventory (>10 quantities)",
            MachineryRegister.size() > 10, "items=" + MachineryRegister.size());
        check("register spans many machines (>=6)",
            MachineryRegister.machines().size() >= 6,
            "machines=" + MachineryRegister.machines().size());
    }

    /** 2. The machines the Director named are present. */
    private static void testNamedMachinesPresent() {
        check("the MCP rated flow is registered", find("mcp.flow_rated") != null, "");
        check("the feedwater flow is registered", find("feedwater.flow") != null, "");
        check("the turbine rated power is registered",
            find("turbine.rated_power") != null, "");
        check("the rated thermal power is registered",
            find("plant.thermal_power") != null, "");
    }

    /**
     * 3. Conformance: every recorded value equals the live code constant. This is the
     * heart of the register — if it fails, a machine's behaviour has drifted from its
     * sourced value.
     */
    private static void testAllConformant() {
        int drifted = 0;
        StringBuilder detail = new StringBuilder();
        for (MachineryRegister.Item it : MachineryRegister.items()) {
            if (!MachineryRegister.conformant(it)) {
                drifted++;
                detail.append(String.format("%n      %s: recorded %.6g, code %.6g",
                    it.id(), it.value(), MachineryRegister.codeValue(it)));
            }
        }
        check("every machine's code value matches its recorded value (conformance)",
            drifted == 0, drifted == 0 ? "all " + MachineryRegister.size() + " conformant"
                : drifted + " drifted:" + detail);
    }

    /** 4. The MCP flow is the real 8,000 m3/h, checked as an absolute figure. */
    private static void testMcpFlowIsReal() {
        MachineryRegister.Item mcp = find("mcp.flow_rated");
        if (mcp == null) {
            check("MCP flow present", false, "missing");
            return;
        }
        double m3h = mcp.value() * 3600.0;
        check("the MCP rated flow is the real 8,000 m3/h (within 1%)",
            Math.abs(m3h - 8000.0) < 80.0, String.format("%.0f m3/h", m3h));
    }

    /**
     * 5. The drift-detector must actually work. We build an item whose recorded value
     * is deliberately WRONG (the MCP flow recorded as 999 m3/s) and assert the register
     * reports it non-conformant. If this passed as conformant, the whole gate would be
     * a no-op — exactly the "test that always passes" failure the Director fears.
     */
    private static void testDriftDetectorWorks() {
        MachineryRegister.Item bogus = new MachineryRegister.Item(
            "test.bogus", "Test machine", "A deliberately wrong quantity",
            "PlantPhysics.MCP_FLOW_RATED_M3S", 999.0, "m3/s",
            MachineryRegister.Status.TO_VERIFY, "test", "drift probe");
        check("a deliberately wrong value is reported NON-conformant",
            !MachineryRegister.conformant(bogus),
            "code=" + MachineryRegister.codeValue(bogus) + " recorded=999.0");

        MachineryRegister.Item missing = new MachineryRegister.Item(
            "test.missing", "Test machine", "A constant that does not exist",
            "PlantPhysics.NO_SUCH_CONSTANT_XYZ", 1.0, "-",
            MachineryRegister.Status.TO_VERIFY, "test", "missing probe");
        check("a named constant that does not exist is reported NON-conformant",
            !MachineryRegister.conformant(missing), "NaN expected");
    }

    /** 6. Sources present; the summary counts add up. */
    private static void testSourcesAndSummary() {
        int missing = 0;
        for (MachineryRegister.Item it : MachineryRegister.items()) {
            if (it.status() != MachineryRegister.Status.PLACEHOLDER
                && (it.source() == null || it.source().isBlank())) {
                missing++;
            }
        }
        check("every non-placeholder quantity carries a source", missing == 0,
            "missing=" + missing);

        int sum = MachineryRegister.verified().size() + MachineryRegister.toVerify().size()
            + MachineryRegister.unsourced().size();
        check("status counts add up to the item count",
            sum == MachineryRegister.size(),
            "sum=" + sum + " size=" + MachineryRegister.size());

        check("some quantities are verified against a primary source",
            !MachineryRegister.verified().isEmpty(),
            "verified=" + MachineryRegister.verified().size());
    }

    private static MachineryRegister.Item find(String id) {
        for (MachineryRegister.Item it : MachineryRegister.items()) {
            if (it.id().equals(id)) {
                return it;
            }
        }
        return null;
    }

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (ok) {
            System.out.println("  [PASS] " + name + (detail.isEmpty() ? "" : "  (" + detail + ")"));
        } else {
            failures++;
            System.out.println("  [FAIL] " + name + (detail.isEmpty() ? "" : "  (" + detail + ")"));
        }
    }
}
