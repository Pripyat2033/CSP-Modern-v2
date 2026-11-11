package com.ben.csp.rbmk;

import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A construction job: an ordered list of regions to fill, drained a few thousand
 * blocks at a time by {@link ConstructionScheduler} on the server clock.
 *
 * <p>It is cursor-based, so the plan is a handful of region descriptors rather
 * than a list of every block: a four-million-block plant costs a few kilobytes of
 * plan, not a gigabyte. This is what makes metered construction possible without
 * the plan itself becoming the memory problem.
 *
 * <p>Regions come in four shapes, because a reactor is not made of boxes: solid
 * boxes and hollow shells for the machines, vertical columns for the channels,
 * and <b>radial</b> fills (discs and annuli) for the round sections — the
 * biological shields, the vessel and the water tank.
 */
public final class ConstructionJob {

    /** What a region is, so the counts stay meaningful. */
    public enum Part {
        GRAPHITE, FUEL, RODS, SEPARATORS, TURBINES, CONDENSERS, PUMPS,
        FEEDWATER, ACCUMULATORS, PIPES, UBS, LBS, VESSEL, WATER_TANK
    }

    private enum Mode { BOX, SHELL, COLUMNS, RADIAL }

    /** A region to fill. */
    public static final class Region {
        private final Part part;
        private final Block block;
        private final Mode mode;
        private final int x0, y0, z0, x1, y1, z1;   // half-open box bounds
        private final int[] colX, colZ;             // columns mode only
        private final int colHeight;                // columns mode only
        private final int ccx, ccz;                 // radial mode only
        private final long rIn2, rOut2;             // radial mode: squared radii
        private final long iterLen;                 // positions to walk
        private final long placeTotal;              // blocks actually placed
        private long idx;                           // cursor

        private Region(Part part, Block block, Mode mode,
                       int x0, int y0, int z0, int x1, int y1, int z1,
                       int[] colX, int[] colZ, int colHeight,
                       int ccx, int ccz, long rIn2, long rOut2) {
            this.part = part;
            this.block = block;
            this.mode = mode;
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.colX = colX;
            this.colZ = colZ;
            this.colHeight = colHeight;
            this.ccx = ccx;
            this.ccz = ccz;
            this.rIn2 = rIn2;
            this.rOut2 = rOut2;
            if (mode == Mode.COLUMNS) {
                this.iterLen = (long) colX.length * colHeight;
                this.placeTotal = iterLen;
            } else {
                long dx = Math.max(0, x1 - x0);
                long dy = Math.max(0, y1 - y0);
                long dz = Math.max(0, z1 - z0);
                long volume = dx * dy * dz;
                this.iterLen = volume;
                if (mode == Mode.SHELL) {
                    this.placeTotal = volume
                        - Math.max(0, dx - 2) * Math.max(0, dy - 2) * Math.max(0, dz - 2);
                } else if (mode == Mode.RADIAL) {
                    this.placeTotal = countRadial();
                } else {
                    this.placeTotal = volume;
                }
            }
        }

        private long countRadial() {
            long n = 0;
            for (int x = x0; x < x1; x++) {
                for (int z = z0; z < z1; z++) {
                    long d2 = (long) (x - ccx) * (x - ccx) + (long) (z - ccz) * (z - ccz);
                    if (d2 >= rIn2 && d2 <= rOut2) {
                        n++;
                    }
                }
            }
            return n * (y1 - y0);
        }

        private boolean done() {
            return idx >= iterLen;
        }

        /** Place up to {@code budget} blocks; return how many were placed. */
        private long fill(ServerWorld world, long budget) {
            long placed = 0;
            if (mode == Mode.COLUMNS) {
                while (idx < iterLen && placed < budget) {
                    int c = (int) (idx / colHeight);
                    int y = (int) (idx % colHeight);
                    world.setBlockState(new BlockPos(colX[c], y0 + y, colZ[c]),
                        block.getDefaultState(), 2);
                    placed++;
                    idx++;
                }
                return placed;
            }
            long dy = y1 - y0;
            long dz = z1 - z0;
            long dyz = dy * dz;
            while (idx < iterLen && placed < budget) {
                long i = idx;
                int x = x0 + (int) (i / dyz);
                long r = i % dyz;
                int y = y0 + (int) (r / dz);
                int z = z0 + (int) (r % dz);
                idx++;
                if (mode == Mode.SHELL) {
                    boolean surface = x == x0 || x == x1 - 1
                        || y == y0 || y == y1 - 1
                        || z == z0 || z == z1 - 1;
                    if (!surface) {
                        continue;
                    }
                } else if (mode == Mode.RADIAL) {
                    long d2 = (long) (x - ccx) * (x - ccx) + (long) (z - ccz) * (z - ccz);
                    if (d2 < rIn2 || d2 > rOut2) {
                        continue;
                    }
                }
                world.setBlockState(new BlockPos(x, y, z), block.getDefaultState(), 2);
                placed++;
            }
            return placed;
        }
    }

