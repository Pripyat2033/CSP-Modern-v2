package com.ben.csp.union;

/**
 * The three separate external authorities an enterprise answered to. They are
 * distinct bodies doing distinct jobs, and it is wrong to treat them as one:
 *
 * <ul>
 *   <li>The branch MINISTRY, which the enterprise is subordinate to: it directs
 *       the branch and hands down the plan to its enterprises.</li>
 *   <li>GOSPLAN, the state planning committee: it makes the plan (tiered, Union
 *       down to city), and sets the targets.</li>
 *   <li>GOSSNB, the state committee for material-technical supply: it distributes
 *       output to consumers by a set nomenclature and runs cooperative deliveries.
 *       It is NOT the planner; it supplies.</li>
 * </ul>
 *
 * <p>The Party committee at the enterprise is a further, parallel authority, but
 * it is not modelled here until it is verified against a source.
 */
public enum AuthorityKind {
    MINISTRY("Министерство", "branch ministry the enterprise is subordinate to"),
    GOSPLAN("Госплан", "state planning committee: makes the plan"),
    GOSSNB("Госснаб", "state committee for material-technical supply: supplies");

    private final String label;
    private final String gloss;

    AuthorityKind(String label, String gloss) {
        this.label = label;
        this.gloss = gloss;
    }

    public String label() { return label; }
    public String gloss() { return gloss; }
}
