package com.ben.csp.entity;

import com.ben.csp.build.DirectorateManager;
import com.ben.csp.build.Enterprise;
import org.jetbrains.annotations.Nullable;

/**
 * An interface for any entity that is a member of a specific Enterprise.
 * This provides a unified way to manage an entity's affiliation.
 */
public interface EnterprisePersonnel {
    String getEnterpriseName();
    void setEnterpriseName(String name);

    /**
     * A helper method to get the full Enterprise object from the Directorate.
     * @return The Enterprise object, or null if not found.
     */
    @Nullable
    default Enterprise getEnterprise() {
        String name = getEnterpriseName();
        if (name == null || name.isEmpty() || DirectorateManager.get() == null) {
            return null;
        }
        return DirectorateManager.get().getEnterprise(name);
    }
}