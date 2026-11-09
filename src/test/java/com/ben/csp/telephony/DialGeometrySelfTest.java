package com.ben.csp.telephony;

import com.ben.csp.telephony.DialGeometry.Face;

/** The dial must read the digit you point at, for every facing. */
public final class DialGeometrySelfTest {

    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        for (Face facing : Face.values()) {
            for (int digit = 0; digit < 10; digit++) {
                double[] uv = DialGeometry.holeCentre(digit);
                double[] local = toLocal(facing, uv[0], uv[1]);
                int got = DialGeometry.digitFor(facing, local[0], local[1], local[2]);
                check(facing + " digit " + digit, digit, got);
            }
        }
        check("a click at the centre is ignored", -1,
            DialGeometry.digitFor(Face.NORTH, 0.5, 0.7, 0.02));

        System.out.println(failures == 0
            ? "DialGeometrySelfTest: " + checks + " checks passed"
            : "DialGeometrySelfTest: " + failures + " FAILED of " + checks);
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** The inverse of faceCoords: a face point back to a point inside the block. */
    private static double[] toLocal(Face facing, double u, double v) {
        double dy = 1.0 - v;
        double dx;
        double dz;
        switch (facing) {
            case NORTH -> { dx = u; dz = 0.05; }
            case SOUTH -> { dx = 1.0 - u; dz = 0.95; }
            case WEST -> { dx = 0.05; dz = 1.0 - u; }
            default -> { dx = 0.95; dz = u; }
        }
        return new double[]{dx, dy, dz};
    }

    private static void check(String what, int want, int got) {
        checks++;
        if (want != got) {
            failures++;
            System.out.println("  FAIL: " + what + " -> " + got + ", wanted " + want);
        }
    }
}
