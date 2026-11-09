package com.ben.csp.entity.goals;

import com.ben.csp.entity.CspPersonEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

/**
 * Walks a worker to its assigned workplace and has it "work" there (seated,
 * facing the post) until something else claims it. Runs on the server, which
 * is where entity AI actually ticks.
 */
public class WorkAtAssignedPostGoal extends Goal {

    private static final double ARRIVE_SQUARED = 4.0;

    private final CspPersonEntity worker;

    public WorkAtAssignedPostGoal(CspPersonEntity worker) {
        this.worker = worker;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (worker.isFollowing()) {
            return false;
        }
        BlockPos post = worker.getWorkplacePos();
        return post != null && distanceSquaredTo(post) > ARRIVE_SQUARED;
    }

    @Override
    public void start() {
        BlockPos post = worker.getWorkplacePos();
        worker.getNavigation().startMovingTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 0.6);
    }

    @Override
    public boolean shouldContinue() {
        if (worker.isFollowing()) {
            return false;
        }
        BlockPos post = worker.getWorkplacePos();
        return post != null && distanceSquaredTo(post) > ARRIVE_SQUARED;
    }

    @Override
    public void tick() {
        BlockPos post = worker.getWorkplacePos();
        if (post == null) {
            return;
        }
        if (distanceSquaredTo(post) <= ARRIVE_SQUARED) {
            worker.setSitting(true);
            worker.getLookControl().lookAt(post.getX() + 0.5, post.getY() + 1.0, post.getZ() + 0.5, 30.0f, 30.0f);
        } else {
            worker.setSitting(false);
        }
    }

    @Override
    public void stop() {
        worker.setSitting(false);
    }

    private double distanceSquaredTo(BlockPos post) {
        return worker.squaredDistanceTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5);
    }
}
