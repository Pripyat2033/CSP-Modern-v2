package com.ben.csp.rbmk.physics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The two-group reactor constants for the RBMK lattice cell, as DERIVED by the
 * lattice calculation in {@code tools/lattice} from evaluated nuclear data
 * (ENDF/B-VIII.0) and the real channel geometry, and emitted to
 * {@code rbmk_lattice.json}.
 *
 * <p>This replaces the hand-set cross sections and feedback scales that used to
 * live in {@link RbmkXsec}. The void dependence of every constant — including the
 * positive void coefficient that defines the RBMK — now comes from the physics,
 * not from a tuned factor.
 *
 * <p>Lookup is linear in void fraction between the tabulated points.
 */
public final class LatticeTable {

    /** One tabulated cell state. Fields are the two-group constants, cm^-1. */
    public record Row(double voidFraction, double kInf, double d1, double sr1,
                      double nsf1, double d2, double sa2, double nsf2, double ss12,
                      double ss21) {}

    private static final List<Row> ROWS = new ArrayList<>();
    private static final java.util.Map<String, RbmkXsec> CONTROLS = new java.util.HashMap<>();
    private static final double THERMAL_CUT_EV;
    private static final double BETA_EFF;

    static {
        double cut = 0.625, beta = 0.0065;
        try (InputStream in = LatticeTable.class.getResourceAsStream("/rbmk_lattice.json")) {
            if (in != null) {
                JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                cut = root.get("thermalCutEV").getAsDouble();
                beta = root.get("betaEff").getAsDouble();
                JsonArray table = root.getAsJsonArray("table");
                for (var el : table) {
                    JsonObject o = el.getAsJsonObject();
                    ROWS.add(new Row(
                        o.get("void").getAsDouble(), o.get("kInf").getAsDouble(),
                        o.get("d1").getAsDouble(), o.get("sr1").getAsDouble(),
                        o.get("nsf1").getAsDouble(), o.get("d2").getAsDouble(),
                        o.get("sa2").getAsDouble(), o.get("nsf2").getAsDouble(),
                        o.get("ss12").getAsDouble(),
                        o.has("ss21") ? o.get("ss21").getAsDouble() : 0.0));
                }
                if (root.has("controls")) {
                    JsonObject controls = root.getAsJsonObject("controls");
                    for (String name : controls.keySet()) {
                        CONTROLS.put(name, toXsec(controls.getAsJsonObject(name)));
                    }
                }
            }
        } catch (Exception e) {
            ROWS.clear();
            CONTROLS.clear();
        }
        THERMAL_CUT_EV = cut;
        BETA_EFF = beta;
    }

    private static RbmkXsec toXsec(JsonObject o) {
        return new RbmkXsec(
            o.get("d1").getAsDouble(), o.get("sr1").getAsDouble(), o.get("nsf1").getAsDouble(),
            o.get("d2").getAsDouble(), o.get("sa2").getAsDouble(), o.get("nsf2").getAsDouble(),
            o.get("ss12").getAsDouble(),
            o.has("ss21") ? o.get("ss21").getAsDouble() : 0.0);
    }

    /** A derived control-rod material by name ("graphite", "b4c"), or null. */
    public static RbmkXsec control(String name) {
        return CONTROLS.get(name);
    }

    private LatticeTable() {}

    public static boolean available() {
        return !ROWS.isEmpty();
    }

    public static double betaEff() {
        return BETA_EFF;
    }

    /** The derived cell constants at a void fraction (0 = water, 1 = steam). */
    public static RbmkXsec cell(double voidFraction) {
        if (ROWS.isEmpty()) {
            return null;
        }
        double v = Math.max(0.0, Math.min(1.0, voidFraction));
        Row lo = ROWS.get(0), hi = ROWS.get(ROWS.size() - 1);
        for (int i = 0; i < ROWS.size() - 1; i++) {
            if (v >= ROWS.get(i).voidFraction() && v <= ROWS.get(i + 1).voidFraction()) {
                lo = ROWS.get(i);
                hi = ROWS.get(i + 1);
                break;
            }
        }
        double span = hi.voidFraction() - lo.voidFraction();
        double t = span > 1e-9 ? (v - lo.voidFraction()) / span : 0.0;
        return new RbmkXsec(
            lerp(lo.d1(), hi.d1(), t), lerp(lo.sr1(), hi.sr1(), t), lerp(lo.nsf1(), hi.nsf1(), t),
            lerp(lo.d2(), hi.d2(), t), lerp(lo.sa2(), hi.sa2(), t), lerp(lo.nsf2(), hi.nsf2(), t),
            lerp(lo.ss12(), hi.ss12(), t), lerp(lo.ss21(), hi.ss21(), t));
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    /** k_inf at a void fraction, for reporting. */
    public static double kInf(double voidFraction) {
        if (ROWS.isEmpty()) {
            return 1.0;
        }
        double v = Math.max(0.0, Math.min(1.0, voidFraction));
        for (int i = 0; i < ROWS.size() - 1; i++) {
            if (v >= ROWS.get(i).voidFraction() && v <= ROWS.get(i + 1).voidFraction()) {
                Row a = ROWS.get(i), b = ROWS.get(i + 1);
                double t = (v - a.voidFraction()) / (b.voidFraction() - a.voidFraction());
                return a.kInf() + t * (b.kInf() - a.kInf());
            }
        }
        return ROWS.get(ROWS.size() - 1).kInf();
    }
}
