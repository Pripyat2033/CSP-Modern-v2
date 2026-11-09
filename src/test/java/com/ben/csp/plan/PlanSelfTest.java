package com.ben.csp.plan;

import com.ben.csp.prombaza.Area;
import com.ben.csp.prombaza.PrombazaRegistry;

import java.util.Optional;

/** Standalone self-test for the generic plan display. */
public final class PlanSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        // --- PlanView mapping ---
        PlanView v = new PlanView(0, 0, 2.0, 100, 100);
        eq(50.0, v.toPixelX(0), "centre maps to the middle of the display");
        eq(70.0, v.toPixelX(10), "10 blocks right is 20 px at 2 px/block");
        eq(10.0, v.toWorldX(70), "and it maps back");
        eq(0.0, v.toWorldZ(50), "z centre maps back");

        // fit an area into a display
        PlanView fit = PlanView.fit(0, 0, 100, 50, 200, 200, 10);
        check(fit.pixelsPerBlock() > 0, "fit gives a positive scale");
        // the wider axis (x=100) should drive the scale: 180 usable / 100 = 1.8
        eq(1.8, fit.pixelsPerBlock(), "fit scales to the limiting axis");
        eq(50.0, fit.centerX(), "fit centres on the area");

        // --- the same renderer draws two entirely different plans ---
        RasterPlanRenderer r = new RasterPlanRenderer(64, 64);
        PlanView view = PlanView.fit(0, 0, 64, 64, 64, 64, 2);

        // plan A: a square boundary (like a prombaza)
        PlanDocument a = new PlanDocument("A");
        a.layer("boundary").add(new PlanPrimitive.Rect(8, 8, 48, 48, false,
            PlanStyle.of(0xFF000000, 1.0)));
        r.begin(view);
        r.drawAll(a.visiblePrimitives());
        r.end();
        int aDark = countNonZero(r);
        check(aDark > 0, "plan A draws a boundary");
        check(r.pixelAt(2, 2) == 0, "plan A leaves the corner blank");

        // plan B: a diagonal line and a dot (like a power-line route) — nothing in common
        PlanDocument b = new PlanDocument("B");
        b.layer("route").add(new PlanPrimitive.Polyline(
            new double[] {0, 63}, new double[] {0, 63}, false, PlanStyle.of(0xFF00FF00, 1.0)));
        b.layer("route").add(new PlanPrimitive.Point(32, 32, PlanStyle.of(0xFFFF0000, 1.0)));
        r.begin(view);
        r.drawAll(b.visiblePrimitives());
        r.end();
        check(r.pixelAt(32, 32) != 0, "plan B draws its dot at the centre");
        check(r.pixelAt(1, 1) != 0 || r.pixelAt(2, 2) != 0, "plan B draws its diagonal near the origin");
        check(countNonZero(r) < 64 * 64, "plan B is not a filled square");

        // --- hidden layers are not drawn ---
        PlanDocument c = new PlanDocument("C");
        PlanLayer shown = c.layer("shown");
        shown.add(new PlanPrimitive.Point(0, 0, PlanStyle.of(0xFFFFFFFF, 1.0)));
        PlanLayer hidden = c.layer("hidden");
        hidden.add(new PlanPrimitive.Point(0, 0, PlanStyle.of(0xFFFFFFFF, 1.0)));
        hidden.setVisible(false);
        eq(1, c.visiblePrimitives().size(), "a hidden layer is excluded");
        hidden.setVisible(true);
        eq(2, c.visiblePrimitives().size(), "showing the layer includes it");

        // --- the prombaza source builds a plan from the registry ---
        PrombazaRegistry reg = new PrombazaRegistry();
        PrombazaPlanSource src = new PrombazaPlanSource(reg, "Prombaza plan");
        check(src.buildPlan().isEmpty(), "no prombaza means no plan");
        reg.designate("pb-1", "Central Prombaza", Area.of(100, 60, 100, 140, 80, 140), 1, "Ben");
        Optional<PlanDocument> doc = src.buildPlan();
        check(doc.isPresent(), "a prombaza yields a plan");
        check(doc.get().visiblePrimitives().size() >= 1, "the plan has the boundary");
        PlanPrimitive first = doc.get().visiblePrimitives().get(0);
        check(first instanceof PlanPrimitive.Polyline, "the boundary is a closed polyline");
        PlanPrimitive.Polyline pl = (PlanPrimitive.Polyline) first;
        check(pl.closed(), "the boundary is closed");
        eq(100.0, pl.xs()[0], "the boundary uses real world coordinates");

        // --- the same source can be shown at a different scale (the point of it) ---
        RasterPlanRenderer big = new RasterPlanRenderer(256, 256);
        PlanView bigView = PlanView.fit(100, 100, 141, 141, 256, 256, 4);
        big.begin(bigView);
        big.drawAll(doc.get().visiblePrimitives());
        big.end();
        check(countNonZero(big) > 0, "the prombaza plan draws at a larger size too");

        System.out.println("PlanSelfTest: " + checks + " checks passed.");
    }

    private static int countNonZero(RasterPlanRenderer r) {
        int n = 0;
        for (int p : r.pixels()) {
            if (p != 0) n++;
        }
        return n;
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
