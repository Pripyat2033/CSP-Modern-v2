package com.ben.csp.agent;

import com.ben.csp.agent.SkalaCodeRegistry.OperationCode;

/**
 * Standalone self-test for {@link SkalaCodeRegistry}: registration, lookup, the
 * seeded SKALA functions, and the rules that keep the registry honest (no blank
 * codes, unknown codes answer null, re-registration replaces).
 */
public final class SkalaCodeRegistrySelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        SkalaCodeRegistry reg = new SkalaCodeRegistry();

        // the registry is seeded with the standard pre-1986 SKALA functions it must know
        check(reg.isKnown("PRIZMA"), "PRIZMA is seeded");
        check(reg.isKnown("DREG"), "DREG is seeded");
        // DIIS is a 1990s upgrade and must NOT be seeded: the pre-1986 computer
        // had no DIIS (Chornobyl Family SKALA restoration).
        check(!reg.isKnown("DIIS"), "DIIS is not seeded — it is a 1990s upgrade");
        check(reg.getOperationDescription("PRIZMA") != null
            && !reg.getOperationDescription("PRIZMA").isBlank(),
            "PRIZMA has a description");
        check(reg.size() >= 2, "the registry is not empty");

        // an unknown code is honestly unknown, not a fabricated answer
        check(!reg.isKnown("NOPE-9999"), "an unknown code is not known");
        eq(null, reg.getOperationDescription("NOPE-9999"), "an unknown code answers null");
        eq(null, reg.get("NOPE-9999"), "get() of an unknown code is null");

        // registering a code makes it findable
        int before = reg.size();
        reg.registerOperationCode("TEST-1", "a test operation", 5, false);
        check(reg.isKnown("TEST-1"), "a registered code is known");
        eq("a test operation", reg.getOperationDescription("TEST-1"),
            "the description round-trips");
        eq(before + 1, reg.size(), "the registry grew by one");

        // re-registering replaces, and returns what was there before
        OperationCode prev = reg.registerOperationCode("TEST-1", "a test operation v2", 9, true);
        check(prev != null, "re-registration returns the previous entry");
        eq("a test operation", prev.description, "the returned entry is the old one");
        eq("a test operation v2", reg.getOperationDescription("TEST-1"),
            "the description was replaced");
        check(reg.isCritical("TEST-1"), "the critical flag was updated");
        eq(before + 1, reg.size(), "re-registration does not grow the registry");

        // a blank code or blank description is refused, not silently stored
        check(threw(() -> reg.registerOperationCode("  ", "x", 1, false)),
            "a blank code is refused");
        check(threw(() -> reg.registerOperationCode("TEST-2", "", 1, false)),
            "a blank description is refused");
        check(threw(() -> reg.registerOperationCode("TEST-3", "x", -1, false)),
            "a negative priority is refused");

        // the critical list is exactly the codes flagged critical
        reg.registerOperationCode("TEST-CRIT", "a critical operation", 1, true);
        check(reg.criticalCodes().stream().anyMatch(c -> c.code.equals("TEST-CRIT")),
            "a critical code appears in the critical list");
        check(reg.criticalCodes().stream().allMatch(c -> c.critical),
            "every code in the critical list is flagged critical");

        // the registry can be listed, and the list is not writable from outside
        check(reg.all().size() == reg.size(), "all() lists every code");
        check(threw(() -> reg.all().add(new OperationCode("X", "x", 0, false))),
            "the code list is unmodifiable");

        // byPriority is sorted, most urgent (highest) first
        var sorted = reg.byPriority();
        boolean ordered = true;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i - 1).priority < sorted.get(i).priority) {
                ordered = false;
                break;
            }
        }
        check(ordered, "byPriority is sorted highest priority first");

        System.out.println("SkalaCodeRegistrySelfTest: " + checks + " checks passed.");
    }

    private static void eq(Object e, Object a, String what) {
        checks++;
        if (!String.valueOf(e).equals(String.valueOf(a))) {
            throw new AssertionError(what + ": expected <" + e + "> but was <" + a + ">");
        }
    }

    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) {
            throw new AssertionError(what);
        }
    }

    private static boolean threw(Runnable r) {
        try {
            r.run();
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }
}
