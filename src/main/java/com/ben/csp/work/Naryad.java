package com.ben.csp.work;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A наряд — the Soviet work order / job ticket.
 *
 * <p>A наряд names a trade operation, a quantity of physical work, the brigade
 * that must do it, and a deadline. It is the instrument a planerka issues and a
 * prorab inspects. Nothing is paid or accepted until a prorab has inspected it.
 */
public final class Naryad {

    public enum Status {
        ISSUED,               // issued by the directorate, not yet accepted
        ACCEPTED,             // brigade has taken it on
        IN_PROGRESS,          // work under way
        PENDING_INSPECTION,   // quantity reached, waiting for a prorab
        COMPLETED,            // inspected and accepted
        REJECTED,             // inspected and refused (defects); work must be redone
        CLOSED                // settled
    }

    private final String id;
    private final String description;
    private final WorkType workType;
    private final Quantity planned;
    private final long issuedDay;
    private final long dueDay;

    private String brigadeId;
    private Quantity completed;
    private Status status = Status.ISSUED;
    private final List<String> defectNotes = new ArrayList<>();

    public Naryad(String id, String description, WorkType workType, Quantity planned,
                  String brigadeId, long issuedDay, long dueDay) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        this.workType = Objects.requireNonNull(workType, "workType");
        this.planned = Objects.requireNonNull(planned, "planned");
        if (planned.unit() != workType.unit()) {
            throw new IllegalArgumentException("planned unit " + planned.unit()
                + " does not match work type unit " + workType.unit());
        }
        this.brigadeId = brigadeId;
        this.issuedDay = issuedDay;
        this.dueDay = dueDay;
        this.completed = Quantity.of(0.0, planned.unit());
    }

    public String id() { return id; }
    public String description() { return description; }
    public WorkType workType() { return workType; }
    public Quantity planned() { return planned; }
    public Quantity completed() { return completed; }
    public Quantity remaining() { return planned.minus(completed); }
    public Status status() { return status; }
    public String brigadeId() { return brigadeId; }
    public long issuedDay() { return issuedDay; }
    public long dueDay() { return dueDay; }
    public List<String> defectNotes() { return Collections.unmodifiableList(defectNotes); }

    /** Total labour the norm says this job needs, in man-hours. */
    public double manHoursRequired() {
        return planned.amount() * workType.manHoursPerUnit();
    }

    public boolean isOverdue(long currentDay) {
        return currentDay > dueDay && status != Status.COMPLETED && status != Status.CLOSED;
    }

    public void assignTo(String brigadeId) {
        this.brigadeId = Objects.requireNonNull(brigadeId, "brigadeId");
    }

    public void accept() {
        requireStatus(Status.ISSUED, "accept");
        status = Status.ACCEPTED;
    }

    /** Record work done; caps at the planned quantity and moves to inspection. */
    public void recordProgress(Quantity done) {
        if (status != Status.ACCEPTED && status != Status.IN_PROGRESS) {
            throw new IllegalStateException("cannot record progress in status " + status);
        }
        if (done.unit() != planned.unit()) {
            throw new IllegalArgumentException("progress unit mismatch: " + done.unit());
        }
        double total = Math.min(planned.amount(), completed.amount() + done.amount());
        completed = Quantity.of(total, planned.unit());
        status = completed.amount() >= planned.amount()
            ? Status.PENDING_INSPECTION : Status.IN_PROGRESS;
    }

    /**
     * A prorab's verdict. Passing completes the наряд. Failing rejects the whole
     * наряд: the recorded defect is noted and the completed quantity is sent back
     * to zero for rework (partial rejection is a later refinement).
     */
    public void inspect(boolean pass, String note) {
        requireStatus(Status.PENDING_INSPECTION, "inspect");
        if (pass) {
            status = Status.COMPLETED;
        } else {
            status = Status.REJECTED;
            defectNotes.add(note == null ? "defect recorded" : note);
            completed = Quantity.of(0.0, planned.unit());
        }
    }

    public void reopen() {
        requireStatus(Status.REJECTED, "reopen");
        status = Status.IN_PROGRESS;
    }

    public void close() {
        if (status != Status.COMPLETED) {
            throw new IllegalStateException("only a completed наряд can be closed");
        }
        status = Status.CLOSED;
    }

    private void requireStatus(Status expected, String action) {
        if (status != expected) {
            throw new IllegalStateException(
                "cannot " + action + " наряд in status " + status + ", expected " + expected);
        }
    }

    @Override
    public String toString() {
        return "Naryad#" + id + "[" + workType.label() + " " + completed.format()
            + "/" + planned.format() + " " + status + "]";
    }
}
