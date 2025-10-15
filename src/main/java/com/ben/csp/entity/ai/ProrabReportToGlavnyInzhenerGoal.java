package com.ben.csp.entity.ai;

import com.ben.csp.entity.ProrabEntity;

import net.minecraft.entity.ai.goal.Goal;

public class ProrabReportToGlavnyInzhenerGoal extends Goal { 
    private final ProrabEntity prorab;

    public ProrabReportToGlavnyInzhenerGoal(ProrabEntity prorab) {
        this.prorab = prorab;
    }

    @Override
    public boolean canStart() {
        // This goal can start if the Prorab has high-level reports to deliver.
        return this.prorab.hasPendingReports();
    }
}