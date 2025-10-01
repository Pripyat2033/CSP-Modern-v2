package com.ben.csp.entity;

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
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Science Grade: A "Master" (Мастер) - a master craftsman or skilled worker.
 * This is the primary "doer" NPC that performs construction tasks.
 */
public class MasterEntity extends PathAwareEntity {

    @Nullable
    private UUID prorabUuid;
    @Nullable
    private BuildTask currentTask;

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
        this.goalSelector.add(1, new GoToBuildSiteGoal(this));
        this.goalSelector.add(2, new PerformWorkPackageGoal(this));
        this.goalSelector.add(3, new ReportToProrabGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
    }

    @Nullable public UUID getProrabUuid() { return prorabUuid; }
    public void setProrabUuid(@Nullable UUID uuid) { this.prorabUuid = uuid; }

    @Nullable public BuildTask getCurrentTask() { return currentTask; }
    public void setCurrentTask(@Nullable BuildTask task) { this.currentTask = task; }

    public void acceptReportFromStroitel(StroitelEntity stroitel) {
        this.sendMessage(Text.literal("Master received report from Stroitel " + stroitel.getName().getString()));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (prorabUuid != null) { nbt.putUuid("ProrabUUID", prorabUuid); }
        if (currentTask != null) { nbt.put("CurrentTask", currentTask.toNbt()); }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("ProrabUUID")) { this.prorabUuid = nbt.getUuid("ProrabUUID"); }
        if (nbt.contains("CurrentTask")) { this.currentTask = BuildTask.fromNbt(nbt.getCompound("CurrentTask")); }
    }
}