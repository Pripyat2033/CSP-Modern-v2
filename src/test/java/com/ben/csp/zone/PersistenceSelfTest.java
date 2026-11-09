package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseRoster;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.prombaza.Area;

import java.util.List;

/**
 * Standalone self-test: the zone survives a save and load. A zone is run part-way,
 * snapshotted, restored into a fresh zone, and both are run to the end — they must
 * finish the same.
 */
public final class PersistenceSelfTest {
    private static int checks = 0;

    public static void main(String[] args) {
        Area area = Area.of(0, 60, 0, 240, 80, 240);

        // run a zone part-way
        ZoneEnterprises original = spawn(area);
        for (int i = 0; i < 40; i++) {
            original.tick(i);
        }
        check(!original.allOperational(), "the zone is only part-built at the snapshot");

        // snapshot the state that would be saved
        String[] snapshot = snapshot(original);

        // restore into a fresh zone
        ZoneEnterprises restored = spawn(area);
        restore(restored, snapshot);

        // the restored zone looks the same as the original at the moment of saving
        eq(original.operationalCount(), restored.operationalCount(),
            "the same enterprises are operational after loading");
        for (int i = 0; i < original.count(); i++) {
            eq(original.lifecycles().get(i).setup().hasHq(),
               restored.lifecycles().get(i).setup().hasHq(), "HQ state matches");
            eq(original.lifecycles().get(i).setup().staffHired(),
               restored.lifecycles().get(i).setup().staffHired(), "staff state matches");
        }

        // both finish the same
        int t = 40;
        while (!original.allOperational() && t < 9000) {
            original.tick(t++);
        }
        while (!restored.allOperational() && t < 18000) {
            restored.tick(t++);
        }
        check(original.allOperational(), "the original zone finishes");
        check(restored.allOperational(), "the restored zone finishes too");

        // and the facilities ended in the same places
        for (int i = 0; i < original.count(); i++) {
            List<Facility> a = original.lifecycles().get(i).enterprise().facilities();
            List<Facility> b = restored.lifecycles().get(i).enterprise().facilities();
            for (int j = 0; j < a.size(); j++) {
                eq(a.get(j).footprint(), b.get(j).footprint(),
                    "facility " + j + " stands in the same place after loading");
                eq(a.get(j).isBuilt(), b.get(j).isBuilt(), "facility " + j + " is built in both");
            }
        }

        System.out.println("PersistenceSelfTest: " + checks + " checks passed.");
    }

    private static ZoneEnterprises spawn(Area area) {
        PrombazaTerritory territory = PrombazaTerritory.standard("pb1", area);
        List<Enterprise> roster = EnterpriseRoster.provisional();
        return ZoneEnterprises.spawn(territory, roster, 0);
    }

    /** Capture the state a save would hold, as strings (as NBT would). */
    private static String[] snapshot(ZoneEnterprises zone) {
        StringBuilder sb = new StringBuilder();
        for (EnterpriseLifecycle l : zone.lifecycles()) {
            sb.append(l.enterprise().id()).append('|')
              .append(l.setup().hasHq()).append(';')
              .append(l.setup().hqProgress()).append(';')
              .append(l.setup().staffHired()).append('|');
            for (Facility f : l.enterprise().facilities()) {
                String fp = f.hasFootprint()
                    ? f.footprint().minX() + ":" + f.footprint().minZ() + ":"
                      + f.footprint().maxX() + ":" + f.footprint().maxZ()
                    : "-";
                String dir = f.facilityDirectorId() == null ? "-" : f.facilityDirectorId();
                sb.append(fp).append('~').append(f.isBuilt()).append('~')
                  .append(f.isAsBuiltAccepted()).append('~').append(dir).append('~')
                  .append(f.staff()).append(';');
            }
            sb.append('|');
            for (ConstructionSite s : l.sites()) {
                sb.append(s.manHoursDone()).append(',').append(s.materialsOnSite()).append(';');
            }
            sb.append('\n');
        }
        return sb.toString().split("\n");
    }

    private static void restore(ZoneEnterprises zone, String[] snapshot) {
        for (int i = 0; i < snapshot.length && i < zone.count(); i++) {
            String[] parts = snapshot[i].split("\\|", -1);
            EnterpriseLifecycle l = zone.lifecycles().get(i);
            String[] setup = parts[1].split(";");
            String[] facs = parts[2].split(";");
            String[] sites = parts[3].split(";");
            l.setup().restore(Boolean.parseBoolean(setup[0]), Integer.parseInt(setup[1]),
                Integer.parseInt(setup[2]), 0, 1);
            List<Facility> fs = l.enterprise().facilities();
            for (int j = 0; j < facs.length && j < fs.size(); j++) {
                String[] f = facs[j].split("~");
                Facility fac = fs.get(j);
                if (!f[0].equals("-")) {
                    String[] xy = f[0].split(":");
                    l.claimPlot(fac, com.ben.csp.enterprise.Footprint.of(
                        Double.parseDouble(xy[0]), Double.parseDouble(xy[1]),
                        Double.parseDouble(xy[2]), Double.parseDouble(xy[3])));
                }
                if (Boolean.parseBoolean(f[1])) {
                    fac.completeConstruction();
                }
                if (Boolean.parseBoolean(f[2])) {
                    fac.acceptAsBuilt();
                }
                if (!f[3].equals("-")) {
                    fac.assignFacilityDirector(f[3]);
                }
                fac.setStaff(Integer.parseInt(f[4]));
            }
            for (int j = 0; j < sites.length && j < l.sites().size(); j++) {
                String[] s = sites[j].split(",");
                l.restoreSite(j, Double.parseDouble(s[0]), Double.parseDouble(s[1]), "br");
            }
            l.recomputePhase();
        }
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
