package com.ben.csp.rbmk;

import net.minecraft.client.MinecraftClient;

/**
 * REKORD Solver Library - JNA-compatible native interop interface.
 * 
 * SGB: This interface wraps the rbmk_solver native library (rbmk_solver.dylib/.so)
 * and provides Java-accessible functions for RBMK-1000 reactor physics calculations.
 * 
 * Architecture Note: Uses JNA for cross-platform compatibility across Linux/macOS/Windows.
 */
public interface REKORDSolverLibrary {

    // Load native library once per JVM process
    void loadLibrary(String solverPath);

    /**
     * Compute 7-point stencil Laplacian for neutron flux distribution.
     * 
     * SGB: Implements Two-Group Diffusion equation diffusion term: D∇²φ
     * Uses pre-computed neighbor map for boundary conditions.
     */
    void computeLaplacian(double[] flux, double[] output, int[] neighbors, int totalNodes);

    /**
     * Lookup thermal cross-sections based on local node temperature and fuel enrichment.
     * 
     * SGB: Implements 1/v law with spectrum hardening approximation.
     */
    void lookupCrossSections(double tempCelsius, double enrichmentPercent,
                             double[] sigmaF, double[] sigmaA);

    /**
     * Native Bateman solver for I-135/Xe-135 evolution dynamics.
     * 
     * SGB: Implements analytical solution accounting for:
     * - Fission production (I and Xe)
     * - Radioactive decay (λ_I, λ_Xe)
     * - Neutron burn-up of Xe-135 (critical for negative reactivity!)
     * - Delayed neutron precursor evolution
     */
    void computeIsotopics(double[] flux, double[] iodine135, double[] xenon135,
                          double[] burnup, double[] graphiteTemp,
                          double[] enrichment, double[] precursors,
                          double timeStepSeconds, int totalNodes);

    /**
     * Compute control rod worth with graphite tip effect.
     * 
     * SGB: Implements RBMK-1000 geometry (5m B4C absorber + 4.5m graphite displacer).
     * Calculates reactivity insertion per channel based on rod depth.
     */
    void computeControlRods(double[] rodPenaltyMesh, double[] rodDepths,
                            int[] rodChannels, int numRods,
                            int totalNodes, int nodesPerChannel);

    /**
     * Compute two-group neutron kinetics time derivative.
     * 
     * SGB: Implements full point-kinetics equations:
     * ∂φ/∂t = ρ_eff(t)/Λ · φ(t) + Σ_i β_i Λ_i⁻¹ C_i(t) - λ̄ Σ C_i(t)
     */
    void computeNeutronKinetics(double[] phi, double[] precursors,
                                double[] lambdaPre, double timeStepSeconds,
                                double reactivity, double lambdaBar,
                                double generationTimeLambda, double[] newPhi);

    /**
     * Compute thermal-hydraulic cooling model.
     * 
     * SGB: Implements 1D subchannel coolant temperature evolution with boiling.
     */
    void computeThermalHydraulics(double[] inletTemp, double[] coolantDensity,
                                  int totalNodes, double timeStepSeconds);

    /**
     * Compute fuel/clad power density distribution.
     * 
     * SGB: Calculates volumetric heat generation from fission events.
     */
    void computePowerDensity(double[] flux, double[] enrichment, double[] sigmaF,
                             double[] sigmaA, int totalNodes,
                             double[] powerDensity);

}
