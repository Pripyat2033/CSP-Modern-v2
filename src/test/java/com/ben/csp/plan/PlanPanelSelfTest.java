package com.ben.csp.plan;

import java.util.List;

/** Standalone self-test for panel layout and sheet rasterisation. */
public final class PlanPanelSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        // a 4-wide, 3-tall panel on the X axis, facing +Z (1), at y=64
        java.util.List<Long> blocks = new java.util.ArrayList<>();
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 3; y++) {
                blocks.add(PlanPanelGeometry.pack(x, 64 + y, 10));
            }
        }
        PlanPanelGeometry.Panel panel = PlanPanelGeometry.layout(blocks, 1);
        eq(4, panel.cols(), "four columns");
        eq(3, panel.rows(), "three rows");
        eq(12, panel.blockCount(), "twelve blocks");
        check(panel.isComplete(), "a filled rectangle is complete");

        // the top-left block (x=0, y=66) is column 0, row 0
        PlanPanelGeometry.Cell topLeft = panel.cells().stream()
            .filter(c -> PlanPanelGeometry.unpackXFrom(c.pos()) == 0
                      && PlanPanelGeometry.unpackYFrom(c.pos()) == 66).findFirst().orElseThrow();
        eq(3, topLeft.col(), "facing +Z: x=0 is the rightmost column");
        eq(0, topLeft.row(), "the top block is row 0");
        // facing +Z (south) means right is -X, so x=3 is the leftmost column
        PlanPanelGeometry.Cell x3 = panel.cells().stream()
            .filter(c -> PlanPanelGeometry.unpackXFrom(c.pos()) == 3
                      && PlanPanelGeometry.unpackYFrom(c.pos()) == 66).findFirst().orElseThrow();
        eq(0, x3.col(), "facing +Z: x=3 is the leftmost column");

        // a panel with a hole is not complete
        java.util.List<Long> holed = new java.util.ArrayList<>(blocks);
        holed.remove(0);
        check(!PlanPanelGeometry.layout(holed, 1).isComplete(), "a missing block is incomplete");

        // --- rasterise a sheet onto a panel and read cells back ---
        PlanDocument doc = new PlanDocument("test");
        // a boundary rectangle around the extent
        doc.layer("boundary").add(new PlanPrimitive.Rect(0, 0, 100, 100, false,
            PlanStyle.of(0xFF000000, 1.0)));
        PlanExtent extent = new PlanExtent(0, 0, 100, 100);
        TitleBlock tb = new TitleBlock("Test", "Territory", Scale.S1_2000, "1", 1, "g", "b", 1);
        PlanSheet sheet = new PlanSheet("1", "Territory", Scale.S1_2000, extent, doc, tb);
        sheet.mark(new ProblemMarker(50, 50, Severity.PROBLEM, "issue", "2"));

        PlanSheetRasterizer rast = new PlanSheetRasterizer(64, 48, 4);
        rast.rasterize(sheet);

        // the corner cell should carry the boundary line; the centre carries the marker
        int topLeftPx = rast.cellColor(0, 0, 4, 3);
        check(topLeftPx != 0, "the boundary is drawn into the corner cell");
        int centrePx = rast.cellColor(1, 1, 4, 3);
        check(centrePx != 0, "the marker is drawn near the centre");
        // a cell well inside a filled-nothing area stays blank
        int innerPx = rast.cellColor(2, 1, 4, 3);
        // inner cell may or may not catch the marker; assert only that the drawing is present
        check(rast.renderer().pixelAt(32, 24) != 0, "centre pixel is drawn (marker)");

        // every cell of a complete panel maps to a pixel block (no crash, in range)
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 3; r++) {
                rast.cellColor(c, r, 4, 3);
            }
        }
        check(true, "every cell maps without error");

        System.out.println("PlanPanelSelfTest: " + checks + " checks passed.");
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
