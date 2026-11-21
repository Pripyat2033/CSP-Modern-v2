package com.ben.csp.rbmk.physics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The phenomena register: the completeness authority for the reactor PHYSICS, the
 * twin of {@code plant.EquipmentRegister}.
 *
 * <p>The equipment register answers "is any machine missing?"; this one answers "is
 * any PHENOMENON missing?". It exists for the same reason, stated by the Director:
 * <i>"if I don't name every machine you won't model it, and I can't list them all."</i>
 * For physics the risk is worse than a missing machine: a missing FEEDBACK or
 * TRANSIENT leaves a reactor that <b>looks</b> right and <b>behaves</b> wrong. That
 * failure has already occurred once — xenon was computed and displayed every step
 * but never entered the neutron balance, so the readout moved and {@code k_eff} did
 * not (see {@code feedback.xenon} in the register).
 *
 * <p>The list is researched from the documents, not dictated: the spine is
 * <b>INSAG-7 section 2</b> ("Features of the reactor", the design features that had
 * a primary influence on the course of the accident), with the Soviet operating
 * regulation (Регламент 1Э-С-II, 1983) and NUREG-1250 for the systems.
 *
 * <p>Like the equipment register this is a <b>gate</b>, not a checklist: it prints
 * how many phenomena are <i>modelled</i> (coupled and measured) versus merely
 * <i>named</i>, so "what is missing" is a printed list rather than a silent
 * omission. The one status that must be watched is {@link Status#READOUT}: a
 * quantity that is computed and shown but NOT coupled — the exact xenon failure
 * mode — so it is surfaced separately.
 */
public final class PhenomenaRegister {

    /** How far a phenomenon has got toward being real in the model. */
    public enum Status {
        /** Coupled into the neutron/thermal balance; a self-test measures it. */
        MODELLED,
        /** Computed and displayed but NOT coupled; cannot change k_eff. A bug. */
        READOUT,
        /**
         * Built and ready to couple, but the data it needs cannot be obtained with the
         * current library or toolchain. Recorded so it cannot be forgotten. This is not
         * a bug and not a backlog item: it is a stated limitation with a named cause.
         */
        BLOCKED_ON_DATA,
        /** Described in a source, not yet built. */
        NAMED_ONLY,
        /** Expected to exist, no source found — MUST be resolved or justified absent. */
        UNSOURCED;

        static Status parse(String s) {
            if (s == null) {
                return UNSOURCED;
            }
            return switch (s.trim().toLowerCase().replace('-', '_')) {
                case "modelled", "modeled" -> MODELLED;
                case "readout" -> READOUT;
                case "blocked_on_data", "blocked" -> BLOCKED_ON_DATA;
                case "named_only", "named" -> NAMED_ONLY;
                default -> UNSOURCED;
            };
        }
    }

    /** One registered phenomenon of the reactor. */
    public record Item(String id, String nameEn, String nameRu, String physicsClass,
                       String source, Status status, String where, String note) {
        public String describe() {
            return String.format("%s (%s) — %s, %s [%s]", nameEn, nameRu, physicsClass,
                status, source);
        }
    }

    private static final List<Item> ITEMS = load();

    private PhenomenaRegister() {}

    /** True if the register resource loaded. */
    public static boolean available() {
        return !ITEMS.isEmpty();
    }

    public static int size() {
        return ITEMS.size();
    }

    /** All phenomena, in register order. */
    public static List<Item> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    /** Phenomena that are coupled and measured. */
    public static List<Item> modelled() {
        return byStatus(Status.MODELLED);
    }

    /** Phenomena computed and displayed but NOT coupled — the xenon failure mode. */
    public static List<Item> readout() {
        return byStatus(Status.READOUT);
    }

    /** Phenomena built but held back by missing data (library or toolchain). */
    public static List<Item> blockedOnData() {
        return byStatus(Status.BLOCKED_ON_DATA);
    }

    /** Phenomena described in a source but not yet built. */
    public static List<Item> namedOnly() {
        return byStatus(Status.NAMED_ONLY);
    }

    /** Phenomena with no source — the register's own integrity failures. */
    public static List<Item> unsourced() {
        return byStatus(Status.UNSOURCED);
    }

    /** Distinct physics classes represented. */
    public static List<String> classes() {
        List<String> out = new ArrayList<>();
        for (Item it : ITEMS) {
            if (!out.contains(it.physicsClass())) {
                out.add(it.physicsClass());
            }
        }
        return out;
    }

    /**
     * The completeness summary: how many phenomena of each status. This is what a
     * gate prints — it turns "is anything missing?" into a number that cannot be
     * quietly zero.
     */
    public static String summary() {
        return String.format(
            "phenomena register: %d phenomena — %d modelled, %d readout (NOT coupled), "
                + "%d blocked on data, %d named-only, %d unsourced",
            ITEMS.size(), modelled().size(), readout().size(), blockedOnData().size(),
            namedOnly().size(), unsourced().size());
    }

    private static List<Item> byStatus(Status s) {
        List<Item> out = new ArrayList<>();
        for (Item it : ITEMS) {
            if (it.status() == s) {
                out.add(it);
            }
        }
        return out;
    }

    private static List<Item> load() {
        List<Item> out = new ArrayList<>();
        try (InputStream in = PhenomenaRegister.class.getResourceAsStream("/phenomena-register.json")) {
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
                    str(o, "name_en"),
                    str(o, "name_ru"),
                    str(o, "class"),
                    str(o, "source"),
                    Status.parse(o.has("status") ? o.get("status").getAsString() : null),
                    str(o, "where"),
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
