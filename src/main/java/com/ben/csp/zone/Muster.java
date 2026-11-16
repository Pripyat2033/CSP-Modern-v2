package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.enterprise.Footprint;
import com.ben.csp.entity.CspPersonEntity;
import com.ben.csp.entity.ModEntities;
import com.ben.csp.entity.WorkerRole;
import com.ben.csp.enterprise.Specialty;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

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
        return everyone(world, zone, musterPoint, -1);
    }

    /**
     * Spawn up to {@code limit} workers (all of them when {@code limit} is negative), so
     * the crowd can be brought on a little at a time and the tick watched as it grows.
     */
    public static int everyone(ServerWorld world, ZoneEnterprises zone, BlockPos musterPoint,
                               int limit) {
        int placed = 0;
        int index = 0;
        for (EnterpriseLifecycle l : zone.lifecycles()) {
            Enterprise e = l.enterprise();
            BlockPos centre = centreOf(e, musterPoint);
            for (Facility f : e.facilities()) {
                BlockPos at = f.hasFootprint() ? centreOfFootprint(f.footprint()) : centre;
                for (int i = 0; i < f.requiredStaff(); i++) {
                    if (limit >= 0 && placed >= limit) {
                        return placed;
                    }
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

    /**
     * Put a secretary at the office. She is the way into the zone: the Director
     * speaks to her, not to a command. Returns false if a secretary is already
     * within sight, so founding twice does not stack clerks.
     */
    public static boolean placeSecretary(ServerWorld world, BlockPos at) {
        Box around = new Box(at.getX() - 24, world.getBottomY(), at.getZ() - 24,
                             at.getX() + 24, world.getTopY(), at.getZ() + 24);
        for (CspPersonEntity p : world.getEntitiesByClass(CspPersonEntity.class, around, e -> true)) {
            if (p.getRole() == WorkerRole.SEKRETAR) {
                return false;
            }
        }
        spawnOne(world, ModEntities.SEKRETAR, at.getX(), at.getY(), at.getZ());
        return true;
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

    /** The worker entity type for an enterprise of this trade. */
    public static EntityType<? extends MobEntity> typeFor(Specialty s) {
        return switch (s) {
            case GENERAL_CONSTRUCTION -> ModEntities.PRORAB;
            case EARTHWORKS, CONCRETE_WORKS, FINISHING -> ModEntities.STROITEL;
            case STEEL_ERECTION, THERMAL_INSTALLATION -> ModEntities.MASTER;
            case SPECIAL_WORKS -> ModEntities.BRIGADIER;
            case ELECTRICAL_INSTALLATION, SANITATION_VENTILATION -> ModEntities.TOLKACH;
            case TRANSPORT -> ModEntities.NACHALNIK_SNABZHENIYA;
        };
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
