package com.ben.csp.agent;

/**
 * Directors' Plansheet Block Entity
 * 
 * Historical Context: The Planshet (Планшет) was a Soviet-era mechanical
 * drafting table used for creating construction plans, schematics, and blueprints.
 * This block entity tracks GUI slots and document state for the director's tablet.
 */
public class DirectorsPlanshetBlockEntity {
    /**
     * Number of inventory slots in the directors' plansheet GUI.
     * Each slot can hold a document file or blueprint.
     */
    public static final int PLANSHEET_SLOT_COUNT = 9;
    
    /**
     * Get the number of plansheet slots for GUI binding
     */
    public static int getSlotCount() {
        return PLANSHEET_SLOT_COUNT;
    }
}
