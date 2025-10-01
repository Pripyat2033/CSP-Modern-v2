package com.ben.csp.radiology;

import java.util.Map;

/**
 * Science Grade: A data record holding the radiological inventory for a specific event.
 * @param id The unique identifier for this source term.
 * @param description A human-readable description.
 * @param airborneReleaseBq A map of volatile isotopes and their total activity released to the air.
 * @param particulateReleaseBq A map of fuel-fragment isotopes and their total activity released as particles.
 * @param particulateCount The number of discrete hot particles to simulate.
 */
public record SourceTerm(
    String id, String description, Map<String, Double> airborneReleaseBq, Map<String, Double> particulateReleaseBq, int particulateCount
) {
}