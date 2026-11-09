package com.ben.csp.plan;

/**
 * A software rasteriser: it draws a plan into an ARGB pixel buffer. This is a
 * real renderer (not a stand-in), usable for previews and tests; a display can
 * use the same PlanDocument with a different renderer.
 */
public final class RasterPlanRenderer implements PlanRenderer {

    private final int[] pixels;
    private final int width;
    private final int height;
    private PlanView view;

    public RasterPlanRenderer(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("buffer must have a positive size");
        }
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }

    public int width() { return width; }
    public int height() { return height; }

    public int[] pixels() { return pixels; }

    public int pixelAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return 0;
        }
        return pixels[y * width + x];
    }

    @Override
    public void begin(PlanView view) {
        this.view = view;
        java.util.Arrays.fill(pixels, 0);
    }

    @Override
    public void draw(PlanPrimitive primitive) {
        if (primitive instanceof PlanPrimitive.Point p) {
            plot((int) Math.round(view.toPixelX(p.x())),
                 (int) Math.round(view.toPixelY(p.z())), p.style().argb());
        } else if (primitive instanceof PlanPrimitive.Polyline pl) {
            polyline(pl.xs(), pl.zs(), pl.closed(), pl.style().argb());
        } else if (primitive instanceof PlanPrimitive.Rect r) {
            rect(r, r.style().argb());
        } else if (primitive instanceof PlanPrimitive.Polygon pg) {
            polyline(pg.xs(), pg.zs(), true, pg.style().argb());
        }
        // Labels need a font; a text renderer handles them separately.
    }

    @Override
    public void end() {
        // nothing to flush
    }

    private void rect(PlanPrimitive.Rect r, int argb) {
        int x0 = (int) Math.round(view.toPixelX(r.x()));
        int z0 = (int) Math.round(view.toPixelY(r.z()));
        int x1 = (int) Math.round(view.toPixelX(r.x() + r.w()));
        int z1 = (int) Math.round(view.toPixelY(r.z() + r.h()));
        if (r.filled()) {
            for (int y = Math.min(z0, z1); y <= Math.max(z0, z1); y++) {
                for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                    plot(x, y, argb);
                }
            }
        } else {
            line(x0, z0, x1, z0, argb);
            line(x1, z0, x1, z1, argb);
            line(x1, z1, x0, z1, argb);
            line(x0, z1, x0, z0, argb);
        }
    }

    private void polyline(double[] xs, double[] zs, boolean closed, int argb) {
        if (xs.length < 2) {
            return;
        }
        for (int i = 0; i < xs.length - 1; i++) {
            line((int) Math.round(view.toPixelX(xs[i])), (int) Math.round(view.toPixelY(zs[i])),
                 (int) Math.round(view.toPixelX(xs[i + 1])), (int) Math.round(view.toPixelY(zs[i + 1])),
                 argb);
        }
        if (closed) {
            int n = xs.length - 1;
            line((int) Math.round(view.toPixelX(xs[n])), (int) Math.round(view.toPixelY(zs[n])),
                 (int) Math.round(view.toPixelX(xs[0])), (int) Math.round(view.toPixelY(zs[0])),
                 argb);
        }
    }

    private void line(int x0, int y0, int x1, int y1, int argb) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            plot(x0, y0, argb);
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    /** Draw a single pixel; used for markers and overlays. */
    public void setPixel(int x, int y, int argb) {
        plot(x, y, argb);
    }

    private void plot(int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        pixels[y * width + x] = argb;
    }
}
