package com.ben.csp.rbmk;

/**
 * The RBMK-1000 core at 1:4 — one Minecraft block = 0.25 m, so that one lattice
 * cell is exactly one block and every graphite block, fuel channel (ТК) and
 * control channel (СУЗ) is its own block. This is the whole point of the RBMK:
 * it is not one reactor but ~1,661 fuel channels sharing one neutron field, each
 * with its own coolant and its own fuel, coupled only through the flux.
 *
 * <p>Every real dimension is quoted in metres (see docs/RBMK-CORE-GEOMETRY.md)
 * and converted here, once, so nothing downstream hard-codes a scale.
 */
public final class RbmkGeometry {

    private RbmkGeometry() {}

    // ---------------------------------------------------------------- scale
    /** 1:4 — one block is a quarter of a metre. */
    public static final double METRES_PER_BLOCK = 0.25;

    public static int toBlocks(double metres) {
        return (int) Math.round(metres / METRES_PER_BLOCK);
    }

    public static double toMetres(int blocks) {
        return blocks * METRES_PER_BLOCK;
    }

    // -------------------------------------------------------------- lattice
    /** Square lattice pitch, 250 mm — the figure the whole core hangs off. */
    public static final double LATTICE_PITCH_M = 0.250;
    public static final int LATTICE_PITCH_BLOCKS = 1;

    /** Graphite moderator block: 250 x 250 mm in plan. */
    public static final double GRAPHITE_BLOCK_M = 0.250;
    /** The 114 mm hole drilled through each graphite block. */
    public static final double CHANNEL_HOLE_M = 0.114;
    /**
     * Pressure tube (ТК), Zr + 2.5 % Nb in core. The standard designation is
     * "88 x 4" -- Russian tube notation is outer diameter x wall -- so the bore is
     * 80 mm. INSAG-7 §11 states the channels were "tubes 80 mm in diameter",
     * which agrees. Earlier revisions read 88 mm as the bore, an 8 mm error.
     */
    public static final double PRESSURE_TUBE_ID_M = 0.080;
    public static final double PRESSURE_TUBE_WALL_M = 0.004;

    // ------------------------------------------------- active core / stack
    public static final double CORE_DIAMETER_M = 11.8;
    public static final double CORE_HEIGHT_M = 7.0;
    public static final int CORE_DIAMETER_BLOCKS = toBlocks(CORE_DIAMETER_M); // 47
    public static final int CORE_HEIGHT_BLOCKS = toBlocks(CORE_HEIGHT_M);     // 28
    /**
     * The channel region: the 12.25 m circle (49 blocks) that holds the 1,872
     * channels. The active region (11.8 m) is smaller than the channel region, and
     * a 250 mm lattice circle only reaches 1,872 channels at 12.25 m — see
     * docs/RBMK-PLANT-SYSTEMS.md §1a. The 11.8 m figure is the fuelled core.
     */
    public static final double CHANNEL_REGION_DIAMETER_M = 12.25;
    public static final int CHANNEL_REGION_DIAMETER_BLOCKS = toBlocks(CHANNEL_REGION_DIAMETER_M); // 49
    /** The graphite stack (moderator + radial reflector): 14 m circle, 56 blocks. */
    public static final double STACK_DIAMETER_M = 14.0;
    public static final double STACK_HEIGHT_M = 8.0;
    public static final int STACK_DIAMETER_BLOCKS = toBlocks(STACK_DIAMETER_M); // 56
    public static final int STACK_HEIGHT_BLOCKS = toBlocks(STACK_HEIGHT_M);     // 32

    // ------------------------------------- shielding and vessel (Schema E etc.)
    /** Upper biological shield, "Схема Е": Ø17 m disc, 3 m thick, ~2000 t. */
    public static final double UBS_DIAMETER_M = 17.0;
    public static final double UBS_THICKNESS_M = 3.0;
    public static final int UBS_DIAMETER_BLOCKS = toBlocks(UBS_DIAMETER_M);     // 68
    public static final int UBS_THICKNESS_BLOCKS = toBlocks(UBS_THICKNESS_M);   // 12
    /** Lower biological shield: Ø14.5 m, 2 m thick. */
    public static final double LBS_DIAMETER_M = 14.5;
    public static final double LBS_THICKNESS_M = 2.0;
    public static final int LBS_DIAMETER_BLOCKS = toBlocks(LBS_DIAMETER_M);     // 58
    public static final int LBS_THICKNESS_BLOCKS = toBlocks(LBS_THICKNESS_M);   // 8
    /** Reactor vessel: annular steel cylinder, inner Ø14.5 m, 9.7 m tall. */
    public static final double VESSEL_INNER_DIAMETER_M = 14.5;
    public static final double VESSEL_HEIGHT_M = 9.7;
    public static final int VESSEL_INNER_DIAMETER_BLOCKS = toBlocks(VESSEL_INNER_DIAMETER_M); // 58
    public static final int VESSEL_HEIGHT_BLOCKS = toBlocks(VESSEL_HEIGHT_M);   // 39
    /** Annular side water tank: inner Ø16.6 m, outer Ø19 m, 16 compartments. */
    public static final double WATER_TANK_INNER_DIAMETER_M = 16.6;
    public static final double WATER_TANK_OUTER_DIAMETER_M = 19.0;
    public static final int WATER_TANK_COMPARTMENTS = 16;

