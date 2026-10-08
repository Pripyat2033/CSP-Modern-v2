package com.ben.csp.agent;

import net.minecraft.text.Text;

/**
 * Head of Supply Department (Начальник снабжения)
 * 
 * Historical Role: Manages material supply chains, logistics coordination, and procurement
 * for the construction enterprise. Responsible for ensuring all materials arrive on time
 * according to OPB-82 regulations.
 */
public class NachalnikSnabzheniya {
    
    private static final String AGENT_NAME = "Начальник снабжения"; // Supply Chief in Cyrillic
    
    // Enterprise management state
    private double operationalReadiness = 0.0;
    private int enterpriseTier = 1;
    private java.util.Set<String> managedFacilities = new java.util.HashSet<>();
    private java.util.Map<String, Integer> supplyChainStatus = new java.util.HashMap<>();
    
    public NachalnikSnabzheniya() {
        this.operationalReadiness = 0.0;
        this.enterpriseTier = 1;
        managedFacilities.add("prombaza_main");
    }
    
    public static Text getAgentName() {
        return Text.literal(AGENT_NAME);
    }
    
    public void evaluateTierUpgrade() {
        if (operationalReadiness >= 80.0 && managedFacilities.size() >= 3) {
            this.enterpriseTier = Math.min(this.enterpriseTier + 1, 5);
        }
    }
    
    public void updateReadiness(double facilityContribution) {
        if (this.managedFacilities.size() > 0) {
            double totalScore = 0;
            for (String facility : this.managedFacilities) {
                int status = this.supplyChainStatus.getOrDefault(facility, 10);
                totalScore += status;
            }
            double averageReadiness = totalScore / this.managedFacilities.size();
            this.operationalReadiness = Math.min(this.operationalReadiness + (averageReadiness - this.operationalReadiness) * 0.1, 100.0);
        }
    }
    
    public void registerFacility(String facilityId) {
        if (!this.managedFacilities.contains(facilityId)) {
            this.managedFacilities.add(facilityId);
            this.supplyChainStatus.put(facilityId, 10);
        }
    }
    
    public void updateFacilityStatus(String facilityId, int newStatus) {
        if (this.managedFacilities.contains(facilityId)) {
            this.supplyChainStatus.put(facilityId, newStatus);
        }
    }
    
    public double getOperationalReadiness() {
        return this.operationalReadiness;
    }
    
    public int getEnterpriseTier() {
        return this.enterpriseTier;
    }
    
    public boolean canPerformTier(int requiredTier) {
        return this.enterpriseTier >= requiredTier;
    }
}
