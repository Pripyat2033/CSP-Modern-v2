package com.ben.csp.entity;

import com.ben.csp.CSPMod;
import com.ben.csp.build.BuildTask;
import com.ben.csp.entity.ai.GoToBuildSiteGoal;
import com.ben.csp.entity.ai.PerformWorkPackageGoal;
import com.ben.csp.entity.ai.ReportToProrabGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

/**
 * Science Grade: A "Master" (Мастер) - a master craftsman or skilled worker.
 * This is the primary "doer" NPC that performs construction tasks.
 */
public class MasterEntity extends PathAwareEntity {

    private enum State {
        IDLE, GOING_TO_SITE, WORKING, REPORTING
    }

    @Nullable
    private UUID prorabUuid;
    @Nullable
    private BuildTask currentTask;
    
    // Simple progress tracking: how many reports are expected for the current task.
    private int expectedReports = 0;

    private State currentState = State.IDLE;

    // AI Goals stored for dynamic addition/removal
    private final GoToBuildSiteGoal goToBuildSiteGoal = new GoToBuildSiteGoal(this);
    private final PerformWorkPackageGoal performWorkPackageGoal = new PerformWorkPackageGoal(this);
    private final ReportToProrabGoal reportToProrabGoal = new ReportToProrabGoal(this);

    public MasterEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createMasterAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f)); // Low priority, always active
    }

    @Nullable public UUID getProrabUuid() { return prorabUuid; }
    public void setProrabUuid(@Nullable UUID uuid) { this.prorabUuid = uuid; }

    @Nullable
    public BuildTask getCurrentTask() { return currentTask; }

    /**
     * Assigns a new task to this Master and sets the number of expected reports.
     * @param task The build task to perform.
     * @param expectedReports The number of Stroitel reports needed to complete the task.
     */
    public void assignTask(@Nullable BuildTask task, int expectedReports) {
        this.currentTask = task;
        this.expectedReports = task != null ? expectedReports : 0;

        if (task != null) {
            CSPMod.LOGGER.info("Master {} assigned new task: '{}' at {}. Expecting {} reports.", this.getUuidAsString(), task.getDescription(), task.getDestination(), expectedReports);
        } else {
            CSPMod.LOGGER.info("Master {} task cleared.", this.getUuidAsString());
        }

        if (task != null) {
            transitionToState(State.GOING_TO_SITE);
        } else {
            transitionToState(State.IDLE);
        }
    }

    public void transitionToState(State newState) {
        if (this.currentState == newState) return;

        CSPMod.LOGGER.debug("Master {} transitioning from {} to {}", this.getUuidAsString(), this.currentState, newState);
        this.currentState = newState;

        // Clear all task-specific goals before adding the correct one.
        goalSelector.remove(goToBuildSiteGoal);
        goalSelector.remove(performWorkPackageGoal);
        goalSelector.remove(reportToProrabGoal);

        switch (newState) {
            case IDLE -> {} // No task goals
            case GOING_TO_SITE -> goalSelector.add(1, goToBuildSiteGoal);
            case WORKING -> goalSelector.add(2, performWorkPackageGoal);
            case REPORTING -> goalSelector.add(3, reportToProrabGoal);
        }
    }

    public void acceptReportFromStroitel(StroitelEntity stroitel) {
        this.sendMessage(Text.literal("Master received report from Stroitel " + stroitel.getName().getString()));
        if (this.expectedReports > 0) { this.expectedReports--; }

        if (this.expectedReports <= 0) {
            this.sendMessage(Text.literal("Work package complete. Ready to report to Prorab."));
            transitionToState(State.REPORTING);
        }
    }

    /**
     * Resets the entity's state, clearing its current task and prorab assignment.
     */
    public void resetTask() {
        this.assignTask(null, 0);
        this.setProrabUuid(null);
    }

    /**
     * Checks if the current work package is complete.
     * @return true if the task is non-null and all reports have been received.
     */
    public boolean isWorkPackageComplete() {
        return this.currentTask != null && this.expectedReports <= 0;
    }

    /**
     * Finds nearby idle Stroitel entities and assigns them to this Master.
     * @param count The number of subordinates to recruit.
     * @return The number of subordinates successfully assigned.
     */
    public int findAndAssignSubordinates(int count) {
        if (this.getWorld().isClient()) return 0;

        ServerWorld world = (ServerWorld) this.getWorld(); 
        Box searchBox = this.getBoundingBox().expand(32);
        var availableStroitels = world.getEntitiesByType(ModEntities.STROITEL, searchBox, 
                stroitel -> stroitel.getMasterUuid() == null)
                .stream()
                .limit(count)
                .toList();
        availableStroitels.forEach(stroitel -> stroitel.setMasterUuid(this.getUuid()));
        return availableStroitels.size();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (prorabUuid != null) { nbt.putUuid("ProrabUUID", prorabUuid); }
        if (currentTask != null) { nbt.put("CurrentTask", currentTask.toNbt()); }
        nbt.putInt("ExpectedReports", this.expectedReports);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("ProrabUUID")) { this.prorabUuid = nbt.getUuid("ProrabUUID"); }
        this.currentTask = nbt.contains("CurrentTask") ? BuildTask.fromNbt(nbt.getCompound("CurrentTask")) : null;
        this.expectedReports = nbt.getInt("ExpectedReports");

        // Re-evaluate state after loading to ensure correctness across game sessions.
        if (this.currentTask == null) {
            transitionToState(State.IDLE);
        } else if (isWorkPackageComplete()) {
            transitionToState(State.REPORTING);
        } else {
            // If task exists but is not complete, assume it needs to go to the site.
            // A more complex system might save the exact state.
            transitionToState(State.GOING_TO_SITE);
        }
    }
}