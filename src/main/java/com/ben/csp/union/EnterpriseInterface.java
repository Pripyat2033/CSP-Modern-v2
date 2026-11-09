package com.ben.csp.union;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * How one enterprise touches the rest of the country. An enterprise is not a
 * free agent: it is subordinate to a branch ministry, its targets come down in
 * the state plan (GOSPLAN), its materials are distributed to it (GOSSNB), and it
 * moves goods and people through real transport nodes — rail, road, river. It
 * also keeps its own supply department (OMTS) and works on economic
 * accountability (хозрасчёт).
 *
 * <p>Everything beyond the transport nodes is abstracted: the node is where the
 * zone meets the country, and the country itself is a set of destinations.
 */
public final class EnterpriseInterface {

    private final String enterpriseId;
    private final List<Authority> authorities = new ArrayList<>();
    private final List<TransportNode> transportNodes = new ArrayList<>();
    private final boolean hozraschet;

    public EnterpriseInterface(String enterpriseId, boolean hozraschet) {
        this.enterpriseId = Objects.requireNonNull(enterpriseId, "enterpriseId");
        this.hozraschet = hozraschet;
    }

    public String enterpriseId() { return enterpriseId; }
    public boolean isHozraschet() { return hozraschet; }

    public EnterpriseInterface to(Authority authority) {
        authorities.add(Objects.requireNonNull(authority, "authority"));
        return this;
    }

    public EnterpriseInterface via(TransportNode node) {
        transportNodes.add(Objects.requireNonNull(node, "node"));
        return this;
    }

    public List<Authority> authorities() {
        return Collections.unmodifiableList(authorities);
    }

    public List<TransportNode> transportNodes() {
        return Collections.unmodifiableList(transportNodes);
    }

    /** The branch ministry this enterprise is subordinate to, if one is set. */
    public Authority ministry() {
        return firstOf(AuthorityKind.MINISTRY);
    }

    public Authority gosplan() {
        return firstOf(AuthorityKind.GOSPLAN);
    }

    public Authority gossnab() {
        return firstOf(AuthorityKind.GOSSNB);
    }

    private Authority firstOf(AuthorityKind kind) {
        return authorities.stream().filter(a -> a.kind() == kind).findFirst().orElse(null);
    }

    /** True once the enterprise has at least one way to move goods in and out. */
    public boolean isConnected() {
        return !transportNodes.isEmpty();
    }

    @Override
    public String toString() {
        return "interface[" + enterpriseId + "] " + authorities.size() + " authorities, "
            + transportNodes.size() + " transport nodes"
            + (hozraschet ? ", хозрасчёт" : "");
    }
}
