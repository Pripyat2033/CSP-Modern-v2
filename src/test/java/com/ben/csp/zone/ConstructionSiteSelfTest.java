package com.ben.csp.zone;

import com.ben.csp.enterprise.Footprint;

/** Standalone self-test: a facility grows from a marker into a building. */
public final class ConstructionSiteSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        Footprint plot = Footprint.of(0, 0, 20, 20);
        ConstructionSite site = new ConstructionSite("cs1", "fac1", plot, 800.0);

        // it starts as a marker on the ground, with nothing raised
        eq(BuildStage.STAKED, site.stage(), "a new site is only a marker");
        eq(0.0, site.progress(), "no work has been done yet");
        check(!site.isComplete(), "the site is not complete");

        // with no brigade, no work happens
        double done = site.work(100);
        eq(0.0, done, "no brigade means no progress");

        // assign a brigade but no materials: still no work
        site.assignBrigade("br1");
        done = site.work(100);
        eq(0.0, done, "no materials means no progress");

        // deliver materials: now the work goes in
        site.deliverMaterials(site.materialsRequired());
        done = site.work(200);
        check(done > 0, "with a brigade and materials, work is done");
        check(site.progress() > 0, "the site has made progress");

        // it passes through the stages as it grows
        boolean sawFoundation = false, sawStructure = false, sawRoofed = false;
        for (int day = 0; day < 100 && !site.isComplete(); day++) {
            switch (site.stage()) {
                case FOUNDATION -> sawFoundation = true;
                case STRUCTURE -> sawStructure = true;
                case ROOFED -> sawRoofed = true;
                default -> { }
            }
            site.work(40);
        }
        check(sawFoundation, "the site reaches the foundation stage");
        check(sawStructure, "the site reaches the structure stage");
        check(sawRoofed, "the site reaches the roofed stage");
        check(site.isComplete(), "the site finishes");
        eq(BuildStage.FINISHED, site.stage(), "a finished site is a building");

        // materials cap the work: it cannot outrun its supply
        ConstructionSite s2 = new ConstructionSite("cs2", "fac2", plot, 800.0);
        s2.assignBrigade("br1");
        s2.deliverMaterials(s2.materialsRequired() / 10.0);
        s2.work(100000);
        check(s2.progress() < 1.0, "a site cannot finish without materials for the whole job");

        // the material norm is honest about its provenance, and the demand is broken
        // into kinds so a delivery plan can name concrete, not "materials"
        check(site.materialNorm().isProvisional(),
            "the default material norm is provisional until the сметные нормы are read in");
        var breakdown = site.materialBreakdown();
        check(!breakdown.isEmpty(), "the site names the kinds of material it needs");
        double sum = breakdown.values().stream().mapToDouble(Double::doubleValue).sum();
        check(Math.abs(sum - site.materialsRequired()) < 1e-6,
            "the breakdown adds up to the total demand");

        // a site given a sourced norm honours it exactly
        MaterialNorm sourced = new MaterialNorm(0.5,
            com.ben.csp.work.NormSource.ENIR_SOURCED, "test figure");
        ConstructionSite s3 = new ConstructionSite("cs3", "fac3", plot, 100.0, sourced);
        eq(50.0, s3.materialsRequired(), "materials follow the norm (100 mh x 0.5)");
        check(!s3.materialNorm().isProvisional(), "a sourced norm is not provisional");

        System.out.println("ConstructionSiteSelfTest: " + checks + " checks passed.");
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
