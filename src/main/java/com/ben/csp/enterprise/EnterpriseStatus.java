package com.ben.csp.enterprise;

/** Where an enterprise stands in its build-up and its contract. */
public enum EnterpriseStatus {
    FORMING("Forming — facilities not yet built"),
    OPERATIONAL("Operational — all facilities working"),
    DELINQUENT("Delinquent — contract not fulfilled");

    private final String label;

    EnterpriseStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
