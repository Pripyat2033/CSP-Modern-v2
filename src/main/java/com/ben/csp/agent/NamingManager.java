package com.ben.csp.agent;

/**
 * Naming and Terminology Management System
 * 
 * Historical Function: Maintains consistent Soviet-era terminology, project codes,
 * and department naming conventions. Ensures all documentation uses correct
 * OPB-82 approved terminology (e.g., "Prorab" not "foreman").
 */
public class NamingManager {
    
    /**
     * Official terminology mappings for authentic Soviet construction industry terms
     */
    private static final java.util.Map<String, String> OFFICIAL_TERMS = new java.util.HashMap<>();
    
    static {
        // Initialize official terminology registry
        OFFICIAL_TERMS.put("foreman", "Прораб");
        OFFICIAL_TERMS.put("engineer", "инженер");
        OFFICIAL_TERMS.put("master", "мастер");
        OFFICIAL_TERMS.put("supervisor", "надзорщик");
        OFFICIAL_TERMS.put("worker", "работник");
        OFFICIAL_TERMS.put("chief engineer", "главный инженер");
        OFFICIAL_TERMS.put("supply chief", "начальник снабжения");
        OFFICIAL_TERMS.put("secretary", "секретарь");
    }
    
    /**
     * Get official Soviet-era term for modern English equivalent
     */
    public static String getOfficialTerm(String englishTerm) {
        return OFFICIAL_TERMS.getOrDefault(englishTerm, englishTerm);
    }
    
    /**
     * Register a new project code or identifier
     */
    public void registerProjectCode(String code, String description) {
        // Project code registration system
    }
}
