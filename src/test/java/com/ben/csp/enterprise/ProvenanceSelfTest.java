package com.ben.csp.enterprise;

/** Standalone self-test for the verified/reconstruction marking. */
public final class ProvenanceSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        // a facility is a reconstruction unless it is marked otherwise
        Facility f = new Facility("f1", FacilityType.HEADQUARTERS, "HQ", 4);
        eq(Provenance.RECONSTRUCTION, f.provenance(),
            "an unmarked facility is a reconstruction, not a claim");
        check(!f.provenance().isVerified(), "a reconstruction is not verified");

        f.setProvenance(Provenance.VERIFIED);
        eq(Provenance.VERIFIED, f.provenance(), "a facility can be marked verified");
        check(f.provenance().isVerified(), "and then it is verified");

        eq("Verified", Provenance.VERIFIED.label(), "verified has a label");
        eq("Reconstruction", Provenance.RECONSTRUCTION.label(), "reconstruction has a label");
        check(Provenance.RECONSTRUCTION.note().length() > 0, "a reconstruction states its status");

        System.out.println("ProvenanceSelfTest: " + checks + " checks passed.");
    }

    private static void eq(Object e, Object a, String what) {
        checks++;
        if (!String.valueOf(e).equals(String.valueOf(a))) {
            throw new AssertionError(what + ": expected <" + e + "> but was <" + a + ">");
        }
    }
    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) throw new AssertionError(what);
    }
}
