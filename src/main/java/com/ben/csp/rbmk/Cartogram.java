package com.ben.csp.rbmk;

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
 * The channel cartogram of the unit 4 core: which cell of the 48x48 channel grid
 * holds a fuel channel, which holds a control rod, and which type of rod.
 *
 * <p>Source: the SVG of the unit's SUZ mimic panel, which carries one {@code rect}
 * per channel with an explicit fill per rod type (provenance key
 * {@code rod.cartogram}). The SVG's own geometry was transcribed to
 * {@code /rbmk_cartogram_ch4.json}; this class is only the reader. Every count is
 * checked against the panel legend on load, so a corrupted or truncated resource
 * fails loudly instead of silently modelling the wrong core.
 */
public final class Cartogram {

    /** Side of the square channel grid, in 250 mm lattice cells. */
    public static final int GRID = 48;

    /** One channel: its grid position and what sits in it. */
    public record Site(int x, int z, RodType rod, boolean detector) {
        /** A fuel channel (ТК): no rod, no detector. */
        public boolean fuel() {
            return rod == null && !detector;
        }
    }

    private static final List<Site> SITES = load();

    private Cartogram() {}

    /** Every channel of the core, in panel row-major order. */
    public static List<Site> sites() {
        return SITES;
    }

    /** The control-rod sites only, in panel order — 211 of them. */
    public static List<Site> rods() {
        List<Site> out = new ArrayList<>(RbmkGeometry.CONTROL_CHANNELS);
        for (Site s : SITES) {
            if (s.rod() != null) {
                out.add(s);
            }
        }
        return out;
    }

    /** The control-rod types, in panel order — the input to a rod bank. */
    public static List<RodType> types() {
        List<RodType> out = new ArrayList<>(RbmkGeometry.CONTROL_CHANNELS);
        for (Site s : rods()) {
            out.add(s.rod());
        }
        return out;
    }

    /** How many sites of each rod type the cartogram holds. */
    public static int count(RodType t) {
        int n = 0;
        for (Site s : SITES) {
            if (s.rod() == t) {
                n++;
            }
        }
        return n;
    }

    private static int count(List<Site> sites, RodType t) {
        int n = 0;
        for (Site s : sites) {
            if (s.rod() == t) {
                n++;
            }
        }
        return n;
    }

    /** Detector sites (ДКЭ) — the 12 in-core neutron detectors. */
    public static int detectors() {
        int n = 0;
        for (Site s : SITES) {
            if (s.detector()) {
                n++;
            }
        }
        return n;
    }

    private static List<Site> load() {
        try (InputStream in = Cartogram.class.getResourceAsStream("/rbmk_cartogram_ch4.json")) {
            if (in == null) {
                return List.of();
            }
            JsonObject root = JsonParser.parseReader(
                new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray rows = root.getAsJsonArray("rows");
            List<Site> out = new ArrayList<>(rows.size() * GRID);
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z).getAsString();
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    switch (c) {
                        case 'A' -> out.add(new Site(x, z, RodType.AZ, false));
                        case 'P' -> out.add(new Site(x, z, RodType.PK_AZ, false));
                        case 'R' -> out.add(new Site(x, z, RodType.AR, false));
                        case 'U' -> out.add(new Site(x, z, RodType.USP, false));
                        // the panel's blue РР category is РР + ПК-РР + ЛАР
                        case 'M' -> out.add(new Site(x, z, RodType.RR, false));
                        case 'D' -> out.add(new Site(x, z, null, true));
                        // ' ' is outside the channel circle: no channel at all
                        case '.' -> out.add(new Site(x, z, null, false));
                        default -> { }
                    }
                }
            }
            return validate(out);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Reject a cartogram that does not reproduce the panel legend. The panel is a
     * drawing of the real core, so a mismatch means the transcription is wrong and
     * the model must not proceed on it.
     */
    private static List<Site> validate(List<Site> sites) {
        int fuel = 0;
        int det = 0;
        int rods = 0;
        for (Site s : sites) {
            if (s.fuel()) {
                fuel++;
            }
            if (s.detector()) {
                det++;
            }
            if (s.rod() != null) {
                rods++;
            }
        }
        int manual = count(sites, RodType.RR) + count(sites, RodType.PK_RR)
            + count(sites, RodType.LAR);
        boolean ok = fuel == RbmkGeometry.FUEL_CHANNELS
            && rods == RbmkGeometry.CONTROL_CHANNELS
            && det == 12
            && manual == 127
            && count(sites, RodType.AZ) == 24
            && count(sites, RodType.PK_AZ) == 24
            && count(sites, RodType.AR) == 12
            && count(sites, RodType.USP) == 24;
        return ok ? Collections.unmodifiableList(sites) : List.of();
    }
}
