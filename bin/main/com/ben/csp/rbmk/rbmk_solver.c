#include <stdint.h>
#include <math.h>

/**
 * SGB Algorithmic Heritage: High-Performance 3D Laplacian Calculation.
 * 
 * This function implements the 7-point stencil for the Two-Group Diffusion equations.
 * It operates directly on off-heap memory segments provided by Project Panama.
 *
 * @param flux          Pointer to the input flux array (fast or thermal phi).
 * @param output        Pointer to the result buffer where the Laplacian results will be stored.
 * @param neighbors     Pointer to the pre-calculated 7-point neighbor map (int[totalNodes][6]).
 *                      Layout: [Up, Down, North, South, East, West]
 * @param totalNodes    The total number of axial nodes in the mesh.
 */
void compute_laplacian(double* flux, double* output, int* neighbors, int totalNodes) {
    // Optimization: The compiler can vectorize this loop if SIMD is enabled (AVX2/AVX-512)
    for (int i = 0; i < totalNodes; i++) {
        double centerFlux = flux[i];
        double leakage = 0.0;
        
        // The neighbor map stores indices. A value of -1 indicates a boundary.
        int baseNeighborIdx = i * 6;

        for (int d = 0; d < 6; d++) {
            int nIdx = neighbors[baseNeighborIdx + d];

            if (nIdx != -1) {
                // Standard Diffusion: Flow = neighbor - center
                leakage += (flux[nIdx] - centerFlux);
            } else if (d >= 2) {
                // SGB Radial Boundary Condition: 
                // Directions 2-5 are North, South, East, West.
                // We apply an albedo penalty to simulate neutron loss into the reflector.
                leakage -= (centerFlux * 0.05);
            }
            // Note: Axial boundaries (d < 2) are treated as vacuum (zero leakage/current)
            // unless an explicit albedo is required for the top/bottom biological shields.
        }

        output[i] = leakage;
    }
}

/**
 * SGB Algorithmic Heritage: Thermal Spectrum Cross-Section Lookup.
 * Approximates the temperature dependence of thermal cross-sections
 * based on the 1/v law and spectrum hardening in the graphite matrix.
 *
 * @param temp_c        Moderator temperature in Celsius.
 * @param enrichment    Local fuel enrichment percentage (e.g., 2.0).
 * @param sigma_f       Output for macroscopic fission cross-section.
 * @param sigma_a       Output for macroscopic absorption cross-section.
 */
void lookup_cross_sections(double temp_c, double enrichment, double* sigma_f, double* sigma_a) {
    // Reference values at 20C (293.15K) and 2.0% nominal enrichment.
    const double sigma_f0 = 0.12;
    const double sigma_a0 = 0.08;
    
    // SGB: Scaling cross-sections based on local enrichment relative to reference.
    double enrichmentFactor = enrichment / 2.0;

    // SGB: Spectrum Hardening Factor (approximate 1/sqrt(T) dependence)
    double factor = sqrt(293.15 / (temp_c + 273.15));
    
    *sigma_f = sigma_f0 * enrichmentFactor * factor;
    *sigma_a = sigma_a0 * enrichmentFactor * factor;
}

/**
 * SGB Algorithmic Heritage: Native Bateman Solver for I-135/Xe-135 evolution.
 * This implements the analytical solution for the Iodine-Xenon decay chain,
 * accounting for fission production, radioactive decay, and neutron burn-up.
 * 
 * @param phi           Pointer to thermal flux array.
 * @param iodine        Pointer to Iodine-135 concentration array (atoms/cm3).
 * @param xenon         Pointer to Xenon-135 concentration array (atoms/cm3).
 * @param burnup        Pointer to fuel burnup array (MWd/kg).
 * @param graphite_temp Pointer to graphite temperature array (C).
 * @param enrichment    Pointer to fuel enrichment array (%).
 * @param precursors    Pointer to delayed neutron precursor concentration.
 * @param dt            Time step in seconds.
 * @param totalNodes    Total number of nodes in the mesh.
 */
