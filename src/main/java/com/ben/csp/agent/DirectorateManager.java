package com.ben.csp.agent;

/**
 * Directorate Management System
 */
public class DirectorateManager {
    
    public static class EnterpriseDirector {
        public final String directorId;
        public final String enterpriseType;
        public double operationalReadiness;
        public java.util.Set<String> managedFacilities = new java.util.HashSet<>();
        
        public EnterpriseDirector(String id, String type) {
            this.directorId = id;
            this.enterpriseType = type;
            this.operationalReadiness = 50.0;
        }
    }
    
    private static java.util.Map<String, EnterpriseDirector> directors = new java.util.HashMap<>();
    
    public static void registerDirector(String id, String type) {
        if (!directors.containsKey(id)) {
            directors.put(id, new EnterpriseDirector(id, type));
        }
    }
    
    public static String getPlayerRole() {
        return "Директор ЧСЗО";
    }
    
    public static java.util.Map<String, EnterpriseDirector> getDirectors() {
        return new java.util.HashMap<>(directors);
    }
    
    public static void updateDirectorReadiness(String directorId, double newReadiness) {
        EnterpriseDirector director = directors.get(directorId);
        if (director != null) {
            director.operationalReadiness = Math.min(Math.max(newReadiness, 0.0), 100.0);
        }
    }
    
    public static boolean canEnterprisePerform(String directorId, int requiredTier) {
        EnterpriseDirector director = directors.get(directorId);
        if (director == null) return false;
        
        int calculatedTier = 1;
        if (director.operationalReadiness >= 30) calculatedTier = 2;
        if (director.operationalReadiness >= 60) calculatedTier = 3;
        if (director.operationalReadiness >= 85) calculatedTier = 4;
        
        return calculatedTier >= requiredTier;
    }
}
