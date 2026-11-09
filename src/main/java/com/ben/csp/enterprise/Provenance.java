package com.ben.csp.enterprise;

/**
 * How much of an organisation or facility rests on a source, and how much is
 * reconstruction.
 *
 * <p>The mission is forensic: where a fact is known it is cited; where it is not
 * it is labelled, never passed off as history. This marks which is which, so the
 * two are never confused — in the data, in the UI, and in any later write-up.
 */
public enum Provenance {

    /** Taken from a source: a document, a drawing, a record. Cited. */
    VERIFIED("Verified", "from a cited source"),

    /** Reconstructed to be plausible where the record is silent. Not a claim. */
    RECONSTRUCTION("Reconstruction", "provisional — the record is silent here");

    private final String label;
    private final String note;

    Provenance(String label, String note) {
        this.label = label;
        this.note = note;
    }

    public String label() { return label; }
    public String note() { return note; }
    public boolean isVerified() { return this == VERIFIED; }
}
