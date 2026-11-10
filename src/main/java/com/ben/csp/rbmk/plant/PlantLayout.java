package com.ben.csp.rbmk.plant;

/**
 * The plant layout at 1:4, in blocks (1 block = 0.25 m), relative to the core
 * centre. The machines sit where the real ones sit: the steam separator drums in
 * side compartments above the reactor, the turbine hall alongside, the condensers
 * under the turbines, the pumps and ECCS around the core
 * (docs/RBMK-PLANT-SYSTEMS.md §9).
 *
 * <p>Every dimension is a real one divided by four: a 2.8 m drum is 11 blocks
 * across, a 31 m drum is 124 blocks long, a 39 m turbogenerator is 156 blocks.
 */
public final class PlantLayout {

    private PlantLayout() {}

    public static final int SEPARATOR_DIAMETER_BLOCKS = (int) Math.round(
        PlantPhysics.SEPARATOR_DIAMETER_M / 0.25); // 11
    public static final int SEPARATOR_LENGTH_BLOCKS = (int) Math.round(
        PlantPhysics.SEPARATOR_LENGTH_M / 0.25);   // 124
    /** Distance from the core centre to the separator rows (m -> blocks). */
    public static final int SEPARATOR_OFFSET_BLOCKS = 40;
    public static final int SEPARATOR_HEIGHT_BLOCKS = 44;

    /** Turbogenerator: ~39 m long, ~12 m across. */
    public static final int TURBINE_LENGTH_BLOCKS = 156;
    public static final int TURBINE_WIDTH_BLOCKS = 48;
    public static final int TURBINE_HEIGHT_BLOCKS = 40;
    /** The machine hall is offset from the core along +x. */
    public static final int TURBINE_HALL_OFFSET_BLOCKS = 120;

    public static final int CONDENSER_LENGTH_BLOCKS = 80;
    public static final int CONDENSER_WIDTH_BLOCKS = 48;
    public static final int CONDENSER_HEIGHT_BLOCKS = 20;

    public static final int MCP_SIZE_BLOCKS = 3;
    public static final int MCP_RING_RADIUS_BLOCKS = 36;

    public static final int DEAERATOR_SIZE_BLOCKS = 24;
    public static final int DEAERATOR_HEIGHT_BLOCKS = 40;

    public static final int ACCUMULATOR_SIZE_BLOCKS = 3;
    public static final int ACCUMULATOR_HEIGHT_BLOCKS = 6;
}