void compute_isotopics(double* phi, double* iodine, double* xenon, double* burnup, double* graphite_temp, double* enrichment, double* precursors, double dt, int totalNodes) {
    // RBMK-1000 Constants (Reference: NIKIET Technical Specs)
    const double lambda_i = 2.874e-5;    // I-135 decay constant (s^-1)
    const double lambda_xe = 2.093e-5;   // Xe-135 decay constant (s^-1)
    const double gamma_i = 0.0639;       // I-135 fission yield
    const double gamma_xe = 0.00237;     // Direct Xe-135 fission yield
    const double sigma_a_xe = 2.65e-18;  // Xe-135 micro absorption (cm^2)
    const double burnup_coeff = 1.157e-8; // Burnup rate coefficient
    
    const double beta = 0.0065;          // Delayed neutron fraction
    const double lambda_pre = 0.08;      // Effective precursor decay constant

    for (int i = 0; i < totalNodes; i++) {
        double p = phi[i];
        double I0 = iodine[i];
        double X0 = xenon[i];
        double B0 = burnup[i];
        double T = graphite_temp[i];
        double E = enrichment[i];
        double C0 = precursors[i];

        // SGB: Dynamic Cross-Section Lookup based on local node temperature and enrichment
        double local_sigma_f, local_sigma_a_fuel;
        lookup_cross_sections(T, E, &local_sigma_f, &local_sigma_a_fuel);

        double SigmaFPhi = local_sigma_f * p;
        double yield_i = gamma_i * SigmaFPhi;
        double lambda_xe_eff = lambda_xe + (sigma_a_xe * p);

        // SGB: Analytical solution for Iodine-135
        double exp_i = exp(-lambda_i * dt);
        double I1 = I0 * exp_i + (yield_i / lambda_i) * (1.0 - exp_i);

        // SGB: Analytical solution for Xenon-135 (Bateman Formula)
        double exp_xe = exp(-lambda_xe_eff * dt);
        double term1 = X0 * exp_xe;
        double term2 = ((gamma_i + gamma_xe) * SigmaFPhi / lambda_xe_eff) * (1.0 - exp_xe);
        double term3 = (lambda_i * I0 - yield_i) / (lambda_xe_eff - lambda_i) * (exp_i - exp_xe);
        
        double X1 = term1 + term2 + term3;

        // SGB: Delayed Neutron Precursor Evolution
        // dC/dt = beta * SigmaF * Phi - lambda * C
        double yield_c = beta * SigmaFPhi;
        double exp_c = exp(-lambda_pre * dt);
        double C1 = C0 * exp_c + (yield_c / lambda_pre) * (1.0 - exp_c);

        // Update UGPB
        iodine[i] = I1;
        xenon[i] = X1;
        burnup[i] = B0 + (p * dt * burnup_coeff);
        precursors[i] = C1;
    }
}

/**
 * SGB Algorithmic Heritage: Native Control Rod Solver.
 * Models the RBMK-1000 rod geometry: 5m Absorber (B4C), 4.5m Displacer (Graphite).
 * This logic calculates the "Tip Effect" by evaluating the axial position of
 * the absorber and displacer relative to the 24 core nodes.
 * 
 * @param rod_penalty_mesh Pointer to the 3D absorption penalty mesh (output).
 * @param rod_depths       Pointer to the 211 rod depths (0.0 to 1.0).
 * @param rod_channels     Pointer to the 211 channel indices for these rods.
 * @param num_rods         Number of rods (211).
 * @param total_nodes      Total nodes in the mesh (channelCount * 24).
 * @param nodes_per_ch     Axial nodes per channel (24).
 */
void compute_control_rods(double* rod_penalty_mesh, double* rod_depths, int* rod_channels, int num_rods, int total_nodes, int nodes_per_ch) {
    // Zero out the mesh first
    for (int i = 0; i < total_nodes; i++) rod_penalty_mesh[i] = 0.0;

    // RBMK-1000 Physics Constants
    const double SIGMA_A_B4C = 0.85;      // Strong Boron Carbide absorption
    const double SIGMA_A_WATER = 0.015;    // Penalty relative to graphite moderator
    const double SIGMA_A_GRAPHITE = -0.01; // Reactivity bonus (displacer)

    const double CORE_HEIGHT = 7.0;       // meters

    for (int r = 0; r < num_rods; r++) {
        int cIdx = rod_channels[r];
        if (cIdx < 0 || cIdx >= (total_nodes / nodes_per_ch)) continue;

        double depth = rod_depths[r]; 
        
        // Calculate positions in meters from the core bottom (0.0 to 7.0)
        // RBMK rods enter from the top. depth 1.0 = fully inserted.
        double absorber_bottom = CORE_HEIGHT - (depth * CORE_HEIGHT);
        double displacer_top = absorber_bottom;
        double displacer_bottom = displacer_top - 4.5; // 4.5m Graphite Follower

        for (int z = 0; z < nodes_per_ch; z++) {
            double node_center_m = (z + 0.5) * (CORE_HEIGHT / nodes_per_ch);
            int nodeIdx = (cIdx * nodes_per_ch) + z;

            if (node_center_m > absorber_bottom) {
                // Absorber section is present
                rod_penalty_mesh[nodeIdx] = SIGMA_A_B4C;
            } else if (node_center_m > displacer_bottom && node_center_m < displacer_top) {
                // Graphite displacer is present (less absorption than water)
                rod_penalty_mesh[nodeIdx] = SIGMA_A_GRAPHITE;
            } else {
                // Water column is present in the channel
                rod_penalty_mesh[nodeIdx] = SIGMA_A_WATER;
            }
        }
    }
}