package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseRoster;
import com.ben.csp.prombaza.Area;

import java.util.List;

/**
 * The zone must actually come to life: the enterprises build their offices, hire, and
 * stand up their facilities, and all ten are operational within a watchable time. This
 * guards that the build progresses at all and does not silently stall — the fault the
 * Director found by running it.
 */
public final class ZoneLifecycleSelfTest {

    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        Area area = Area.of(0, 64, 0, 400, 64, 400);
        PrombazaTerritory territory = PrombazaTerritory.standard("pb1", area);
        List<Enterprise> roster = EnterpriseRoster.provisional();
        ZoneEnterprises zone = ZoneEnterprises.spawn(territory, roster, 0);

        check("every enterprise gets an HQ plot",
            roster.size(), countHqPlots(zone));

        int firstOperational = -1;
        int allOperational = -1;
        for (int day = 1; day <= 120; day++) {
            zone.tick(day);
            int op = zone.operationalCount();
            if (op >= 1 && firstOperational < 0) {
                firstOperational = day;
            }
            if (op == roster.size()) {
                allOperational = day;
                break;
            }
        }

        check("the first enterprise is operational within 30 days",
            firstOperational > 0 && firstOperational <= 30, true);
        check("all ten enterprises are operational within 60 days",
            allOperational > 0 && allOperational <= 60, true);
        check("all ten reach operational", roster.size(), zone.operationalCount());

        int totalSites = 0;
        for (EnterpriseLifecycle l : zone.lifecycles()) {
            totalSites += l.sites().size();
        }
        check("facilities get built (construction sites exist)", totalSites > 0, true);

        System.out.println(failures == 0
            ? "ZoneLifecycleSelfTest: " + checks + " checks passed"
            : "ZoneLifecycleSelfTest: " + failures + " FAILED of " + checks);
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static int countHqPlots(ZoneEnterprises zone) {
        int n = 0;
        for (EnterpriseLifecycle l : zone.lifecycles()) {
            if (l.hqPlot() != null) {
                n++;
            }
        }
        return n;
    }

    private static void check(String what, int want, int got) {
        checks++;
        if (want != got) {
            failures++;
            System.out.println("  FAIL: " + what + " -> " + got + ", wanted " + want);
        }
    }

    private static void check(String what, boolean want, boolean got) {
        checks++;
        if (want != got) {
            failures++;
            System.out.println("  FAIL: " + what + " -> " + got + ", wanted " + want);
        }
    }
}
