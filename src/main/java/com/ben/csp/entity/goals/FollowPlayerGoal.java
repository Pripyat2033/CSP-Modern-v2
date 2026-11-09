package com.ben.csp.entity.goals;

import com.ben.csp.entity.CspPersonEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

/**
 * Walks the worker to the player who ordered it to come or follow. Runs while the
 * order lasts; the worker resumes its post when the order runs out or is cancelled.
 */
public class FollowPlayerGoal extends Goal {

    private static final double START_DISTANCE_SQUARED = 9.0;
    private static final double STOP_DISTANCE_SQUARED = 2.0;

    private final CspPersonEntity worker;
    private PlayerEntity target;

    public FollowPlayerGoal(CspPersonEntity worker) {
        this.worker = worker;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!worker.isFollowing()) {
            return false;
        }
        if (!(worker.getWorld().getEntityById(worker.getFollowTargetId()) instanceof PlayerEntity p)
            || !p.isAlive()) {
            return false;
        }
        this.target = p;
        return worker.squaredDistanceTo(p) > START_DISTANCE_SQUARED;
    }

    @Override
    public void start() {
        worker.getNavigation().startMovingTo(target, 1.0);
    }

    @Override
    public boolean shouldContinue() {
        return worker.isFollowing() && target != null && target.isAlive()
            && worker.squaredDistanceTo(target) > STOP_DISTANCE_SQUARED;
    }

    @Override
    public void tick() {
        worker.getNavigation().startMovingTo(target, 1.0);
        worker.getLookControl().lookAt(target, 30.0f, 30.0f);
    }

    @Override
    public void stop() {
        target = null;
        worker.getNavigation().stop();
    }
}
