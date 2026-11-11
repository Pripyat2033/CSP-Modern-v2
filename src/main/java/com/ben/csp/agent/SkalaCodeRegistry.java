package com.ben.csp.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Skala Code Registry
 *
 * Historical Function: Maintains the coding system for SKALA-1 and SKALA-V
 * computers used in Soviet nuclear plant operations. Tracks the operational
 * codes, error codes, and diagnostic identifiers the enterprise computing
 * network used, and resolves a code to what it means.
 *
 * <p>The registry is seeded with the standard pre-1986 SKALA functions (the
 * reactor control program PRIZMA, and the DREG data-registration loop) and is
 * open to further codes. An unknown code is answered as unknown - it is never
 * guessed - and a code that is registered must carry a non-blank description.
 *
 * <p>Scope note (Chornobyl Family SKALA restoration): DIIS is deliberately
 * absent. The DIIS auxiliary system, and its later DIIS-2000 firmware, was fitted
 * at Chernobyl only in the 1990s, after Unit 4 was destroyed; the pre-1986
 * computer had no DIIS and no video displays.
 */
public class SkalaCodeRegistry {

    /**
     * Operation code mapping for SKALA computer systems. {@code priority} runs
     * 0 (lowest) upward; a higher number is more urgent.
     */
    public static class OperationCode {
        public final String code;
        public final String description;
        public final int priority;
        public final boolean critical;

        public OperationCode(String code, String description, int priority,
                            boolean isCritical) {
            this.code = code;
            this.description = description;
            this.priority = priority;
            this.critical = isCritical;
        }

        @Override
        public String toString() {
            return code + " [" + priority + (critical ? ", critical" : "") + "] " + description;
        }
    }

    private final Map<String, OperationCode> codes = new LinkedHashMap<>();

    /**
     * Initialize the code registry with the standard pre-1986 SKALA codes. These
     * are the functions the plant's computing network ran at the time of Unit 4;
     * further codes are added as they are encountered.
     */
    public SkalaCodeRegistry() {
        registerOperationCode("PRIZMA",
            "ПРИЗМА: the reactor control program — rod positioning, "
                + "power distribution and the fast scram (AZ-5)", 10, true);
        registerOperationCode("DREG",
            "ДРЕГ: the data-registration program — the single channel polling "
                + "loop (~12,000 channels); its tape held Unit 4's last output", 3, false);
    }

    /**
     * Register a new operation or diagnostic code in the registry.
     *
     * @return the previous entry for this code, or {@code null} if it was new
     * @throws IllegalArgumentException if the code or description is blank, or the
     *         priority is negative
     */
    public OperationCode registerOperationCode(String code, String description,
                                               int priority, boolean isCritical) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("priority must not be negative");
        }
        return codes.put(code, new OperationCode(code, description, priority, isCritical));
    }

    /**
     * Get operation description by code, or {@code null} if the code is unknown.
     */
    public String getOperationDescription(String code) {
        OperationCode op = codes.get(code);
        return op == null ? null : op.description;
    }

    /** The full entry for a code, or {@code null} if it is unknown. */
    public OperationCode get(String code) {
        return codes.get(code);
    }

    /** Whether a code has been registered. */
    public boolean isKnown(String code) {
        return codes.containsKey(code);
    }

    /** Whether a code is flagged critical (false for an unknown code). */
    public boolean isCritical(String code) {
        OperationCode op = codes.get(code);
        return op != null && op.critical;
    }

    /** How many codes are registered. */
    public int size() {
        return codes.size();
    }

    /** Every registered code, in registration order. The list is unmodifiable. */
    public List<OperationCode> all() {
        return Collections.unmodifiableList(new ArrayList<>(codes.values()));
    }

    /** Every critical code, in registration order. */
    public List<OperationCode> criticalCodes() {
        List<OperationCode> out = new ArrayList<>();
        for (OperationCode op : codes.values()) {
            if (op.critical) {
                out.add(op);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** All codes sorted most urgent first, so an operator sees what matters. */
    public List<OperationCode> byPriority() {
        List<OperationCode> out = new ArrayList<>(codes.values());
        out.sort(Comparator.comparingInt((OperationCode c) -> c.priority).reversed());
        return Collections.unmodifiableList(out);
    }

    @Override
    public String toString() {
        return "SkalaCodeRegistry[" + codes.size() + " codes]";
    }
}
