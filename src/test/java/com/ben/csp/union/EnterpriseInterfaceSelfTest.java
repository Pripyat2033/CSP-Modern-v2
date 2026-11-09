package com.ben.csp.union;

import com.ben.csp.enterprise.Provenance;

/** Standalone self-test for how an enterprise meets the rest of the country. */
public final class EnterpriseInterfaceSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        // the three authorities are distinct, not one thing
        eq(3, AuthorityKind.values().length, "there are three separate authorities");
        check(AuthorityKind.MINISTRY != AuthorityKind.GOSPLAN, "the ministry is not the planner");
        check(AuthorityKind.GOSPLAN != AuthorityKind.GOSSNB, "the planner is not the supplier");

        EnterpriseInterface iface = UnionFacts.powerBranchInterface("zhelezobeton");

        // it is subordinate to the branch ministry
        check(iface.ministry() != null, "the enterprise has a branch ministry");
        eq(AuthorityKind.MINISTRY, iface.ministry().kind(), "its ministry is a ministry");
        check(iface.ministry().name().contains("энергетики"), "the power branch ministry is Minenergo");
        eq(Provenance.VERIFIED, iface.ministry().provenance(), "the ministry is a cited fact");

        // its targets come down in the plan, and its materials from Gossnab
        check(iface.gosplan() != null, "its targets come from GOSPLAN");
        check(iface.gossnab() != null, "its materials come from GOSSNB");
        check(iface.gosplan().kind() != iface.gossnab().kind(),
            "the planner and the supplier are different bodies");

        // it works on economic accountability and keeps its own supply department
        check(iface.isHozraschet(), "the enterprise works on хозрасчёт");

        // it is connected to the country by real transport, and not otherwise
        check(iface.isConnected(), "the enterprise is connected by rail");
        eq(2, iface.transportNodes().size(), "the power branch has two rail nodes");
        eq(Provenance.VERIFIED, UnionFacts.semikhodyRail().provenance(),
            "Semikhody, the plant's own station, is a cited fact");
        check(UnionFacts.semikhodyRail().connectsTo().contains("internal"),
            "Semikhody links on to the plant's internal rail network");
        eq(TransportKind.RAIL, iface.transportNodes().get(0).kind(), "the link is rail");
        eq(Provenance.VERIFIED, iface.transportNodes().get(0).provenance(),
            "the Yanov rail link is a cited fact");
        check(UnionFacts.SITE_CHOSEN_FOR_THE_RAIL.contains("Чернигов"),
            "the record notes the rail line decided the site");
        eq(Provenance.RECONSTRUCTION, UnionFacts.pripyatRiverPort().provenance(),
            "the river port detail is still a reconstruction");

        // an interface with no transport is not connected
        EnterpriseInterface isolated = new EnterpriseInterface("x", true).to(UnionFacts.minenergo());
        check(!isolated.isConnected(), "an enterprise with no transport is not connected");

        // the plan is tiered, not only a Union affair
        check(UnionFacts.gosplan(AdministrativeLevel.REPUBLIC).level() == AdministrativeLevel.REPUBLIC,
            "GOSPLAN exists at republic level too");
        check(AdministrativeLevel.values().length >= 4, "the plan runs down several levels");

        System.out.println("EnterpriseInterfaceSelfTest: " + checks + " checks passed.");
    }

    private static void eq(Object e, Object a, String what) {
        checks++;
        if (!String.valueOf(e).equals(String.valueOf(a))) {
            throw new AssertionError(what + ": expected <" + e + "> but was <" + a + ">");
        }
    }
    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) throw new AssertionError(what);
    }
}