    // ---------------------------------------------------------------- factories

    /** A solid box, half-open on the far corner. */
    public static Region box(Part part, Block block, int x0, int y0, int z0,
                             int x1, int y1, int z1) {
        return new Region(part, block, Mode.BOX, x0, y0, z0, x1, y1, z1,
            null, null, 0, 0, 0, 0, 0);
    }

    /** Only the surface of a box — a hollow shell. */
    public static Region shell(Part part, Block block, int x0, int y0, int z0,
                               int x1, int y1, int z1) {
        return new Region(part, block, Mode.SHELL, x0, y0, z0, x1, y1, z1,
            null, null, 0, 0, 0, 0, 0);
    }

    /** A set of vertical columns, one block wide, from baseY upward. */
    public static Region columns(Part part, Block block, int[] xs, int[] zs,
                                 int baseY, int height) {
        return new Region(part, block, Mode.COLUMNS, 0, baseY, 0, 0, 0, 0,
            xs, zs, height, 0, 0, 0, 0);
    }

    /**
     * A radial fill — a disc (innerRadius 0) or an annulus — extruded from y0 to
     * y1. Radii are in blocks; the outer radius includes the boundary cell.
     */
    public static Region radial(Part part, Block block, int cx, int cz,
                                int innerRadius, int outerRadius, int y0, int y1) {
        int x0 = cx - outerRadius;
        int x1 = cx + outerRadius + 1;
        int z0 = cz - outerRadius;
        int z1 = cz + outerRadius + 1;
        long rIn2 = (long) innerRadius * innerRadius;
        long rOut2 = (long) outerRadius * outerRadius;
        return new Region(part, block, Mode.RADIAL, x0, y0, z0, x1, y1, z1,
            null, null, 0, cx, cz, rIn2, rOut2);
    }

    // ------------------------------------------------------------------- state

    private final ServerWorld world;
    private final String label;
    private final List<Region> regions = new ArrayList<>();
    private final Map<Part, Long> placed = new EnumMap<>(Part.class);
    private int regionIndex;
    private long placedTotal;
    private final long planTotal;
    private boolean finished;

    public ConstructionJob(ServerWorld world, String label) {
        this(world, label, new ArrayList<>(), 0);
    }

    private ConstructionJob(ServerWorld world, String label, List<Region> regions,
                            long planTotal) {
        this.world = world;
        this.label = label;
        this.regions.addAll(regions);
        this.planTotal = planTotal;
    }

    public String label() {
        return label;
    }

    public long planTotal() {
        return planTotal;
    }

    public long placedTotal() {
        return placedTotal;
    }

    public long placed(Part part) {
        return placed.getOrDefault(part, 0L);
    }

    public boolean finished() {
        return finished;
    }

    public double progress() {
        return planTotal == 0 ? 1.0 : (double) placedTotal / planTotal;
    }

    /** Build a job from a list of regions (used by {@link RbmkWorldBuilder}). */
    public static ConstructionJob of(ServerWorld world, String label, List<Region> regions) {
        long total = 0;
        for (Region r : regions) {
            total += r.placeTotal;
        }
        return new ConstructionJob(world, label, regions, total);
    }

    /**
     * Place up to {@code budget} blocks. Returns how many were placed. When the
     * last region is exhausted the job reports {@link #finished()}.
     */
    public long fill(long budget) {
        long remaining = budget;
        while (remaining > 0 && regionIndex < regions.size()) {
            Region r = regions.get(regionIndex);
            long done = r.fill(world, remaining);
            remaining -= done;
            placedTotal += done;
            placed.merge(r.part, done, Long::sum);
            if (r.done()) {
                regionIndex++;
            } else if (done == 0) {
                break; // defensive: never spin
            }
        }
        if (regionIndex >= regions.size()) {
            finished = true;
        }
        return budget - remaining;
    }
}
