package com.ben.csp.rbmk.physics;

import java.util.List;

/**
 * Self-test for {@link PhysicsProvenance}: the machine-checkable form of mission
 * rule 2 ("nothing is guessed"). It says every physics constant is either sourced
 * or explicitly flagged — never silently a number — and that the registry has not
 * drifted away from the constants the code actually uses.
 *
 * <ul>
 *   <li>the registry loads and covers the core constants;</li>
 *   <li>every {@code to_verify} entry states what it must be verified against;</li>
 *   <li>every {@code verified} entry carries a machine-checkable citation
 *       (a sourceUrl and a quote) — "verified" cannot mean "we said so";</li>
 *   <li>there is NO unresolved placeholder — no guessed number in the model;</li>
 *   <li>the registered value agrees with the constant in the code (beta_eff).</li>
 * </ul>
 */
public final class PhysicsProvenanceSelfTest {

    private static int checks = 0;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError("FAILED: " + what);
        }
    }

    public static void main(String[] args) {
        check(PhysicsProvenance.available(), "the provenance registry loads");
        check(PhysicsProvenance.size() >= 10,
            "the registry covers the core constants (" + PhysicsProvenance.size() + ")");

        // every reference value must say what it is to be verified against
        for (String key : PhysicsProvenance.toVerify()) {
            PhysicsProvenance.Entry e = PhysicsProvenance.get(key);
            check(e.source() != null && e.source().length() > 10,
                "to_verify entry '" + key + "' cites what it awaits");
        }
        check(!PhysicsProvenance.toVerify().isEmpty(),
            "the reference values are flagged to_verify, not passed off as verified");

        // every VERIFIED entry must carry a citation we can check: a source URL
        // and a verbatim quote. This is what stops "verified" from being an
        // assertion with no evidence behind it.
        for (String key : PhysicsProvenance.verified()) {
            PhysicsProvenance.Entry e = PhysicsProvenance.get(key);
            check(e.sourceUrl() != null && e.sourceUrl().startsWith("http"),
                "verified entry '" + key + "' cites a source URL");
            check(e.quote() != null && e.quote().length() > 10,
                "verified entry '" + key + "' carries a verbatim quote");
        }
        check(!PhysicsProvenance.verified().isEmpty(),
            "at least one constant is now sourced from a primary document");

        // no guessed number may sit in the model unexplained
        List<String> unresolved = PhysicsProvenance.unresolvedPlaceholders();
        check(unresolved.isEmpty(),
            "no unresolved placeholder constants (" + unresolved + ")");

        // a fallback placeholder is allowed, but only as a marked fallback
        PhysicsProvenance.Entry fb = PhysicsProvenance.get("xsec.void_feedback_scale");
        check(fb != null && fb.status() == PhysicsProvenance.Status.PLACEHOLDER && fb.fallback(),
            "the legacy feedback scale is a marked fallback, not an active guess");

        // the registry must agree with the code it describes
        PhysicsProvenance.Entry beta = PhysicsProvenance.get("delayed.beta_eff");
        check(beta != null && beta.status() == PhysicsProvenance.Status.TO_VERIFY,
            "beta_eff is registered and flagged to_verify");
        check(Math.abs(beta.value() - DelayedNeutrons.BETA_EFFECTIVE) < 1e-12,
            "the registered beta_eff matches the constant in the code");

        PhysicsProvenance.Entry voidc = PhysicsProvenance.get("feedback.void_coeff");
        check(voidc != null && Math.abs(voidc.value() - ReactivityModel.VOID_COEFF) < 1e-12,
            "the registered void coefficient matches the constant in the code");

        // unknown keys are reported as unknown, not invented
        check(PhysicsProvenance.status("no.such.key") == null,
            "an unregistered key has no status (not silently verified)");

        System.out.println("PhysicsProvenanceSelfTest: " + checks + " checks passed.");
    }
}
