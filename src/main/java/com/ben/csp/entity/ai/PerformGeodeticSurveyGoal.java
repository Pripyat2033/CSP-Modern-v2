package com.ben.csp.entity.ai;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.entity.GeodezistEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Science Grade: AI Goal for a Geodezist to travel to the corners of a survey area and place marker blocks.
 */
public class PerformGeodeticSurveyGoal extends Goal {
    private final GeodezistEntity geodezist;
    private List<BlockPos> markerPoints;
    private int currentPointIndex;
    private int placementCooldown;

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

        this.currentPointIndex = -1;
        moveToNextPoint();
    }

    @Override
    public boolean shouldContinue() {
        return this.geodezist.getSurveyArea() != null && this.currentPointIndex < this.markerPoints.size();
    }

    @Override
    public void tick() {
        if (this.geodezist.getNavigation().isIdle()) {
            // Arrived at marker point.
            this.placementCooldown++;
            if (this.placementCooldown > 40) { // Simulate 2 seconds of work.
                BlockPos targetPos = this.markerPoints.get(this.currentPointIndex);
                int y = this.geodezist.getWorld().getTopY(Heightmap.Type.WORLD_SURFACE, targetPos.getX(), targetPos.getZ());
                this.geodezist.getWorld().setBlockState(new BlockPos(targetPos.getX(), y, targetPos.getZ()), ModBlocks.GEODETIC_MARKER_BLOCK.getDefaultState());
                moveToNextPoint();
            }
        }
    }

    private void moveToNextPoint() {
        this.currentPointIndex++;
        this.placementCooldown = 0;
        if (this.currentPointIndex < this.markerPoints.size()) {
            BlockPos nextPoint = this.markerPoints.get(this.currentPointIndex);
            this.geodezist.getNavigation().startMovingTo(nextPoint.getX(), nextPoint.getY(), nextPoint.getZ(), 1.0D);
        } else {
            // Survey complete.
            this.geodezist.discard();
        }
    }
}