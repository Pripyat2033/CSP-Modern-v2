package com.ben.csp.zone;

import com.ben.csp.enterprise.Footprint;
import com.ben.csp.entity.CspPersonEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

/**
 * Puts each enterprise's crew on its own plot, so a site is seen to be worked and not
 * merely filled in by a clock. Workers are spawned around the plot being built — the
 * office first, then each facility — up to a small gang per enterprise. The crews are
 * capped so the world never fills up.
 */
public final class CrewMuster {

    private CrewMuster() {}

    private static final int CREW_PER_SITE = 4;
    private static final int MAX_TOTAL_CREW = 240;

    public static void spawnCrews(ServerWorld world, ZoneEnterprises zone) {
        if (countWorkers(world) >= MAX_TOTAL_CREW) {
            return;
        }
        for (EnterpriseLifecycle l : zone.lifecycles()) {
            Footprint plot = l.activePlot();
            if (plot == null) {
                continue;
            }
            int have = workersNear(world, plot);
            if (have >= CREW_PER_SITE) {
                continue;
            }
            EntityType<? extends MobEntity> type = Muster.typeFor(l.enterprise().specialty());
            for (int i = have; i < CREW_PER_SITE; i++) {
                spawnOne(world, type, plot);
            }
        }
    }

    private static int countWorkers(ServerWorld world) {
        int n = 0;
        for (var e : world.iterateEntities()) {
            if (e instanceof CspPersonEntity) {
                n++;
            }
        }
        return n;
    }

    private static int workersNear(ServerWorld world, Footprint plot) {
        Box box = new Box(plot.minX() - 4, world.getBottomY(), plot.minZ() - 4,
                          plot.maxX() + 4, world.getTopY(), plot.maxZ() + 4);
        return world.getEntitiesByClass(CspPersonEntity.class, box, e -> true).size();
    }

    private static void spawnOne(ServerWorld world, EntityType<? extends MobEntity> type,
                                 Footprint plot) {
        MobEntity e = type.create(world);
        if (e == null) {
            return;
        }
        int x = (int) Math.floor(plot.minX() + world.random.nextDouble() * Math.max(1, plot.width()));
        int z = (int) Math.floor(plot.minZ() + world.random.nextDouble() * Math.max(1, plot.height()));
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        e.refreshPositionAndAngles(x + 0.5, y, z + 0.5, world.random.nextFloat() * 360f, 0f);
        world.spawnEntity(e);
    }
}
