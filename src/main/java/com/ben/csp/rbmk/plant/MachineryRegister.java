package com.ben.csp.rbmk.plant;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The machinery register: the completeness AND conformance authority for machine
 * PERFORMANCE — the third leg of the fidelity architecture.
 *
 * <p>The other two legs answer two questions and leave a third open:
 * <ul>
 *   <li>{@code plant.EquipmentRegister} — "does this machine exist?" (the inventory);</li>
 *   <li>{@code plant.MachineValidator} — "is its shell the right shape?" (the geometry);</li>
 *   <li><b>this class</b> — "does it BEHAVE as the real machine did?" (the performance).</li>
 * </ul>
 *
 * <p>That third question is the Director's exact fear: <i>"what if a feedwater pump
 * does not have the same flow rate as it does in reality?"</i> Until now the answer
 * was: a human typed a number into {@code PlantPhysics.java} and nothing could prove
 * where it came from, or that it was still that number. The failure mode is the same
 * one the xenon bug demonstrated — a number that LOOKS real but is unverified.
 *
 * <p>This register closes it. Each machine performance quantity is recorded with a
 * source and tied by name to the {@code PlantPhysics} constant the code uses. The
 * self-test ({@code MachineryRegisterSelfTest}) then reads the constant by reflection
 * and asserts it EQUALS the recorded value, so a performance number cannot silently
 * drift: edit the constant and the test fails until the register is updated to a new
 * SOURCED value. A machine is conformant only when its code value matches its sourced
 * value.
 */
public final class MachineryRegister {

    /** How much a performance value rests on a source. */
    public enum Status {
        /** Cites a primary source, with sourceUrl + quote. */
        VERIFIED,
        /** A reference value not yet confirmed against a primary source. */
        TO_VERIFY,
        /** No source at all; allowed only as a fallback. */
        PLACEHOLDER;

        static Status parse(String s) {
            if (s == null) {
                return PLACEHOLDER;
            }
            return switch (s.trim().toLowerCase().replace('-', '_')) {
                case "verified" -> VERIFIED;
                case "to_verify" -> TO_VERIFY;
                default -> PLACEHOLDER;
            };
        }
    }

    /** One performance quantity of one machine. */
    public record Item(String id, String machine, String quantity, String constant,
                       double value, String unit, Status status, String source,
                       String note) {
        public String describe() {
            return String.format("%s — %s = %.6g %s [%s]", machine, quantity, value, unit,
                status);
        }
    }

    private static final List<Item> ITEMS = load();

    private MachineryRegister() {}

    public static boolean available() {
        return !ITEMS.isEmpty();
    }

    public static int size() {
        return ITEMS.size();
    }

    public static List<Item> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    public static List<Item> byStatus(Status s) {
        List<Item> out = new ArrayList<>();
        for (Item it : ITEMS) {
            if (it.status() == s) {
                out.add(it);
            }
        }
        return out;
    }

    /**
     * Read the live value of the {@code PlantPhysics} constant this item names, by
     * reflection. Returns NaN if the constant does not exist — which is itself a
     * finding (the register names a constant the code no longer has).
     */
    public static double codeValue(Item item) {
        String path = item.constant();
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return Double.NaN;
        }
        String simple = path.substring(dot + 1);
        try {
            Field f = PlantPhysics.class.getDeclaredField(simple);
            if (!Modifier.isStatic(f.getModifiers())) {
                return Double.NaN;
            }
            f.setAccessible(true);
            return f.getDouble(null);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * True if the code's value equals the value recorded here (to a tight tolerance
     * for floating-point). This is the conformance test: a machine whose code value
     * has drifted from its sourced value is NOT conformant.
     */
    public static boolean conformant(Item item) {
        double code = codeValue(item);
        if (Double.isNaN(code)) {
            return false;
        }
        return Math.abs(code - item.value()) <= 1e-9 * Math.max(1.0, Math.abs(item.value()));
    }

    /** Items whose code value has drifted from the recorded (sourced) value. */
    public static List<Item> drifted() {
        List<Item> out = new ArrayList<>();
        for (Item it : ITEMS) {
            if (!conformant(it)) {
                out.add(it);
            }
        }
        return out;
    }

    /** Items recorded as verified, with a source and a quote-grade citation. */
    public static List<Item> verified() {
        return byStatus(Status.VERIFIED);
    }

    /** Items recorded but not yet confirmed against a primary source. */
    public static List<Item> toVerify() {
        return byStatus(Status.TO_VERIFY);
    }

    /** Items with no source — the register's own integrity failures. */
    public static List<Item> unsourced() {
        return byStatus(Status.PLACEHOLDER);
    }

    /** Distinct machines represented. */
    public static List<String> machines() {
        List<String> out = new ArrayList<>();
        for (Item it : ITEMS) {
            if (!out.contains(it.machine())) {
                out.add(it.machine());
            }
        }
        return out;
    }

    /**
     * The completeness + conformance summary. This is what a gate prints: how many
     * machines are recorded, how many are verified, and — the new part — how many
     * have DRIFTED from their recorded value.
     */
    public static String summary() {
        return String.format(
            "machinery register: %d performance quantities across %d machines — "
                + "%d verified, %d to_verify, %d unsourced, %d DRIFTED",
            ITEMS.size(), machines().size(), verified().size(), toVerify().size(),
            unsourced().size(), drifted().size());
    }

    private static List<Item> load() {
        List<Item> out = new ArrayList<>();
        try (InputStream in = MachineryRegister.class.getResourceAsStream("/machinery-register.json")) {
            if (in == null) {
                return out;
            }
            JsonObject root = JsonParser.parseReader(
                new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray items = root.getAsJsonArray("items");
            for (JsonElement e : items) {
                JsonObject o = e.getAsJsonObject();
                out.add(new Item(
                    str(o, "id"),
                    str(o, "machine"),
                    str(o, "quantity"),
                    str(o, "constant"),
                    o.has("value") ? o.get("value").getAsDouble() : Double.NaN,
                    str(o, "unit"),
                    Status.parse(o.has("status") ? o.get("status").getAsString() : null),
                    str(o, "source"),
                    str(o, "note")));
            }
        } catch (Exception e) {
            out.clear();
        }
        return out;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsString() : "";
    }
}
