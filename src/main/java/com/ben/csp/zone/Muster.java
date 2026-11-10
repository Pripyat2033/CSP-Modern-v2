package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.Footprint;
import com.ben.csp.entity.CspPersonEntity;
import com.ben.csp.entity.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Puts the whole workforce on the ground, so the Director can see and measure whether
 * the world can carry it. Every person the roster calls for is spawned as a real
 * worker, around the enterprise that employs them; the count is exact.
 *
 * <p>The roster is the authority on who exists; this only places them. If a facility
 * has not been sited yet, its people fall back to the muster point.
 */
public final class Muster {

    private Muster() {}

    /** Spawn the whole workforce around each enterprise; returns how many were placed. */
    public static int everyone(ServerWorld world, ZoneEnterprises zone, BlockPos musterPoint) {
        int placed = 0;
        int index = 0;
        for (EnterpriseLifecycle l : zone.lifecycles()) {
            Enterprise e = l.enterprise();
            BlockPos centre = centreOf(e, musterPoint);
            for (Facility f : e.facilities()) {
                BlockPos at = f.hasFootprint() ? centreOfFootprint(f.footprint()) : centre;
                for (int i = 0; i < f.requiredStaff(); i++) {
                    double angle = index * 2.39996;              // golden angle
                    double radius = 3.0 + Math.sqrt(index) * 1.4;
                    int x = at.getX() + (int) Math.round(Math.cos(angle) * radius);
                    int z = at.getZ() + (int) Math.round(Math.sin(angle) * radius);
                    int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                    spawnOne(world, pickType(e, f), x, y, z);
                    placed++;
                    index++;
                }
            }
        }
        return placed;
    }

    /** Remove every worker of ours from the world; returns how many were removed. */
    public static int demobilise(ServerWorld world) {
        int removed = 0;
        for (Entity e : world.iterateEntities()) {
            if (e instanceof CspPersonEntity) {
                e.discard();
                removed++;
            }
        }
        return removed;
    }

    /** How many workers of ours are standing in the world right now. */
    public static int count(ServerWorld world) {
        int n = 0;
        for (Entity e : world.iterateEntities()) {
            if (e instanceof CspPersonEntity) {
                n++;
            }
        }
        return n;
    }

    private static BlockPos centreOf(Enterprise e, BlockPos fallback) {
        for (Facility f : e.facilities()) {
            if (f.hasFootprint()) {
                return centreOfFootprint(f.footprint());
            }
        }
        return fallback;
    }

    private static BlockPos centreOfFootprint(Footprint fp) {
        return BlockPos.ofFloored(fp.centerX(), 64, fp.centerZ());
    }

    private static void spawnOne(ServerWorld world, EntityType<? extends MobEntity> type,
                                 int x, int y, int z) {
        MobEntity e = type.create(world);
        if (e == null) {
            return;
        }
        e.refreshPositionAndAngles(x + 0.5, y, z + 0.5, world.random.nextFloat() * 360f, 0f);
        world.spawnEntity(e);
    }

    /** Choose a trade fitting the enterprise's specialty and the facility's kind. */
    private static EntityType<? extends MobEntity> pickType(Enterprise e, Facility f) {
        return switch (e.specialty()) {
            case GENERAL_CONSTRUCTION ->
                f.type() == com.ben.csp.enterprise.FacilityType.HEADQUARTERS
                    ? ModEntities.ENTERPRISE_DIRECTOR : ModEntities.PRORAB;
            case EARTHWORKS -> ModEntities.STROITEL;
            case CONCRETE_WORKS -> ModEntities.STROITEL;
            case STEEL_ERECTION -> ModEntities.MASTER;
            case SPECIAL_WORKS -> ModEntities.BRIGADIER;
            case ELECTRICAL_INSTALLATION -> ModEntities.TOLKACH;
            case THERMAL_INSTALLATION -> ModEntities.MASTER;
            case FINISHING -> ModEntities.STROITEL;
            case TRANSPORT -> ModEntities.NACHALNIK_SNABZHENIYA;
            case SANITATION_VENTILATION -> ModEntities.TOLKACH;
        };
    }
}
