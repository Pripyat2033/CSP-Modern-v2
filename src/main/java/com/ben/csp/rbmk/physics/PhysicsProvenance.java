package com.ben.csp.rbmk.physics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provenance for every numeric constant the RBMK model rests on, loaded from
 * {@code /provenance.json}.
 *
 * <p>Mission rule 2: <em>nothing is guessed</em>. A dimension or parameter either
 * cites a source or is marked as not yet known — it is never silently a number.
 * This registry is that marking, in the data, for the physics constants.
 *
 * <p>Three states:
 * <ul>
 *   <li>{@link Status#VERIFIED} — taken from a cited source.</li>
 *   <li>{@link Status#TO_VERIFY} — a reference value not yet confirmed against a
 *       primary (NIKIET / design) source. Usable, but flagged.</li>
 *   <li>{@link Status#PLACEHOLDER} — a value with no source at all. Allowed only
 *       when it is a {@code fallback} that is unused while real data is present.</li>
 * </ul>
 *
 * <p>The build gate ({@code checkProvenance} in {@code build.gradle}) warns on any
 * {@code TO_VERIFY} and fails on any non-fallback {@code PLACEHOLDER}, so a
 * guessed number cannot quietly become "fact". This is the machine-checkable form
 * of the Director's standing concern: a summary that was true once must not be
 * treated as true forever.
 */
public final class PhysicsProvenance {

    /** How much a constant rests on a source. */
    public enum Status {
        VERIFIED, TO_VERIFY, PLACEHOLDER;

        static Status parse(String s) {
            if (s == null) {
                return PLACEHOLDER;
            }
            return switch (s.trim().toLowerCase()) {
                case "verified" -> VERIFIED;
                case "to_verify" -> TO_VERIFY;
                default -> PLACEHOLDER;
            };
        }
    }

    /** One registered constant. {@code value} is NaN for a non-scalar entry. */
    public record Entry(String key, double value, String unit, Status status,
                        boolean fallback, String source, String sourceUrl, String quote) {
        public boolean unresolved() {
            return status == Status.PLACEHOLDER && !fallback;
        }
    }

    private static final Map<String, Entry> ENTRIES = load();

    private PhysicsProvenance() {}

    /** True if the registry resource loaded. */
    public static boolean available() {
        return !ENTRIES.isEmpty();
    }

    public static int size() {
        return ENTRIES.size();
    }

    /** The entry for a key, or null if it is not registered. */
    public static Entry get(String key) {
        return ENTRIES.get(key);
    }

    public static Status status(String key) {
        Entry e = ENTRIES.get(key);
        return e == null ? null : e.status();
    }

    public static boolean isVerified(String key) {
        return status(key) == Status.VERIFIED;
    }

    /** Keys that are reference values not yet confirmed against a source. */
    public static List<String> toVerify() {
        List<String> out = new ArrayList<>();
        for (Entry e : ENTRIES.values()) {
            if (e.status() == Status.TO_VERIFY) {
                out.add(e.key());
            }
        }
        return out;
    }

    /** Keys that are taken from a cited primary source. */
    public static List<String> verified() {
        List<String> out = new ArrayList<>();
        for (Entry e : ENTRIES.values()) {
            if (e.status() == Status.VERIFIED) {
                out.add(e.key());
            }
        }
        return out;
    }

    /**
     * Keys that are placeholders with no source AND no fallback justification —
     * the values the build gate refuses. An empty list means no guessed number is
     * in the model.
     */
    public static List<String> unresolvedPlaceholders() {
        List<String> out = new ArrayList<>();
        for (Entry e : ENTRIES.values()) {
            if (e.unresolved()) {
                out.add(e.key());
            }
        }
        return out;
    }

    private static Map<String, Entry> load() {
        Map<String, Entry> map = new LinkedHashMap<>();
        try (InputStream in = PhysicsProvenance.class.getResourceAsStream("/provenance.json")) {
            if (in == null) {
                return map;
            }
            JsonObject root = JsonParser.parseReader(
                new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject entries = root.getAsJsonObject("entries");
            for (Map.Entry<String, JsonElement> e : entries.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                JsonElement v = o.get("value");
                double value = (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber())
                    ? v.getAsDouble() : Double.NaN;
                map.put(e.getKey(), new Entry(
                    e.getKey(),
                    value,
                    o.has("unit") ? o.get("unit").getAsString() : "",
                    Status.parse(o.has("status") ? o.get("status").getAsString() : null),
                    o.has("fallback") && o.get("fallback").getAsBoolean(),
                    o.has("source") ? o.get("source").getAsString() : "",
                    o.has("sourceUrl") ? o.get("sourceUrl").getAsString() : "",
                    o.has("quote") ? o.get("quote").getAsString() : ""));
            }
        } catch (Exception e) {
            map.clear();
        }
        return map;
    }
}
