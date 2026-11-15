package com.ben.csp.rbmk.plant;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The equipment register: the completeness authority for the energoblok.
 *
 * <p>Every item a real RBMK operator would recognise — every machine, tank,
 * pump, valve, crane, fan and panel — must appear here, with a source. This is
 * the machine-checkable answer to the Director's standing concern: an item he
 * did not name must not silently vanish from the build. The register is sourced
 * from INSAG-7 Table II-II "Main equipment for an RBMK unit", NUREG-1250, and the
 * plant's own <b>Технологический регламент 1Э-С-II (Союзатомэнерго, 1983)</b>, so
 * the list comes from the documents, not from memory.
 *
 * <p>The register is a GATE, not a checklist: {@link #namedOnly()} and
 * {@link #unsourced()} name the items still to be built or resolved, so "what is
 * missing" is a printed list rather than a silent omission. It does not fail the
 * build (the plant is legitimately incomplete), but it must never be empty of
 * findings while the build is unfinished — that would mean the register had
 * drifted out of date, which is the failure mode this class exists to prevent.
 */
public final class EquipmentRegister {

    /** How far an item has got toward being in the world. */
    public enum Status {
        /** Built and validated in-game (has a MachineSpec and is placed). */
        MODELLED,
        /** Has a spec/geometry but is not yet placed, or is a partial build. */
        SPECIFIED,
        /** Named in a source, not yet built. */
        NAMED_ONLY,
        /** Named but with no source — MUST be resolved or removed. */
        UNSOURCED;

        static Status parse(String s) {
            if (s == null) {
                return UNSOURCED;
            }
            return switch (s.trim().toLowerCase().replace('-', '_')) {
                case "modelled", "modeled" -> MODELLED;
                case "specified" -> SPECIFIED;
                case "named_only", "named" -> NAMED_ONLY;
                default -> UNSOURCED;
            };
        }
    }

    /** One registered item of plant equipment. */
    public record Item(String id, String nameRu, String nameEn, String system,
                       String location, int qty, String source, Status status) {
        public String describe() {
            return String.format("%s (%s) — %s, qty %d, %s [%s]",
                nameEn, nameRu, system, qty, status, source);
        }
    }

    private static final List<Item> ITEMS = load();

    private EquipmentRegister() {}

    /** True if the register resource loaded. */
    public static boolean available() {
        return !ITEMS.isEmpty();
    }

    public static int size() {
        return ITEMS.size();
    }

    /** All items, in register order. */
    public static List<Item> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    /** Items that are named in a source but not yet built. */
    public static List<Item> namedOnly() {
        return byStatus(Status.NAMED_ONLY);
    }

    /** Items that have a spec/geometry but are not fully placed. */
    public static List<Item> specified() {
        return byStatus(Status.SPECIFIED);
    }

    /** Items that are built and validated. */
    public static List<Item> modelled() {
        return byStatus(Status.MODELLED);
    }

    /** Items named with no source — the register's own integrity failures. */
    public static List<Item> unsourced() {
        return byStatus(Status.UNSOURCED);
    }

    /** Distinct systems represented in the register. */
    public static List<String> systems() {
        List<String> out = new ArrayList<>();
        for (Item it : ITEMS) {
            if (!out.contains(it.system())) {
                out.add(it.system());
            }
        }
        return out;
    }

    /**
     * The completeness summary: how many items of each status. This is what a
     * gate prints — it turns "is anything missing?" into a number that cannot be
     * quietly zero.
     */
    public static String summary() {
        return String.format(
            "equipment register: %d items — %d modelled, %d specified, %d named-only, %d unsourced",
            ITEMS.size(), modelled().size(), specified().size(),
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
        try (InputStream in = EquipmentRegister.class.getResourceAsStream("/equipment-register.json")) {
            if (in == null) {
                return out;
            }
            JsonObject root = JsonParser.parseReader(
                new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray items = root.getAsJsonArray("items");
            for (JsonElement e : items) {
                JsonObject o = e.getAsJsonObject();
                out.add(new Item(
                    o.get("id").getAsString(),
                    str(o, "name_ru"),
                    str(o, "name_en"),
                    str(o, "system"),
                    str(o, "location"),
                    o.has("qty") ? o.get("qty").getAsInt() : 0,
                    str(o, "source"),
                    Status.parse(o.has("status") ? o.get("status").getAsString() : null)));
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
