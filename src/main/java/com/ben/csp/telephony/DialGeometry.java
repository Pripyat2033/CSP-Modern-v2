package com.ben.csp.telephony;

/**
 * The rotary dial as the operator uses it: you put your finger in the hole for the
 * digit you want. The digit is read from where on the dial face the click landed —
 * its angle around the dial centre — so dialling feels like the real thing rather
 * than stepping a counter.
 *
 * <p>The face is described in two coordinates, u across and v up, both 0..1, with the
 * dial centre at {@code (0.5, 0.3)}. Ten holes sit every 36 degrees, numbered
 * clockwise from the top. No Minecraft types here, so the reading can be tested on
 * its own.
 */
public final class DialGeometry {

    /** Which way the dial face points. */
    public enum Face { NORTH, SOUTH, EAST, WEST }

    private DialGeometry() {}

    private static final double CX = 0.5;
    private static final double CY = 0.78;
    private static final double MIN_RADIUS = 0.06;

    /** The digit under a click inside the block, or -1 if it is too near the centre. */
    public static int digitFor(Face facing, double dx, double dy, double dz) {
        double[] uv = faceCoords(facing, dx, dy, dz);
        double ru = uv[0] - CX;
        double rv = uv[1] - CY;
        if (Math.hypot(ru, rv) < MIN_RADIUS) {
            return -1;
        }
        double angleDeg = Math.toDegrees(Math.atan2(rv, ru));
        int digit = (int) Math.round((90.0 - angleDeg) / 36.0);
        digit %= 10;
        return digit < 0 ? digit + 10 : digit;
    }

    /** Map a point inside the block to face coordinates: u across, v up, 0..1. */
    static double[] faceCoords(Face facing, double dx, double dy, double dz) {
        double u = switch (facing) {
            case NORTH -> dx;
            case SOUTH -> 1.0 - dx;
            case WEST -> 1.0 - dz;
            case EAST -> dz;
        };
        return new double[]{u, 1.0 - dy};
    }

    /** The centre of the hole for a digit, on the face, for tests and the renderer. */
    public static double[] holeCentre(int digit) {
        double angle = Math.toRadians(90.0 - digit * 36.0);
        return new double[]{CX + 0.31 * Math.cos(angle), CY + 0.31 * Math.sin(angle)};
    }
}
