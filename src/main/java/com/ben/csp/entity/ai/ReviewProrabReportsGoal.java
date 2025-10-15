package com.ben.csp.entity.ai;

import com.ben.csp.entity.ProrabEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.nbt.NbtCompound;

import java.util.EnumSet;

/**
 * Science Grade: AI Goal for a Prorab to find a quiet place (their desk) to review
 * documents from their queue and consolidate them into a high-level report.
 */
public class ReviewProrabReportsGoal extends Goal {
    private final ProrabEntity prorab;
    private int reviewCooldown;

    public ReviewProrabReportsGoal(ProrabEntity prorab) {
        this.prorab = prorab;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        // Can start if the document queue is getting full and the Prorab is not already busy.
        return this.prorab.getDocumentQueueSize() > 5 && this.prorab.getNavigation().isIdle();
    }

    @Override
    public boolean shouldContinue() {
        // Continue until the queue is empty.
        return this.prorab.getDocumentQueueSize() > 0;
    }

    @Override
    public void start() {
        this.reviewCooldown = 0;
        // In a full implementation, this would navigate to a POI of type "OFFICE_DESK".
        // For now, it just stops moving to "concentrate".
        this.prorab.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.reviewCooldown++;
        if (this.reviewCooldown > 20) { // Process one document per second.
            NbtCompound document = this.prorab.getNextReport();
            if (document != null) {
                // In a real implementation, this would aggregate data.
                // For now, we just "process" it.
                this.prorab.addHighLevelReport(document); // Add to the outgoing report queue.
            }
            this.reviewCooldown = 0;
        }
    }

    @Override
    public void stop() {
        this.reviewCooldown = 0;
    }
}