    // ------------------------------------------------------------- channels
    /** Second-generation (ChNPP 3/4): 1,661 fuel + 211 control channels. */
    public static final int FUEL_CHANNELS = 1661;
    public static final int CONTROL_CHANNELS = 211;
    public static final int TOTAL_CHANNELS = FUEL_CHANNELS + CONTROL_CHANNELS; // 1872

    // -------------------------------------------------------- fuel assembly
    public static final double TVS_LENGTH_M = 10.025;
    public static final double TVS_ACTIVE_M = 6.862;
    public static final int TVS_SUBASSEMBLIES = 2;          // lower and upper
    public static final int TVS_RODS_PER_SUBASSEMBLY = 18;
    public static final double FUEL_ROD_OD_M = 0.0136;
    public static final double FUEL_ROD_LENGTH_M = 3.64;

    // --------------------------------------------------------- control rod
    // Pre-1986 (2nd generation) rod, as built. Source: NIKIET's own design book,
    // Abramov, Avdeev, Adamov et al., "Канальный энергетический реактор РБМК",
    // ed. Yu.M. Cherkashov (GUP NIKIET) — verbatim in
    // docs/primary-sources/nikiet-rod-geometry-quote.txt:
    //   "Стержни АР, ЛАР ... состоят из поглотителя длиной 6772 мм,
    //    телескопического соединения и вытеснителя длиной 4560 мм,
    //    ход стержней - 6550 мм."
    /** Graphite displacer at the rod foot: 4.56 m (NIKIET: 4560 mm). */
    public static final double ROD_DISPLACER_M = 4.56;
    /**
     * Telescope: the water-filled gap above the displacer. NIKIET does not state
     * it; RU Wikipedia gives the withdrawn rod a 1.25 m water gap at each end.
     * Flagged to verify against the rod drawing.
     */
    public static final double ROD_TELESCOPE_M = 1.25;
    /** B4C absorber: 6.772 m (NIKIET: 6772 mm). Shorter than the 7 m core. */
    public static final double ROD_ABSORBER_M = 6.772;
    /**
     * Absorber clad outer diameter, 70 mm. Source: DOE/NE-0076 Table G-2
     * ({@code rod.diameters}) — "Outer Clad Diameter 7.0 cm", B4C 65 mm OD / 57.5 mm ID
     * inside a 2 mm clad with a 50 mm bore cooling passage.
     *
     * <p>This is the figure the model was missing. The rod is NOT a plug filling its
     * channel: it is Ø70 mm inside the Ø114 mm bore, so it occupies 6.2 % of the
     * 250 mm cell while water fills the remaining 10.2 % of the bore. Treating the
     * whole bore as absorber (as {@link #ROD_ABSORBER_M} alone implied) overstated
     * rod absorption by 2.65x — the same class of error as the earlier hand-set
     * 0.75/0.25 split, which was 1.24x the bore.
     */
    public static final double ROD_ABSORBER_OD_M = 0.070;
    /** Graphite displacer clad outer diameter, 74 mm (DOE/NE-0076 Table G-2). */
    public static final double ROD_DISPLACER_OD_M = 0.074;
    /**
     * Rod travel, 6.55 m (NIKIET: 6550 mm) — NOT the 7 m core height. A rod
     * therefore never fully clears the core, which is why a withdrawn rod still
     * leaves 1.25 m of water below its displacer.
     */
    public static final double ROD_TRAVEL_M = 6.55;
    /**
     * Full-stroke insertion time, 17.5 s — the mid-point of the panel's 17-18 s
     * ({@code rod.travel}). Two sources disagree and the disagreement is recorded,
     * not papered over ({@code rod.travel}, {@code rod.insert_s_insag7}):
     * <ul>
     *   <li>the panel table: 17-18 s for PK-AZ/AR/RR, 6-7 s for AZ;</li>
     *   <li>INSAG-7 p.51: "the same for all the RCPS rods (18-21 s)".</li>
     * </ul>
     * What both agree on is the 0.4 m/s drive rate (INSAG-7 states it three times).
     * Over the 7 m core height 0.4 m/s gives 17.5 s, which is this value; over the
     * 6.55 m travel it gives 16.4 s. The panel time is used because it is the
     * Russian figure and this model prefers the primary source; the 18-21 s band is
     * probably the same stroke plus a slow final seating phase.
     */
    public static final double ROD_FULL_TRAVEL_S = 17.5;
    /** Average rod speed over the 6.55 m travel: 6.55 / 17.5 = 0.374 m/s. */
    public static final double ROD_SPEED_M_PER_S = ROD_TRAVEL_M / ROD_FULL_TRAVEL_S;
    /**
     * Rod drive rate in units of INSERTION per second, which is what the core models
     * integrate. One full stroke of the travel is one unit of insertion, so this is
     * simply 1/17.5. It must not be computed as speed/core height: the travel (6.55 m)
     * is shorter than the core (7 m), and mixing the two gives 18.7 s per stroke.
     */
    public static final double ROD_INSERTION_PER_S = 1.0 / ROD_FULL_TRAVEL_S;
    public static final int RODS_FROM_BELOW = 24;                       // УСП, axial shaping
    public static final int RODS_FROM_ABOVE = CONTROL_CHANNELS - RODS_FROM_BELOW; // 187

