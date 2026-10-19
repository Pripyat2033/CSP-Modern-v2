package com.ben.csp.entity;

/**
 * EnterprisePersonnel - Interface for entities that belong to an enterprise organization.
 * Provides methods to get/set enterprise association and access personnel data.
 */
public interface EnterprisePersonnel {
    
    /**
     * Gets the associated enterprise for this personnel entity.
     * @return The enterprise, or null if not assigned.
     */
    Enterprise getEnterprise();
    
    /**
     * Sets the enterprise this personnel belongs to.
     * @param enterprise The enterprise to assign, or null to unassign.
     */
    void setEnterprise(Enterprise enterprise);
    
    /**
     * Gets the name of the associated enterprise.
     * @return Enterprise name as displayed.
     */
    String getEnterpriseName();
    
    /**
     * Sets the enterprise name for display purposes.
     * @param name The enterprise name to display.
     */
    void setEnterpriseName(String name);
    
    /**
     * Checks if this entity is currently assigned to an enterprise.
     * @return true if assigned to an enterprise, false otherwise.
     */
    boolean hasEnterprise();
    
    /**
     * Gets the type of personnel within the enterprise (role/title).
     * @return Personnel role or type, empty string if not set.
     */
    String getPersonnelRole();
    
    /**
     * Sets the personnel's role within their enterprise.
     * @param role The role to assign (e.g., "Stroitel", "Prorab", "Kommutator").
     */
    void setPersonnelRole(String role);
}
