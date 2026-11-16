package com.ben.csp.prombaza;

import com.ben.csp.logistics.MaterialBatch;

import java.util.Random;

/**
 * Standalone self-test for входной контроль (no Minecraft, no JUnit).
 *
 * <p>Proves the three things the gate must do: it finds defects it can see, it
 * lets a latent defect through exactly when the site is too busy to look
 * properly, and it costs man-hours that make the job take longer.
 */
public final class IncomingControlSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        // --- thoroughness falls with load, with a floor ---
        eq(1.0, IncomingControl.thoroughness(0.0), "no load -> full check");
        check(IncomingControl.thoroughness(1.0) < 1.0, "load reduces thoroughness");
        check(IncomingControl.thoroughness(4.0) < IncomingControl.thoroughness(1.0),
            "more load -> less thorough");
        eq(IncomingControl.MIN_THOROUGHNESS, IncomingControl.thoroughness(1e9),
            "thoroughness has a floor");
        eq(1.0, IncomingControl.thoroughness(-5.0),
            "negative load clamps to zero -> full check");

        // --- a clean batch passes at zero load ---
        MaterialBatch clean = new MaterialBatch("steel", 10, 1.0, "supplier-1");
        eq(0, clean.getDefects().size(), "quality 1.0 makes no defects");
        IncomingControl.Verdict vClean = IncomingControl.inspect(clean, 0.0, new Random(1));
        eq(IncomingControl.Disposition.INSTALLED, vClean.disposition(), "clean batch installed");
        check(vClean.passedClean(), "clean batch passed with nothing hiding");

        // --- a batch with a very visible defect is caught and returned/reworked ---
        MaterialBatch bad = new MaterialBatch("pump", 1, 0.1, "supplier-2");
        // force a severe, fully visible defect so the disposition is deterministic
        bad.getDefects().clear();
        bad.getDefects().add(new com.ben.csp.logistics.Defect(
            "CRACK", 0.9, 1.0, new net.minecraft.util.math.Vec3d(0, 0, 0)));
        IncomingControl.Verdict vBad = IncomingControl.inspect(bad, 0.0, new Random(1));
        eq(IncomingControl.Disposition.RETURNED, vBad.disposition(),
            "severe visible defect -> returned to supplier");
        eq(1, vBad.defectsFound(), "the visible defect was found");
        check(!vBad.latentDefect(), "nothing left hiding when it was fully visible");

        // --- a fixable defect goes to the on-site workshop ---
        MaterialBatch fixable = new MaterialBatch("valve", 1, 0.5, "supplier-3");
        fixable.getDefects().clear();
        fixable.getDefects().add(new com.ben.csp.logistics.Defect(
            "IMPURITY", 0.3, 1.0, new net.minecraft.util.math.Vec3d(0, 0, 0)));
        IncomingControl.Verdict vFix = IncomingControl.inspect(fixable, 0.0, new Random(1));
        eq(IncomingControl.Disposition.REWORKED, vFix.disposition(),
            "minor visible defect -> reworked in the site workshop");

        // --- the load case: a defect that is hard to see slips through when busy ---
        // discoverability 0.0 means it can never be seen; the batch installs anyway,
        // carrying a latent defect -- the case only behaviour can catch later.
        MaterialBatch sneaky = new MaterialBatch("cable", 1, 0.5, "supplier-4");
        sneaky.getDefects().clear();
        sneaky.getDefects().add(new com.ben.csp.logistics.Defect(
            "LATENT", 0.3, 0.0, new net.minecraft.util.math.Vec3d(0, 0, 0)));
        IncomingControl.Verdict vSneak = IncomingControl.inspect(sneaky, 3.0, new Random(7));
        eq(IncomingControl.Disposition.INSTALLED, vSneak.disposition(),
            "an invisible defect does not stop installation");
        check(vSneak.latentDefect(), "the batch installed carrying a latent defect");
        check(!vSneak.passedClean(), "installed is not the same as passed clean");
        eq(1, sneaky.getDefects().size(), "the latent defect is still recorded on the batch");
        eq(0, vSneak.defectsFound(), "the latent defect was never found");

        // --- suspect batch with no proven defect is quarantined, not installed ---
        MaterialBatch suspect = new MaterialBatch("rebar", 5, 0.2, "supplier-5");
        suspect.getDefects().clear();
        IncomingControl.Verdict vSuspect = IncomingControl.inspect(suspect, 0.0, new Random(1));
        eq(IncomingControl.Disposition.QUARANTINED, vSuspect.disposition(),
            "low inherent quality with no proof -> held");

        // --- detection improves as thoroughness improves (same defect, same seed) ---
        int foundAtLowLoad = 0;
        int foundAtHighLoad = 0;
        for (int seed = 0; seed < 2000; seed++) {
            if (foundVisible(seed, 0.0)) foundAtLowLoad++;
            if (foundVisible(seed, 9.0)) foundAtHighLoad++;
        }
        check(foundAtLowLoad > foundAtHighLoad,
            "a full check finds a visible defect more often than a rushed one");

        // --- the cost: inspection man-hours make the job take longer ---
        eq(1.0, IncomingControl.buildTimeMultiplier(0, 100), "no inspection, no delay");
        eq(1.5, IncomingControl.buildTimeMultiplier(50, 100),
            "inspection half the building effort -> 50% longer");
        eq(1.0, IncomingControl.buildTimeMultiplier(50, 0), "no building, no multiplier");

        // --- the desk: a clean batch is cleared, a bad one is held, a sneaky2 one is latent ---
        PrombazaControl desk = new PrombazaControl("pb1");
        desk.bookBuildingManHours(100.0);

        MaterialBatch good = new MaterialBatch("steel", 10, 1.0, "s1");
        desk.receive(good, 0.0);
        eq(1, desk.cleared(), "the clean batch is cleared for building");
        eq(0, desk.held(), "nothing held yet");
        eq(0, desk.latent(), "nothing latent yet");

        MaterialBatch cracked = new MaterialBatch("pump", 1, 0.5, "s2");
        cracked.getDefects().clear();
        cracked.getDefects().add(new com.ben.csp.logistics.Defect(
            "CRACK", 0.9, 1.0, new net.minecraft.util.math.Vec3d(0, 0, 0)));
        desk.receive(cracked, 0.0);
        eq(1, desk.held(), "the cracked pump is held, not cleared");

        MaterialBatch sneaky2 = new MaterialBatch("cable", 1, 0.5, "s3");
        sneaky2.getDefects().clear();
        sneaky2.getDefects().add(new com.ben.csp.logistics.Defect(
            "LATENT", 0.3, 0.0, new net.minecraft.util.math.Vec3d(0, 0, 0)));
        desk.receive(sneaky2, 3.0);
        eq(2, desk.cleared(), "the sneaky2 batch is cleared too");
        eq(1, desk.latent(), "but it is counted as latent -- only behaviour will catch it");
        eq(3, desk.receipts().size(), "three receipts on the desk");

        // --- the desk books the cost, and it shows up as a build-time multiplier ---
        check(desk.inspectionManHours() > 0, "the desk booked inspection man-hours");
        check(desk.buildTimeMultiplier() > 1.0, "checking makes the job take longer");

        // --- report lines name every batch ---
        eq(3, desk.report().size(), "one report line per receipt");
        check(desk.report().get(2).contains("LATENT"), "the latent batch is flagged in the report");

        System.out.println("IncomingControlSelfTest: " + checks + " checks passed.");
    }

    /** Inspect a batch whose single defect is fully visible; report whether it was found. */
    private static boolean foundVisible(int seed, double load) {
        MaterialBatch b = new MaterialBatch("x", 1, 0.5, "s");
        b.getDefects().clear();
        b.getDefects().add(new com.ben.csp.logistics.Defect(
            "CRACK", 0.3, 0.8, new net.minecraft.util.math.Vec3d(0, 0, 0)));
        IncomingControl.Verdict v = IncomingControl.inspect(b, load, new Random(seed));
        return v.defectsFound() > 0;
    }

    private static void eq(Object expected, Object actual, String what) {
        checks++;
        if (!String.valueOf(expected).equals(String.valueOf(actual))) {
            throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) {
            throw new AssertionError(what);
        }
    }
}
