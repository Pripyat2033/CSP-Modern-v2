package com.ben.csp.plan;

import java.util.Optional;

/** Standalone self-test for sheets, standard scales and callouts. */
public final class SheetSetSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        // standard scales step in order
        eq(2000, Scale.S1_2000.denominator(), "1:2000 denominator");
        eq("1:500", Scale.S1_500.label(), "scale label");
        eq(Scale.S1_200, Scale.S1_500.moreDetailed(), "more detailed steps in");
        eq(Scale.S1_1000, Scale.S1_500.lessDetailed(), "less detailed steps out");
        eq(Scale.S1_5, Scale.S1_5.moreDetailed(), "cannot go finer than the last");
        eq(Scale.S1_2000, Scale.S1_2000.lessDetailed(), "cannot go coarser than the first");
        check(Scale.S1_500.canZoomIn(), "1:500 can zoom in");
        check(Scale.S1_5.canZoomOut(), "1:5 can zoom out");

        // three sheets at three scales for one object
        SheetSet set = new SheetSet("Central Prombaza");
        set.add(sheet("1", "Territory plan", Scale.S1_2000, 0, 0, 2000, 2000));
        set.add(sheet("2", "Construction site plan", Scale.S1_500, 90, 80, 350, 200));
        set.add(sheet("3", "Facility detail", Scale.S1_100, 100, 100, 160, 140));

        set.startCoarsest();
        eq("1", set.current().sheetNumber(), "starts on the coarsest sheet");
        eq(Scale.S1_2000, set.current().scale(), "which is 1:2000");
        check(set.canZoomIn(), "can zoom in from the coarsest");
        check(!set.canZoomOut(), "cannot zoom out from the coarsest");

        // zoom in steps to the next scale that exists
        check(set.zoomIn(), "zoom in works");
        eq("2", set.current().sheetNumber(), "now on 1:500 sheet");
        check(set.zoomIn(), "zoom in again");
        eq("3", set.current().sheetNumber(), "now on 1:100 sheet");
        check(!set.zoomIn(), "no finer sheet exists");
        eq("3", set.current().sheetNumber(), "stays put at the limit");

        check(set.zoomOut(), "zoom out works");
        eq("2", set.current().sheetNumber(), "back to 1:500");

        // the title block says what the sheet is
        TitleBlock tb = set.current().titleBlock();
        eq("Central Prombaza", tb.objectName(), "title block names the object");
        eq(Scale.S1_500, tb.scale(), "title block carries the scale");
        eq("Лист 2 из 3, 1:500", tb.sheetLine(), "sheet line is legible");

        // problem markers with a callout, and following it
        PlanSheet site = set.byNumber("2").orElseThrow();
        site.mark(new ProblemMarker(120, 120, Severity.PROBLEM,
            "Concrete plant behind schedule", "3"));
        eq(1, site.markers().size(), "one marker on the site sheet");
        check(site.markers().get(0).hasCallout(), "the marker has a callout");

        // stand on the problem and follow the callout to the detail sheet
        Optional<PlanSheet> detail = set.followCallout(121, 121, 5);
        check(detail.isPresent(), "a callout near the problem resolves");
        eq("3", set.current().sheetNumber(), "following the callout jumps to the detail sheet");
        eq(Scale.S1_100, set.current().scale(), "which is at the larger scale");

        // a callout away from any marker goes nowhere
        check(set.followCallout(900, 900, 5).isEmpty(), "no callout away from the markers");

        System.out.println("SheetSetSelfTest: " + checks + " checks passed.");
    }

    private static PlanSheet sheet(String number, String title, Scale scale,
                                   int minX, int minZ, int maxX, int maxZ) {
        PlanView view = PlanView.fit(minX, minZ, maxX, maxZ, 256, 192, 12);
        TitleBlock tb = new TitleBlock("Central Prombaza", title, scale, number, 3,
            "geodezist-1", "Ben", 40);
        return new PlanSheet(number, title, scale, view, tb);
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
