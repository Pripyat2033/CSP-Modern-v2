package com.ben.csp.block.entity;

public class RbmkConstants {
    // --- Physics Kinetics ---
    public static final double DELAYED_NEUTRON_FRACTION = 0.0065;
    public static final double PRECURSOR_DECAY_CONSTANT = 0.08;

    // --- Thermal-Hydraulics ---
    public static final double NOMINAL_DRUM_PRESSURE = 6.8; // MPa
    public static final double MCP_PUMP_HEAD_COEFF = 0.05;
    public static final double MIN_MASS_FLOW_THRESHOLD = 1.0;
    public static final double MIXING_SPECIFIC_HEAT_REF_TEMP = 270.0;
    public static final double MIN_SUBCOOLING_MARGIN = 5.0;
    public static final double MIN_FLOW_CAVITATION_PENALTY = 0.1;
    public static final double BASE_HYDRAULIC_RESISTANCE = 0.0005;
    public static final double TWO_PHASE_FRICTION_BASE = 0.0001;
    public static final double TWO_PHASE_FRICTION_EXPONENT = 10.0;
    public static final double FLOW_RELAXATION_COEFF = 0.15;
    public static final double FLUX_TO_POWER_KW = 150.0;
    public static final double RADIAL_HEAT_MIXING_COEFF = 2.5;
    public static final double MAX_VOID_FRACTION = 0.95;
    public static final double QUALITY_TO_VOID_COEFF = 5.0;
    public static final double THERMAL_RESISTANCE_CLAD_FILM = 0.12;
    public static final double THERMAL_RESISTANCE_FUEL_GAP = 0.38;
    public static final double CLAD_TEMP_RELAXATION = 0.15;
    public static final double FUEL_TEMP_RELAXATION = 0.1;
    public static final double ZIRCONIUM_OXIDATION_THRESHOLD = 1200.0;
    public static final double OXIDATION_POWER_BASE = 50.0;
    public static final double OXIDATION_EXP_SCALE = 100.0;
    public static final double OXIDATION_CLAD_FEEDBACK = 0.05;
    public static final double OXIDATION_FUEL_FEEDBACK = 0.02;
    public static final double CLAD_MELTING_POINT = 1500.0;
    public static final double PRESSURE_DROP_FRICTION_BASE = 0.012;
    public static final double PRESSURE_DROP_ACCEL_COEFF = 0.02;
    public static final double GRAPHITE_HEATING_FRACTION = 0.05;
    public static final double GRAPHITE_COOLING_COEFF = 0.02;
    public static final double GRAPHITE_THERMAL_INERTIA = 0.05;
    public static final double TURBINE_FLOW_CAPACITY = 1200.0;
    public static final double SATURATION_TEMP_REFERENCE = 284.5;
    public static final double FEEDWATER_COOLING_CONDENSATION_COEFF = 0.0001;
    public static final double DRUM_CAPACITANCE = 850.0;
    public static final double CONDENSER_PRESSURE = 0.005; // MPa

    // --- Solver Control ---
    public static final double SOLVER_CONVERGENCE_TOLERANCE = 1e-7;
    public static final int SOLVER_MAX_ITERATIONS = 100;
    public static final int SIMULATION_TICK_RATE = 20;
    public static final double TIME_STEP_DELTA = 1.0;

    // --- Core Geometry ---
    public static final int CORE_WIDTH = 45;
    public static final int CORE_DEPTH = 45;
    public static final int CORE_HEIGHT = 24;

    // --- Neutronics Feedback & Constants ---
    public static final double ABS_ZERO_KELVIN = 273.15;
    public static final double REF_TEMP_CELSIUS = 20.0;
    public static final double REF_TEMP_KELVIN = 293.15;
    public static final double DOPPLER_COEFFICIENT = 0.0002;
    public static final double XENON_POISONING_FACTOR = 0.5;
    public static final double VOID_MODERATION_PENALTY = 0.02;
    public static final double GRAPHITE_EXPANSION_COEFF = 0.000015;
    public static final double RUPTURE_THRESHOLD = 0.5;
    public static final double RUPTURE_ABSORPTION_PENALTY = 1.0;
    public static final double RUPTURE_FISSION_PENALTY = 0.05;
    public static final double RUPTURE_DIFFUSION_PENALTY = 5.0;
    public static final double BOUNDARY_ALBEDO_PENALTY = 0.05;
    public static final double INTEGRATION_TIME_STEP = 0.1;

    // --- PRIZMA Algorithm ---
    public static final int PRIZMA_CHANNELS_PER_TICK = 40;
    public static final int PRIZMA_MAX_ITERATIONS = 20;
    public static final double Kq_SAFETY_LIMIT = 1.50;
    public static final double Kz_SAFETY_LIMIT = 1.90;

    // --- Macroscopic Cross-Sections ---
    public static final double MACRO_FISSION_CROSS_SECTION_THERMAL = 0.12;
    public static final double NEUTRONS_PER_FISSION = 2.43;
    public static final double MACRO_ABSORPTION_CROSS_SECTION_FAST = 0.01;
    public static final double MACRO_SCATTER_CROSS_SECTION = 0.04;
    public static final double NEUTRON_DIFFUSION_COEFFICIENT_FAST = 1.2;
    public static final double MACRO_ABSORPTION_CROSS_SECTION_THERMAL = 0.08;
    public static final double CONTROL_ROD_ABSORPTION = 0.85;
    public static final double XENON_ABSORPTION_CROSS_SECTION = 2.65e-18;
    public static final double NEUTRON_DIFFUSION_COEFFICIENT_THERMAL = 0.4;
    public static final double CONTROL_ROD_EFFECTIVENESS = 1.0;
}