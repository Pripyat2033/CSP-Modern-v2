package com.ben.csp.directorate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The director's in-tray. Requests from the enterprises (and from the survey)
 * accumulate here; the director approves or denies each. Nothing is built and no
 * territory is designated until it has been approved.
 */
public final class DirectorateWorkflow {

    private final List<DirectorateRequest> requests = new ArrayList<>();

    public void submit(DirectorateRequest request) {
        if (byId(request.id()).isPresent()) {
            throw new IllegalStateException("a request with id '" + request.id() + "' already exists");
        }
        requests.add(request);
    }

    public Optional<DirectorateRequest> byId(String id) {
        return requests.stream().filter(r -> r.id().equals(id)).findFirst();
    }

    public List<DirectorateRequest> pending() {
        List<DirectorateRequest> out = new ArrayList<>();
        for (DirectorateRequest r : requests) {
            if (r.isPending()) {
                out.add(r);
            }
        }
        return out;
    }

    public List<DirectorateRequest> all() {
        return Collections.unmodifiableList(requests);
    }

    public DirectorateRequest approve(String id, String by, long day) {
        DirectorateRequest r = require(id);
        r.approve(by, day);
        return r;
    }

    public DirectorateRequest deny(String id, String by, long day, String reason) {
        DirectorateRequest r = require(id);
        r.deny(by, day, reason);
        return r;
    }

    private DirectorateRequest require(String id) {
        return byId(id).orElseThrow(() ->
            new IllegalArgumentException("no request with id '" + id + "'"));
    }

    public int pendingCount() {
        return pending().size();
    }
}
