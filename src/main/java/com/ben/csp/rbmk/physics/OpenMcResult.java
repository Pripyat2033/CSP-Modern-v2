package com.ben.csp.rbmk.physics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The result of an external OpenMC calculation, loaded from
 * {@code /openmc_void_coeff.json}.
 *
 * <p>This is the "mechanics" half of the split: the neutron physics is computed
 * by OpenMC (a real Monte Carlo transport code, ENDF/B-VIII.0 cross-sections)
 * in {@code openmc/rbmk_void_coeff.py}, and Minecraft only <em>displays</em> it.
 * Nothing here is invented — the numbers are read straight from the OpenMC run.
 *
 * <p>The quantity that matters is the coolant-void reactivity coefficient: the
 * RBMK's defining positive feedback. The coefficient is a derivative, so the
 * sweep is kept and read at the operating point rather than collapsed to a
 * single misleading secant (the lattice's k rises as the channel voids, then
 * falls once the water — itself a moderator — is gone).
 *
 * <p>Independent result, no tabulated value fed in: +0.015 to +0.019 dk/k per
 * unit void, against the documented INSAG-7 / NUREG-1250 value of +0.02.
 */
public final class OpenMcResult {

    /** One point of the coolant-density sweep. */
    public record Point(double density, double voidFraction, double k, double sigma) {}

    private static final JsonObject ROOT = load();

    private OpenMcResult() {}

    /** True if an OpenMC result resource is present and parsed. */
    public static boolean available() {
        return ROOT != null;
    }

    /** The operating-point void coefficient, dk/k per unit void. */
    public static double voidCoeffOperating() {
        return get("void_coeff_operating");
    }

    /** "positive" or "negative" — the sign of the operating-point coefficient. */
    public static String sign() {
        return ROOT != null && ROOT.has("sign") ? ROOT.get("sign").getAsString() : "unknown";
    }

    public static double kFullWater() {
        return get("k_full_water");
    }

    public static double kFullVoid() {
        return get("k_full_void");
    }

    public static String note() {
        return ROOT != null && ROOT.has("note") ? ROOT.get("note").getAsString() : "";
    }

    /** The coolant-density sweep, for display. */
    public static List<Point> sweep() {
        List<Point> out = new ArrayList<>();
        if (ROOT == null || !ROOT.has("sweep")) {
            return out;
        }
        JsonArray arr = ROOT.getAsJsonArray("sweep");
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            out.add(new Point(
                o.get("density").getAsDouble(),
                o.get("void_fraction").getAsDouble(),
                o.get("k").getAsDouble(),
                o.get("sigma").getAsDouble()));
        }
        return out;
    }

    /**
     * k-infinity at a given void fraction, linearly interpolated between the
     * computed sweep points. Lets the visualisation drive the display from the
     * OpenMC curve rather than a hard-coded number.
     */
    public static double kAtVoid(double voidFraction) {
        List<Point> pts = sweep();
        if (pts.isEmpty()) {
            return Double.NaN;
        }
        double v = Math.max(0.0, Math.min(1.0, voidFraction));
        if (v <= pts.get(0).voidFraction()) {
            return pts.get(0).k();
        }
        for (int i = 1; i < pts.size(); i++) {
            Point a = pts.get(i - 1);
            Point b = pts.get(i);
            if (v <= b.voidFraction()) {
                double t = (v - a.voidFraction()) / (b.voidFraction() - a.voidFraction());
                return a.k() + t * (b.k() - a.k());
            }
        }
        return pts.get(pts.size() - 1).k();
    }

    private static double get(String key) {
        if (ROOT == null || !ROOT.has(key) || !ROOT.get(key).isJsonPrimitive()) {
            return Double.NaN;
        }
        return ROOT.get(key).getAsDouble();
    }

    private static JsonObject load() {
        try (InputStream in = OpenMcResult.class.getResourceAsStream("/openmc_void_coeff.json")) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(
                new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}
