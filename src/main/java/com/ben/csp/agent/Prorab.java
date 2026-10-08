package com.ben.csp.agent;

import net.minecraft.text.Text;

/**
 * Chief Master / Construction Supervisor (Прораб)
 * 
 * Historical Role: Technical supervisor responsible for construction quality control,
 * schedule adherence, and workforce management at specific worksites. Reports to
 * GlavnyInzhener or NachalnikSnabzheniya depending on the project type.
 * 
 * Key Responsibilities (per OPB-82):
 * - Daily inspection of construction operations
 * - Verification of work quality against technical specifications
 * - Coordination with MasteraDispatcher for task assignments
 * - Reporting incidents to GlavnyInzhener immediately
 */
public class Prorab {
    
    private static final String AGENT_NAME = "Прораб"; // Chief Master in Cyrillic
    
    // Site-specific state
    private double stressLevel = 0.0;           // Affects inspection quality
    private java.util.Set<String> managedSites = new java.util.HashSet<>();
    private int daysWithoutIncident = 0;        // Safety record
    private java.util.Map<String, String> assignedWorkPackages = new java.util.HashMap<>();
    
    /**
     * Initialize Prorab agent with default values
     */
    public Prorab() {
        this.stressLevel = 0.0;
        this.daysWithoutIncident = 0;
    }
    
    /**
     * Get the agent name in Cyrillic for authentic Soviet terminology
     */
    public static Text getAgentName() {
        return Text.literal(AGENT_NAME);
    }
    
    /**
     * Register a construction worksite under supervision
     */
    public void registerSite(String siteId) {
        this.managedSites.add(siteId);
    }
    
    /**
     * Assign a work package to this Prorab's management
     */
    public void assignWorkPackage(String siteId, String workPackageId) {
        this.assignedWorkPackages.put(siteId, workPackageId);
    }
    
    /**
     * Simulate inspection with quality affected by stress level
     * Stress > 0.7 reduces detection probability of defects
     */
    public double simulateInspection(String siteId, boolean hasDefect) {
        // Base inspection accuracy: 95% when unstressed, drops to 60% at max stress
        double accuracy = Math.max(60.0, 95.0 - (this.stressLevel * 35.0));
        double probability = hasDefect ? accuracy : (100.0 - accuracy);
        return (probability >= Math.random() * 100.0) ? 1.0 : 0.0;
    }
    
    /**
     * Update stress level based on workload and incident history
     */
    public void updateStressLevel(double additionalStress) {
        // Stress decays over time (simulated via decay factor)
        double decayedStress = this.stressLevel * 0.9;
        this.stressLevel = Math.min(Math.max(decayedStress + additionalStress, 0.0), 100.0);
    }
    
    /**
     * Record incident and reset safety record
     */
    public void recordIncident() {
        this.daysWithoutIncident = 0;
        this.updateStressLevel(15.0); // Significant stress increase
    }
    
    /**
     * Get current stress level (0-100%)
     */
    public double getStressLevel() {
        return this.stressLevel;
    }
    
    /**
     * Get days without incident safety record
     */
    public int getDaysWithoutIncident() {
        return this.daysWithoutIncident;
    }
    
    /**
     * Check if Prorab is within safe stress limits for inspection duty
     */
    public boolean isWithinSafeLimits() {
        // OPB-82 requires inspectors to maintain < 70% stress for quality control
        return this.stressLevel < 70.0;
    }
    
    /**
     * Get managed worksites count
     */
    public int getManagedSitesCount() {
        return this.managedSites.size();
    }
    
    /**
     * Get assigned work packages for a specific site
     */
    public String getAssignedWorkPackage(String siteId) {
        return this.assignedWorkPackages.get(siteId);
    }
}
