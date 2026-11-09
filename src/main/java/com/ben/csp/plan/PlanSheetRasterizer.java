package com.ben.csp.plan;

/**
 * Rasterises a sheet onto a surface of a given size, and reports the pixel for
 * any cell of that surface. The display maps its blocks to cells; this maps cells
 * to plan pixels. It stays generic: it draws a plan, it does not know what the
 * plan depicts.
 */
public final class PlanSheetRasterizer {

    private final RasterPlanRenderer renderer;
    private final int widthPx;
    private final int heightPx;

    public PlanSheetRasterizer(int widthPx, int heightPx, double marginPx) {
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.renderer = new RasterPlanRenderer(widthPx, heightPx);
        this.marginPx = marginPx;
    }

    private final double marginPx;

    public int widthPx() { return widthPx; }
    public int heightPx() { return heightPx; }

    /** Rasterise a sheet into this surface. */
    public void rasterize(PlanSheet sheet) {
        PlanView view = sheet.viewFor(widthPx, heightPx, marginPx);
        renderer.begin(view);
        renderer.drawAll(sheet.document().visiblePrimitives());
        // problem markers are drawn as small squares in their severity colour
        for (ProblemMarker m : sheet.markers()) {
            int argb = switch (m.severity()) {
                case NOTE -> 0xFF2E6DA4;
                case WARNING -> 0xFFC98A00;
                case PROBLEM -> 0xFFB00020;
            };
            int cx = (int) Math.round(view.toPixelX(m.x()));
            int cz = (int) Math.round(view.toPixelY(m.z()));
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    renderer.setPixel(cx + dx, cz + dz, argb);
                }
            }
        }
        renderer.end();
    }

    /** The colour for a cell of the display, given the cell's pixel block. */
    public int cellColor(int col, int row, int cols, int rows) {
        int x0 = col * widthPx / cols;
        int x1 = Math.max(x0 + 1, (col + 1) * widthPx / cols);
        int y0 = row * heightPx / rows;
        int y1 = Math.max(y0 + 1, (row + 1) * heightPx / rows);
        int argb = 0; // transparent if nothing drawn
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int p = renderer.pixelAt(x, y);
                if (p != 0) {
                    argb = p;
                }
            }
        }
        return argb;
    }

    public RasterPlanRenderer renderer() {
        return renderer;
    }
}
