package com.ben.csp.rbmk.plant;

/**
 * Physical constants for the RBMK-1000 plant systems, from
 * {@code docs/RBMK-PLANT-SYSTEMS.md}. SI units throughout (W, kg, s, K, Pa).
 *
 * <p>The plant is not one machine. These are the numbers that make each of the
 * separate systems — pumps, drums, turbines, condensers, feedwater — behave as
 * the real plant did, and that let the whole thing close its heat balance.
 */
public final class PlantPhysics {

    private PlantPhysics() {}

    // ---------------------------------------------------------- reactor power
    /** Nominal thermal power, 3,200 MWt (1,000 MWe). */
    public static final double P_THERMAL_W = 3200e6;

    // ------------------------------------------------------ MFC circuit / MCP
    public static final int CIRCUITS = 2;
    public static final int MCP_PER_CIRCUIT = 4;
    /** Pumps running at power (three running, one standby, per circuit). */
    public static final int MCP_RUNNING = 3;
    /** Nominal flow per pump, 8,000 m3/h. */
    public static final double MCP_FLOW_RATED_M3S = 8000.0 / 3600.0;
    /** Throttled flow per pump below 500 MWt (6,000-7,000 m3/h). */
    public static final double MCP_FLOW_THROTTLED_M3S = 6500.0 / 3600.0;
    /** Motor rating, 6 kV, ~6 MW. */
    public static final double MCP_MOTOR_W = 6.0e6;
    /** Flywheel rundown time constant on trip (s). The rundown drove the ECCS. */
    public static final double MCP_COAST_TAU_S = 8.0;

    // ------------------------------------------------------- coolant / circuit
    /** System pressure, 6.9 MPa. */
    public static final double P_SYSTEM_PA = 6.9e6;
    /** Coolant density near saturation at 6.9 MPa (~750 kg/m3). */
    public static final double RHO_COOLANT = 750.0;
    /** Coolant specific heat near 280 C (J/kg/K). */
    public static final double CP_COOLANT = 5.0e3;
    /** Latent heat at 6.9 MPa (J/kg). */
    public static final double H_FG = 1505e3;
    /** Core inlet temperature, 265-270 C. */
    public static final double T_INLET_C = 267.0;
    /** Saturation temperature at 6.9 MPa. */
    public static final double T_SAT_C = 284.0;
    /** Nominal flow per circuit: 3 pumps x 8,000 m3/h. */
    public static final double MCP_FLOW_CIRCUIT_M3S = MCP_RUNNING * MCP_FLOW_RATED_M3S;
    /** Mass flow per circuit (kg/s). */
    public static final double MFLOW_CIRCUIT_KGS = MCP_FLOW_CIRCUIT_M3S * RHO_COOLANT;

    // --------------------------------------------------------- steam separator
    public static final int SEPARATOR_COUNT = 4;
    public static final double SEPARATOR_DIAMETER_M = 2.8;
    public static final double SEPARATOR_LENGTH_M = 31.0;
    public static final double SEPARATOR_WALL_M = 0.10;
    public static final double SEPARATOR_MASS_T = 240.0;
    /** Steam quality leaving the separators, ~15%. */
    public static final double STEAM_QUALITY = 0.15;
    /** Drum level: nominal fill fraction and the trip band. */
    public static final double SEPARATOR_NOMINAL_FILL = 0.5;
    public static final double SEPARATOR_TRIP_HIGH_FILL = 0.72;
    public static final double SEPARATOR_TRIP_LOW_FILL = 0.28;
    /** Cross-sectional area of a drum (m2). */
    public static final double SEPARATOR_AREA_M2 =
        Math.PI * SEPARATOR_DIAMETER_M * SEPARATOR_DIAMETER_M / 4.0;

    // ---------------------------------------------------------------- turbine
    public static final int TURBINE_COUNT = 2;
    public static final double TURBINE_RATED_W = 500e6;
    public static final double TURBINE_RPM = 3000.0;
    /** Steam to power efficiency (derived from the heat balance). */
    public static final double TURBINE_EFFICIENCY = 0.33;
    /** Turbine coastdown time constant on loss of steam (s). */
    public static final double TURBINE_COAST_TAU_S = 20.0;

    // -------------------------------------------------------------- condenser
    /** Condenser cooling water flow per turbine, 82,880 t/h. */
    public static final double COND_COOLING_KGS = 82880e3 / 3600.0;
    /** Cooling water temperature rise (K). */
    public static final double COND_DT_K = 12.0;
    /** Condensate temperature (C). */
    public static final double T_CONDENSATE_C = 40.0;

    // -------------------------------------------------------------- feedwater
    public static final int DEAERATOR_COUNT = 4;
    public static final double T_FEEDWATER_C = 160.0;
    /** Feedwater flow equals steam production at steady state (kg/s). */
    public static final double MFLOW_FEEDWATER_KGS = 1500.0;

    // ---------------------------------------------------------- steam enthalpy
    /** Saturated steam enthalpy at 6.9 MPa (J/kg). */
    public static final double H_STEAM = 2772e3;
    /** Feedwater enthalpy at 160 C (J/kg). */
    public static final double H_FEEDWATER = 676e3;
    /** Condensate enthalpy at 40 C (J/kg). */
    public static final double H_CONDENSATE = 167e3;
    /** Water specific heat (J/kg/K), for the condensate side. */
    public static final double CP_WATER = 4186.0;

    /**
     * Specific work extracted per kg of steam (J/kg), from the rated point:
     * 500 MWe / (nominal steam flow / 2). The honest number — the expansion is
     * not the full latent drop times an efficiency.
     */
    public static final double TURBINE_SPECIFIC_WORK_J_PER_KG =
        TURBINE_RATED_W / (P_THERMAL_W / (H_STEAM - H_FEEDWATER) / TURBINE_COUNT);

    // --------------------------------------------------------------- derived
    /** Steam mass flow for the whole reactor at nominal power (kg/s). */
    public static double steamFlowKgS(double powerFraction) {
        return powerFraction * P_THERMAL_W / (H_STEAM - H_FEEDWATER);
    }

    /** Coolant enthalpy rise across the core for a given power fraction. */
    public static double coreEnthalpyRise(double powerFraction) {
        return powerFraction * P_THERMAL_W / (CIRCUITS * MFLOW_CIRCUIT_KGS);
    }

    /** Core void (steam quality) for a given power fraction, from the enthalpy rise. */
    public static double coreQuality(double powerFraction) {
        double dh = coreEnthalpyRise(powerFraction);
        double x = (dh - CP_COOLANT * (T_SAT_C - T_INLET_C)) / H_FG;
        return Math.max(0, x);
    }
}
