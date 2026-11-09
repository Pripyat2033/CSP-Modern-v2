package com.ben.csp.plan;

/**
 * Maps plan space (world X/Z) onto a display surface (pixels), with a scale and
 * a centre. The same view works for any plan, whatever it depicts, and the same
 * plan can be shown at any scale on any size of display.
 */
public record PlanView(double centerX, double centerZ, double pixelsPerBlock,
                       int widthPx, int heightPx) {

    public PlanView {
        if (pixelsPerBlock <= 0) {
            throw new IllegalArgumentException("pixelsPerBlock must be positive");
        }
        if (widthPx <= 0 || heightPx <= 0) {
            throw new IllegalArgumentException("display must have a positive size");
        }
    }

    /** A view that fits an area into a display of the given pixel size. */
    public static PlanView fit(double minX, double minZ, double maxX, double maxZ,
                               int widthPx, int heightPx, double marginPx) {
        double spanX = Math.max(1e-6, maxX - minX);
        double spanZ = Math.max(1e-6, maxZ - minZ);
        double usableW = Math.max(1.0, widthPx - 2 * marginPx);
        double usableH = Math.max(1.0, heightPx - 2 * marginPx);
        double scale = Math.min(usableW / spanX, usableH / spanZ);
        return new PlanView((minX + maxX) / 2.0, (minZ + maxZ) / 2.0,
                            scale, widthPx, heightPx);
    }

    public double toPixelX(double worldX) {
        return widthPx / 2.0 + (worldX - centerX) * pixelsPerBlock;
    }

    public double toPixelY(double worldZ) {
        return heightPx / 2.0 + (worldZ - centerZ) * pixelsPerBlock;
    }

    public double toWorldX(double px) {
        return centerX + (px - widthPx / 2.0) / pixelsPerBlock;
    }

    public double toWorldZ(double py) {
        return centerZ + (py - heightPx / 2.0) / pixelsPerBlock;
    }
}
