package com.ben.csp.rbmk;

import com.ben.csp.perf.PerformanceProbe;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Builds things on the server clock, a few thousand blocks per tick, so no command
 * can hitch the server.
 *
 * <p>This is the answer to the one real cost: a monolithic build of a four-million
 * block plant took 3.7 seconds in a single tick. Here the same plan is drained at a
 * bounded rate — and the rate adapts, so if the server is already busy (or the
 * player is watching blocks appear) construction slows down rather than blowing the
 * tick budget. It is also what the Stroiteli will drive: the workforce places the
 * plant over time, not all at once.
 */
public final class ConstructionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger("csp-modern/construction");

    /** Default blocks placed per tick: 3,000 is about 0.5 ms/tick on the sandbox. */
    public static final int DEFAULT_BLOCKS_PER_TICK = 3000;

    /** The tick is 50 ms; construction stops growing once it is this busy. */
    private static final double SOFT_MS_BUDGET = 35.0;

    private static final Deque<ConstructionJob> QUEUE = new ArrayDeque<>();
    private static final List<ConstructionJob> ACTIVE = new ArrayList<>();
    private static int blocksPerTick = DEFAULT_BLOCKS_PER_TICK;
    private static boolean installed;

    private ConstructionScheduler() {}

    public static void register() {
        if (!installed) {
            ServerTickEvents.END_SERVER_TICK.register(ConstructionScheduler::tick);
            installed = true;
        }
    }

    public static void enqueue(ConstructionJob job) {
        register();
        QUEUE.add(job);
        LOGGER.info("construction queued: {} ({} blocks, {} per tick)",
            job.label(), job.planTotal(), blocksPerTick);
    }

    public static int blocksPerTick() {
        return blocksPerTick;
    }

    public static void setBlocksPerTick(int n) {
        blocksPerTick = Math.max(1, n);
    }

    public static int queueDepth() {
        return QUEUE.size() + ACTIVE.size();
    }

    public static boolean busy() {
        return !QUEUE.isEmpty() || !ACTIVE.isEmpty();
    }

    public static String status() {
        if (!busy()) {
            return "construction: idle";
        }
        ConstructionJob j = ACTIVE.isEmpty() ? QUEUE.peek() : ACTIVE.get(0);
        return String.format("construction: %s %.0f%% (%d/%d blocks), %d queued",
            j.label(), j.progress() * 100, j.placedTotal(), j.planTotal(), queueDepth());
    }

    private static void tick(MinecraftServer server) {
        if (QUEUE.isEmpty() && ACTIVE.isEmpty()) {
            return;
        }
        // start the next queued job if nothing is active
        if (ACTIVE.isEmpty()) {
            ACTIVE.add(QUEUE.poll());
        }

        // adaptive rate: back off if the server is already busy, so a big build
        // never pushes the tick past the point where the game stutters
        double ms = PerformanceProbe.averageMspt();
        int budget = blocksPerTick;
        if (ms > SOFT_MS_BUDGET) {
            budget = Math.max(1, (int) (blocksPerTick * (SOFT_MS_BUDGET / ms)));
        }

        long placed = 0;
        while (budget > 0 && !ACTIVE.isEmpty()) {
            ConstructionJob job = ACTIVE.get(0);
            long done = job.fill(budget);
            placed += done;
            budget -= done;
            if (job.finished()) {
                ACTIVE.remove(0);
                LOGGER.info("construction finished: {} ({} blocks)", job.label(),
                    job.placedTotal());
                if (!QUEUE.isEmpty()) {
                    ACTIVE.add(QUEUE.poll());
                }
            } else if (done == 0) {
                break;
            }
        }
        if (placed > 0 && !busy()) {
            LOGGER.info("construction idle; placed {} blocks this tick", placed);
        }
    }
}
