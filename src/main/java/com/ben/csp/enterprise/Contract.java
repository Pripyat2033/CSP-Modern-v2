package com.ben.csp.enterprise;

import java.util.Objects;

/**
 * A contract (подряд / договор поставки) between the directorate and an
 * enterprise: deliver this work by this day. Failure to fulfil it is what makes
 * an enterprise delinquent and exposes its management to liability.
 */
public final class Contract {

    private final String id;
    private final String enterpriseId;
    private final String description;
    private final long dueDay;

    private boolean fulfilled;
    private CriminalLiability liability = CriminalLiability.NONE;
    private String failureNote;

    public Contract(String id, String enterpriseId, String description, long dueDay) {
        this.id = Objects.requireNonNull(id, "id");
        this.enterpriseId = Objects.requireNonNull(enterpriseId, "enterpriseId");
        this.description = Objects.requireNonNull(description, "description");
        this.dueDay = dueDay;
    }

    public String id() { return id; }
    public String enterpriseId() { return enterpriseId; }
    public String description() { return description; }
    public long dueDay() { return dueDay; }
    public boolean isFulfilled() { return fulfilled; }
    public CriminalLiability liability() { return liability; }
    public String failureNote() { return failureNote; }

    public void fulfil() {
        this.fulfilled = true;
    }

    public boolean isOverdue(long currentDay) {
        return !fulfilled && currentDay > dueDay;
    }

    /**
     * Record the failure of the contract and the liability it attracts. The
     * directorate decides the severity; the enterprise and its management answer
     * for it.
     */
    public void recordFailure(CriminalLiability liability, String note) {
        if (liability == CriminalLiability.NONE) {
            throw new IllegalArgumentException("a recorded failure must carry some liability");
        }
        this.liability = Objects.requireNonNull(liability, "liability");
        this.failureNote = Objects.requireNonNull(note, "note");
    }

    @Override
    public String toString() {
        return "Contract#" + id + "[" + enterpriseId + " due day " + dueDay
            + (fulfilled ? " FULFILLED" : " open") + "]";
    }
}
