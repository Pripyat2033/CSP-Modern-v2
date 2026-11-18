package com.ben.csp.rbmk.physics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The GRAPHITE temperature dependence of the lattice cell, derived from the OpenMC
 * cell (see {@code openmc/rbmk_graphite_mgxs.py}).
 *
 * <p>The RBMK's graphite temperature coefficient is POSITIVE — a documented
 * destabilising property (INSAG-7 Table II-I, +6e-5/degC; NUREG-1250 corroborates).
 * It was absent from the live core: {@code UnifiedReactorCore} computed a graphite
 * temperature but never fed it to the neutronics, and the retired point-kinetics
 * model carried the wrong (negative) sign. This class supplies the missing link.
 *
 * <p>The effect is in the THERMAL SCATTERING: as the graphite heats, up-scatter
 * into the fast group (ss21) and the fast removal (sr1) grow, so k rises. Each
 * homogenised constant is scaled by {@code 1 + fracPerK * (T_K - refTempK)}. The
 * fractions are per-kelvin fractional changes measured over a 200 K sweep above
 * the operating point.
 *
 * <p>The SIGN is sourced; the MAGNITUDE is {@code to_verify} (the library carries
 * the graphite S(alpha,beta) law at a finite temperature grid, so the sweep
 * captures the thermal-law temperature dependence at the grid resolution). At the
 * reference temperature the factor is 1.0, so the reference critical state is
 * unchanged.
 */
public final class RbmkGraphiteTemp {

    private static final double[] FRAC_PER_K = new double[8];
    private static final boolean AVAILABLE;
    private static final double REF_TEMP_K;
    private static final double DERIVED_COEFF_PER_C;
    private static final String LIBRARY;
    private static final String STATUS;

    static {
        boolean ok = false;
        double ref = 873.15;
        double coeff = Double.NaN;
        String lib = "unknown";
        String status = "unknown";
        try (InputStream in = RbmkGraphiteTemp.class
                .getResourceAsStream("/rbmk_graphite_temp.json")) {
            if (in != null) {
                JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject f = root.getAsJsonObject("fracPerK");
                FRAC_PER_K[0] = f.get("d1").getAsDouble();
                FRAC_PER_K[1] = f.get("sr1").getAsDouble();
                FRAC_PER_K[2] = f.get("nsf1").getAsDouble();
                FRAC_PER_K[3] = f.get("d2").getAsDouble();
                FRAC_PER_K[4] = f.get("sa2").getAsDouble();
                FRAC_PER_K[5] = f.get("nsf2").getAsDouble();
                FRAC_PER_K[6] = f.get("ss12").getAsDouble();
                FRAC_PER_K[7] = f.get("ss21").getAsDouble();
                ref = root.get("nominalGraphiteK").getAsDouble();
                coeff = root.get("slopeCoeffPerC").getAsDouble();
                lib = root.get("library").getAsString();
                status = root.get("status").getAsString();
                ok = true;
            }
        } catch (Exception e) {
            ok = false;
        }
        AVAILABLE = ok;
        REF_TEMP_K = ref;
        DERIVED_COEFF_PER_C = coeff;
        LIBRARY = lib;
        STATUS = status;
    }

    private RbmkGraphiteTemp() {}

    public static boolean available() {
        return AVAILABLE;
    }

    /** Reference graphite temperature, in kelvin. */
    public static double refTempK() {
        return REF_TEMP_K;
    }

    /** The derived net graphite temperature coefficient, dk/k per degC (positive). */
    public static double derivedCoeffPerC() {
        return DERIVED_COEFF_PER_C;
    }

    /**
     * True only when the live lattice table was produced by the SAME OpenMC cell the
     * graphite slopes were measured from. The slopes are FRACTIONAL changes of that
     * cell's constants, so applying them to a different table (the 1-D one, whose
     * fast-removal and up-scatter constants differ by an order of magnitude) flips
     * the sign of the effect. The pair must match, or the link is a no-op.
     */
    public static boolean matchesLattice() {
        return LatticeTable.source().startsWith("openmc");
    }

    public static String library() {
        return LIBRARY;
    }

    public static String status() {
        return STATUS;
    }

    /**
     * Scale a cell's constants for the given graphite temperature, in degrees
     * Celsius (the core's unit). A no-op (returns the cell unchanged) when the
     * derived data is missing or the temperature is the reference, so the reference
     * critical state is preserved.
     */
    public static RbmkXsec apply(RbmkXsec cell, double graphiteTempC) {
        if (!AVAILABLE || !matchesLattice()) {
            return cell;
        }
        double dT = (graphiteTempC + 273.15) - REF_TEMP_K;
        if (dT == 0.0) {
            return cell;
        }
        return new RbmkXsec(
            cell.d1()  * (1.0 + FRAC_PER_K[0] * dT),
            cell.sr1() * (1.0 + FRAC_PER_K[1] * dT),
            cell.nsf1() * (1.0 + FRAC_PER_K[2] * dT),
            cell.d2()  * (1.0 + FRAC_PER_K[3] * dT),
            cell.sa2() * (1.0 + FRAC_PER_K[4] * dT),
            cell.nsf2() * (1.0 + FRAC_PER_K[5] * dT),
            cell.ss12() * (1.0 + FRAC_PER_K[6] * dT),
            cell.ss21() * (1.0 + FRAC_PER_K[7] * dT));
    }
}