    /** The material filling one axial node of a control channel. */
    public enum NodeMaterial { WATER, GRAPHITE, B4C }

    /**
     * The material in each axial core node for a control rod at the given insertion
     * (0 = fully withdrawn, 1 = fully inserted).
     *
     * <p>From its top down, the rod is: the B4C absorber, the 1.25 m telescope (a
     * water gap), and the 4.56 m graphite displacer at its foot. Withdrawn, the
     * displacer occupies 1.19 m to 5.75 m, so it sits mid-core with the telescope
     * above it and 1.19 m of water below. Driving the rod in moves the displacer
     * down through the core, replacing water (a weak absorber) with graphite (almost
     * none) — a local <em>rise</em> in reactivity before the absorber arrives.
     * That is the "positive scram" effect, and it is pure geometry: a rod modelled
     * as a bare absorber cannot reproduce it.
     */
    public static NodeMaterial[] rodMaterials(double insertion) {
        double d = clamp01(insertion) * ROD_TRAVEL_M;
        double displacerTop = 5.75 - d;                        // withdrawn: 1.19..5.75
        double displacerBottom = displacerTop - ROD_DISPLACER_M;
        double telescopeTop = displacerTop + ROD_TELESCOPE_M;  // withdrawn: 5.75..7.0
        double absorberBottom = telescopeTop;
        double absorberTop = absorberBottom + ROD_ABSORBER_M;

        int nodes = CORE_HEIGHT_CELLS;
        double nodeH = CORE_HEIGHT_M / nodes;
        NodeMaterial[] out = new NodeMaterial[nodes];
        for (int z = 0; z < nodes; z++) {
            double h = (z + 0.5) * nodeH; // node centre, metres above the core bottom
            if (h >= displacerBottom && h <= displacerTop) {
                out[z] = NodeMaterial.GRAPHITE;
            } else if (h >= absorberBottom && h <= absorberTop) {
                out[z] = NodeMaterial.B4C;
            } else {
                out[z] = NodeMaterial.WATER;
            }
        }
        return out;
    }

    /** How far a rod has travelled, in blocks, after {@code seconds} at rod speed. */
    public static double rodTravelBlocks(double seconds) {
        return toBlocks(ROD_SPEED_M_PER_S * seconds);
    }

    // ------------------------------------------------------- lattice cells
    // The physics is meshed in LATTICE CELLS (one node per 250 mm lattice cell),
    // NOT in drawn blocks. The two counts coincide only while
    // METRES_PER_BLOCK == LATTICE_PITCH_M (true at 1:4 today). The build scale may
    // change (1:1 bit-scale later); the physics must not. So the physics reads
    // *CELLS and the world builder reads *BLOCKS. Never cross them.
    //
    // These are declared after every *_M constant so their initialisers see the
    // real metres, not zero.

    /** Count of lattice cells across a metre dimension. */
    public static int toCells(double metres) {
        return (int) Math.round(metres / LATTICE_PITCH_M);
    }

    /** Active core height: 7 m / 250 mm = 28 lattice cells. */
    public static final int CORE_HEIGHT_CELLS = toCells(CORE_HEIGHT_M);
    /** Active core diameter: 11.8 m / 250 mm = 47 lattice cells. */
    public static final int CORE_DIAMETER_CELLS = toCells(CORE_DIAMETER_M);
    /** Channel region: 12.25 m / 250 mm = 49 lattice cells. */
    public static final int CHANNEL_REGION_DIAMETER_CELLS = toCells(CHANNEL_REGION_DIAMETER_M);
    /** Graphite stack: 14 m / 250 mm = 56 lattice cells. */
    public static final int STACK_DIAMETER_CELLS = toCells(STACK_DIAMETER_M);

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
