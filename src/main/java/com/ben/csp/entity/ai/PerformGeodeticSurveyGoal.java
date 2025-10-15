package com.ben.csp.entity.ai;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.entity.GeodezistEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Science Grade: AI Goal for a Geodezist to travel to the corners of a survey area and place marker blocks.
 */
public class PerformGeodeticSurveyGoal extends Goal {
    private enum State {
        MOVING,
        PLACING_MARKER
    }

    private final GeodezistEntity geodezist;
    private List<BlockPos> markerPoints;
    private int currentPointIndex;
    private int placementCooldown;
    private State currentState;

    public PerformGeodeticSurveyGoal(GeodezistEntity geodezist) {
        this.geodezist = geodezist;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // Can start if we have a survey area and are not currently working on one.
        return this.geodezist.getSurveyArea() != null && (this.markerPoints == null || this.currentPointIndex >= this.markerPoints.size());
    }

    @Override
    public void start() {
        BlockBox area = this.geodezist.getSurveyArea();
        if (area == null) return;

        // Define the corners of the survey area as the points to mark.
        this.markerPoints = new ArrayList<>();
        this.markerPoints.add(new BlockPos(area.getMinX(), 0, area.getMinZ()));
        this.markerPoints.add(new BlockPos(area.getMaxX(), 0, area.getMinZ()));
        this.markerPoints.add(new BlockPos(area.getMaxX(), 0, area.getMaxZ()));
        this.markerPoints.add(new BlockPos(area.getMinX(), 0, area.getMaxZ()));

        this.currentPointIndex = 0;
        moveToNextPoint();
    }

    @Override
    public boolean shouldContinue() {
        return this.geodezist.getSurveyArea() != null && this.currentPointIndex < this.markerPoints.size();
    }

    @Override
    public void tick() {
        BlockPos currentTarget = this.markerPoints.get(this.currentPointIndex);

        if (this.currentState == State.MOVING && this.geodezist.getBlockPos().isWithinDistance(currentTarget, 2.0)) {
            // Arrived at the destination
            this.currentState = State.PLACING_MARKER;
            this.placementCooldown = 40; // Set cooldown for 2 seconds of "work"
        }

        if (this.currentState == State.PLACING_MARKER) {
            this.placementCooldown--;
            if (this.placementCooldown <= 0) {
                // Cooldown finished, place the marker and move to the next point
                int y = this.geodezist.getWorld().getTopY(Heightmap.Type.WORLD_SURFACE, currentTarget.getX(), currentTarget.getZ());
                this.geodezist.getWorld().setBlockState(new BlockPos(currentTarget.getX(), y, currentTarget.getZ()), ModBlocks.GEODETIC_MARKER_BLOCK.getDefaultState());
                
                this.currentPointIndex++;
                moveToNextPoint();
            }
        }
    }

    @Override
    public void stop() {
        // If the goal is stopping because the survey is finished, discard the entity.
        if (this.currentPointIndex >= this.markerPoints.size()) {
            this.geodezist.discard();
        }
        this.geodezist.getNavigation().stop();
    }

    private void moveToNextPoint() {
        if (this.currentPointIndex < this.markerPoints.size()) {
            this.currentState = State.MOVING;
            BlockPos nextPoint = this.markerPoints.get(this.currentPointIndex);
            this.geodezist.getNavigation().startMovingTo(nextPoint.getX(), nextPoint.getY(), nextPoint.getZ(), 1.0D);
        } else {
            // Survey complete.
            this.geodezist.discard();
        }
    }
}