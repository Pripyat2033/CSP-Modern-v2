package com.ben.csp.planning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A планёрка — the operational meeting of a work unit.
 *
 * <p>It is not only the routine daily stand-up: it is also where a missed goal
 * or an accident is examined and people are held to account, so it can be called
 * urgently, outside the routine slot, by whoever holds authority over the unit.
 *
 * <p>Attendance, decisions and any summons are recorded as minutes, which is how
 * the Secretary keeps the zone's administration running.
 */
public final class Planerka {

    public enum Status { SCHEDULED, IN_SESSION, CONCLUDED, CANCELLED }

    private final String id;
    private final PlanerkaReason reason;
    private final String convenorId;
    private final Authority convenorAuthority;
    private final String unitId;          // the scope: an enterprise id, or "zone"
    private final long scheduledDay;
    private final int scheduledMinute;
    private final Set<String> attendees = new LinkedHashSet<>();
    private final List<String> minutes = new ArrayList<>();
    private Status status = Status.SCHEDULED;

    public Planerka(String id, PlanerkaReason reason, String convenorId,
                    Authority convenorAuthority, String unitId,
                    long scheduledDay, int scheduledMinute) {
        this.id = Objects.requireNonNull(id, "id");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.convenorId = Objects.requireNonNull(convenorId, "convenorId");
        this.convenorAuthority = Objects.requireNonNull(convenorAuthority, "convenorAuthority");
        this.unitId = Objects.requireNonNull(unitId, "unitId");
        if (!convenorAuthority.isManager()) {
            throw new IllegalArgumentException(
                convenorAuthority.label() + " may not convene a planerka");
        }
        this.scheduledDay = scheduledDay;
        this.scheduledMinute = scheduledMinute;
    }

    public String id() { return id; }
    public PlanerkaReason reason() { return reason; }
    public String convenorId() { return convenorId; }
    public Authority convenorAuthority() { return convenorAuthority; }
    public String unitId() { return unitId; }
    public long scheduledDay() { return scheduledDay; }
    public int scheduledMinute() { return scheduledMinute; }
    public Status status() { return status; }
    public Set<String> attendees() { return Collections.unmodifiableSet(attendees); }
    public List<String> minutes() { return Collections.unmodifiableList(minutes); }

    public void addAttendee(String personId) {
        if (status == Status.CONCLUDED || status == Status.CANCELLED) {
            throw new IllegalStateException("cannot add an attendee to a " + status + " planerka");
        }
        attendees.add(Objects.requireNonNull(personId, "personId"));
    }

    /** Open the session. Refuses to open before the scheduled moment. */
    public void open(long currentDay, int currentMinute) {
        requireStatus(Status.SCHEDULED, "open");
        if (currentDay < scheduledDay
            || (currentDay == scheduledDay && currentMinute < scheduledMinute)) {
            throw new IllegalStateException(
                "planerka " + id + " is not due until day " + scheduledDay + " " + scheduledMinute);
        }
        status = Status.IN_SESSION;
        minutes.add("Opened: " + reason.label() + " (unit " + unitId + ", convened by "
            + convenorId + ").");
    }

    /** Record an agreed decision in the minutes. */
    public void recordDecision(String decision) {
        requireStatus(Status.IN_SESSION, "record a decision in");
        minutes.add("Decision: " + Objects.requireNonNull(decision, "decision"));
    }

    /**
     * Summon a person to be held to account, with the reason on the record.
     * This is the accountability use of a planerka, not a routine one.
     */
    public void summon(String personId, String why) {
        requireStatus(Status.IN_SESSION, "summon into");
        attendees.add(Objects.requireNonNull(personId, "personId"));
        minutes.add("Summons: " + personId + " to account — "
            + Objects.requireNonNull(why, "why"));
    }

    public void conclude() {
        requireStatus(Status.IN_SESSION, "conclude");
        status = Status.CONCLUDED;
        minutes.add("Concluded. Attended: " + String.join(", ", attendees) + ".");
    }

    public void cancel() {
        if (status == Status.CONCLUDED) {
            throw new IllegalStateException("cannot cancel a concluded planerka");
        }
        status = Status.CANCELLED;
    }

    private void requireStatus(Status expected, String action) {
        if (status != expected) {
            throw new IllegalStateException(
                "cannot " + action + " a " + status + " planerka (expected " + expected + ")");
        }
    }

    @Override
    public String toString() {
        return "Planerka#" + id + "[" + reason.label() + " " + unitId + " " + status + "]";
    }
}
