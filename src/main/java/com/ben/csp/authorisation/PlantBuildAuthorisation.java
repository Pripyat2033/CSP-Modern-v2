package com.ben.csp.authorisation;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.prombaza.PrombazaRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Whether construction of the power plant may start.
 *
 * <p>The plant cannot be begun by hand. It is built by the enterprises, and they
 * cannot build it until they themselves exist and function: every enterprise must
 * be operational (its facilities built, directed and manned), and a prombaza must
 * have been designated to build in. Until both are true, nothing of the plant may
 * be raised. This is the single gate that enforces that.
 */
public final class PlantBuildAuthorisation {

    /** Why construction is not yet permitted. */
    public enum Blocker {
        ENTERPRISES_NOT_READY("not every enterprise is built, directed and manned"),
        NO_PROMBAZA("no prombaza has been designated to build in");

        private final String reason;

        Blocker(String reason) { this.reason = reason; }

        public String reason() { return reason; }
    }

    private final List<Enterprise> enterprises;
    private final PrombazaRegistry prombazas;

    public PlantBuildAuthorisation(List<Enterprise> enterprises, PrombazaRegistry prombazas) {
        this.enterprises = List.copyOf(Objects.requireNonNull(enterprises, "enterprises"));
        this.prombazas = Objects.requireNonNull(prombazas, "prombazas");
    }

    public int readyCount() {
        return (int) enterprises.stream().filter(Enterprise::isOperational).count();
    }

    public int total() {
        return enterprises.size();
    }

    /** Every enterprise is operational. */
    public boolean enterprisesReady() {
        return !enterprises.isEmpty() && enterprises.stream().allMatch(Enterprise::isOperational);
    }

    /** A prombaza has been designated. */
    public boolean prombazaDesignated() {
        return !prombazas.isEmpty();
    }

    /** Construction may start. */
    public boolean isGranted() {
        return enterprisesReady() && prombazaDesignated();
    }

    /** Everything still standing in the way, in order. */
    public List<Blocker> blockers() {
        List<Blocker> out = new ArrayList<>();
        if (!enterprisesReady()) {
            out.add(Blocker.ENTERPRISES_NOT_READY);
        }
        if (!prombazaDesignated()) {
            out.add(Blocker.NO_PROMBAZA);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Refuse to let plant construction proceed unless it is authorised. Plant
     * construction code must call this before it does anything.
     */
    public void require() {
        if (isGranted()) {
            return;
        }
        StringBuilder sb = new StringBuilder(
            "construction of the power plant is not authorised: ");
        List<Blocker> b = blockers();
        for (int i = 0; i < b.size(); i++) {
            if (i > 0) sb.append("; ");
            sb.append(b.get(i).reason());
        }
        sb.append(" (enterprises ready: ").append(readyCount()).append('/').append(total())
          .append("; prombaza designated: ").append(prombazaDesignated()).append(')');
        throw new IllegalStateException(sb.toString());
    }
}
