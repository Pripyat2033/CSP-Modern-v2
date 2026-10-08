package com.ben.csp.agent;

import net.minecraft.text.Text;

/**
 * Chief Engineer (Главный инженер) - Enterprise Director Level
 * 
 * Historical Role: Senior executive responsible for overall enterprise strategy,
 * cross-department coordination, and high-level technical decision making.
 * Reports to Player/Director of ChSZO and coordinates with other Enterprise Directors.
 * 
 * Key Responsibilities:
 * - Strategic planning and resource allocation across enterprises
 * - Technical oversight of all construction operations
 * - Quality assurance compliance with OPB-82 regulations
 * - Emergency response coordination for critical incidents
 */
public class GlavnyInzhener {
    
    private static final String AGENT_NAME = "Главный инженер"; // Chief Engineer in Cyrillic
    
    // Enterprise management state
    private double strategicPriority = 1.0;     // Weight of current strategic focus
    private java.util.Set<String> oversightDepartments = new java.util.HashSet<>();
    private java.util.Map<String, Integer> departmentReadiness = new java.util.HashMap<>();
    
    /**
     * Initialize Chief Engineer agent with default values
     */
    public GlavnyInzhener() {
        this.strategicPriority = 1.0;
        // Initial oversight of core departments
        oversightDepartments.add("construction");
        oversightDepartments.add("logistics");
        oversightDepartments.add("research");
        oversightDepartments.add("quality_control");
    }
    
    /**
     * Get the agent name in Cyrillic for authentic Soviet terminology
     */
    public static Text getAgentName() {
        return Text.literal(AGENT_NAME);
    }
    
    /**
     * Add a department under strategic oversight
     */
    public void addDepartment(String department) {
        if (!this.oversightDepartments.contains(department)) {
            this.oversightDepartments.add(department);
        }
    }
    
    /**
     * Get readiness score for specific department (0-100%)
     */
    public int getDepartmentReadiness(String department) {
        return this.departmentReadiness.getOrDefault(department, 0);
    }
    
    /**
     * Update department readiness based on performance metrics
     */
    public void updateDepartmentReadiness(String department, int newReadiness) {
        if (newReadiness < 0) newReadiness = 0;
        if (newReadiness > 100) newReadiness = 100;
        this.departmentReadiness.put(department, newReadiness);
    }
    
    /**
     * Check if enterprise can handle strategic initiative of complexity N
     */
    public boolean canHandleStrategicInitiative(int complexity) {
        // Complexity 1-3: routine operations (requires 50%+ readiness across all departments)
        // Complexity 4-5: major projects (requires 75%+ readiness)
        // Complexity 6-7: crisis management (requires 90%+ readiness)
        int avgReadiness = 0;
        for (String dept : this.oversightDepartments) {
            avgReadiness += this.getDepartmentReadiness(dept);
        }
        return (this.oversightDepartments.size() > 0) 
            ? (avgReadiness / this.oversightDepartments.size()) >= 
              (complexity <= 3 ? 50 : complexity <= 5 ? 75 : 90)
            : false;
    }
    
    /**
     * Calculate average department readiness for enterprise assessment
     */
    public double getAverageDepartmentReadiness() {
        if (this.oversightDepartments.size() == 0) return 0.0;
        int total = 0;
        for (String dept : this.oversightDepartments) {
            total += this.getDepartmentReadiness(dept);
        }
        return (double) total / this.oversightDepartments.size();
    }
}
