package com.ben.csp.enterprise;

/**
 * How a Soviet enterprise's failure was answered for.
 *
 * <p>An enterprise that failed to fulfil its contract was liable. The gravest
 * case was халатность (criminal negligence by an official — failure or improper
 * performance of duty through a dishonest or careless attitude to the service),
 * which under the RSFSR Criminal Code carried criminal responsibility. Property
 * disputes between enterprises were heard by State Arbitration.
 */
public enum CriminalLiability {
    NONE("No failure on the record"),
    DISCIPLINARY("Disciplinary — a reprimand, an order to make good"),
    MATERIAL("Material — the enterprise makes good the loss"),
    CRIMINAL_NEGLIGENCE("Criminal — халатность, referred for prosecution");

    private final String label;

    CriminalLiability(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
