package com.ben.csp.enterprise;

import java.util.List;

/**
 * The workforce is a fact, not a guess: ten enterprises and six hundred and eight
 * people in their facilities. This guards that number, because the whole scale question
 * (and the GO decision) rests on it.
 */
public final class WorkforceSelfTest {

    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        List<Enterprise> roster = EnterpriseRoster.provisional();

        check("ten enterprises", 10, roster.size());

        int staff = roster.stream()
            .mapToInt(e -> e.facilities().stream().mapToInt(Facility::requiredStaff).sum())
            .sum();
        check("facility staff", 608, staff);
        check("workforce with enterprise overhead", 688, staff + roster.size() * 8);

        long verified = roster.stream()
            .filter(e -> e.provenance() == Provenance.VERIFIED).count();
        check("one cited enterprise (the general contractor)", 1, (int) verified);

        boolean anyEmpty = roster.stream().anyMatch(e -> e.facilities().isEmpty());
        check("no enterprise is without facilities", false, anyEmpty);

        System.out.println(failures == 0
            ? "WorkforceSelfTest: " + checks + " checks passed"
            : "WorkforceSelfTest: " + failures + " FAILED of " + checks);
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(String what, int want, int got) {
        checks++;
        if (want != got) {
            failures++;
            System.out.println("  FAIL: " + what + " -> " + got + ", wanted " + want);
        }
    }

    private static void check(String what, boolean want, boolean got) {
        checks++;
        if (want != got) {
            failures++;
            System.out.println("  FAIL: " + what + " -> " + got + ", wanted " + want);
        }
    }
}
