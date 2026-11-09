package com.ben.csp.agent;

/**
 * Directors' Planshet Block Entity
 *
 * Historical note: a планшет (from French planchette) is the rigid drawing board
 * — usually an aluminium sheet about 500x500 mm — that is part of the mensula
 * (plane table). The surveyor pins the paper to it and draws the topographic
 * plan in the field with a kipregel. So the planshet is the surveyor's field map
 * board on which the plan of the site is built up, not a drafting desk.
 *
 * This block entity tracks GUI slots and document state for the director's
 * plan display.
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
