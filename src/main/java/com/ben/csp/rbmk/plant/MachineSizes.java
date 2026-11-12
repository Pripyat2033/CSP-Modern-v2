package com.ben.csp.rbmk.plant;

/**
 * The block dimensions of the plant machines, at 1:4 (1 block = 0.25 m).
 *
 * <p>These are kept separate from {@link MachineSpecs} (which attaches a
 * Minecraft {@code Block} to each machine) so that the pure geometry — the
 * energoblok layout and its self-test — can be reasoned about and checked with no
 * Minecraft type in scope. {@link MachineSpecs} builds its specs from these, so
 * there is one source of truth for every dimension.
 */
public final class MachineSizes {

    private MachineSizes() {}

    /** Steam separator drum: 2.8 m dia x 31 m. */
    public static final int SEPARATOR_X = 124;
    public static final int SEPARATOR_Y = 11;
    public static final int SEPARATOR_Z = 11;

    /** Turbogenerator: 5 cylinder covers + generator, ~39 m overall. */
    public static final int TURBINE_X = 157;
    public static final int TURBINE_Y = 27;
    public static final int TURBINE_Z = 19;

    /** Condenser under the turbine. */
    public static final int CONDENSER_X = 80;
    public static final int CONDENSER_Y = 20;
    public static final int CONDENSER_Z = 48;

    /** Main circulating pump (ГЦН) with its 6 kV motor above it. */
    public static final int MCP_X = 3;
    public static final int MCP_Y = 6;
    public static final int MCP_Z = 3;

    /** Deaerator / feedwater storage tank. */
    public static final int DEAERATOR_X = 24;
    public static final int DEAERATOR_Y = 40;
    public static final int DEAERATOR_Z = 24;

    /** ECCS accumulator (САОР). */
    public static final int ECCS_X = 3;
    public static final int ECCS_Y = 6;
    public static final int ECCS_Z = 3;
}
