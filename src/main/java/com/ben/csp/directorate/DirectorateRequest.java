package com.ben.csp.directorate;

import com.ben.csp.prombaza.Area;
import com.ben.csp.enterprise.FacilityType;

import java.util.Objects;

/**
 * A matter put to the director for a decision: an enterprise asking to raise a
 * facility, or a request to designate a prombaza territory. The enterprises
 * build and manage their own facilities; what comes to the director is the
 * request to build, and he approves or denies it.
 */
public final class DirectorateRequest {

    private final String id;
    private final RequestType type;
    private final String subject;
    private final String enterpriseId;     // may be null
    private final FacilityType facilityType; // may be null
    private final Area area;               // may be null
    private final String requestedBy;
    private final long requestedDay;

    private RequestStatus status = RequestStatus.PENDING;
    private String decidedBy;
    private long decidedDay;
    private String decisionReason;

    public DirectorateRequest(String id, RequestType type, String subject, String enterpriseId,
                              FacilityType facilityType, Area area, String requestedBy, long day) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.enterpriseId = enterpriseId;
        this.facilityType = facilityType;
        this.area = area;
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy");
        this.requestedDay = day;
    }

    public String id() { return id; }
    public RequestType type() { return type; }
    public String subject() { return subject; }
    public String enterpriseId() { return enterpriseId; }
    public FacilityType facilityType() { return facilityType; }
    public Area area() { return area; }
    public String requestedBy() { return requestedBy; }
    public long requestedDay() { return requestedDay; }
    public RequestStatus status() { return status; }
    public String decidedBy() { return decidedBy; }
    public long decidedDay() { return decidedDay; }
    public String decisionReason() { return decisionReason; }
    public boolean isPending() { return status == RequestStatus.PENDING; }

    /** Restore a decided request from a save. */
    public void restore(RequestStatus status, String decidedBy, long decidedDay, String reason) {
        this.status = status;
        this.decidedBy = decidedBy;
        this.decidedDay = decidedDay;
        this.decisionReason = reason;
    }

    public void approve(String by, long day) {
        requirePending("approve");
        status = RequestStatus.APPROVED;
        decidedBy = Objects.requireNonNull(by, "by");
        decidedDay = day;
    }

    public void deny(String by, long day, String reason) {
        requirePending("deny");
        status = RequestStatus.DENIED;
        decidedBy = Objects.requireNonNull(by, "by");
        decidedDay = day;
        decisionReason = reason;
    }

    private void requirePending(String action) {
        if (status != RequestStatus.PENDING) {
            throw new IllegalStateException("cannot " + action + " a " + status + " request");
        }
    }

    @Override
    public String toString() {
        return "#" + id + " [" + type.label() + "] " + subject + " — " + status;
    }
}
