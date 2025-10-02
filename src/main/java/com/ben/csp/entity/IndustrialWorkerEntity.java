package com.ben.csp.entity;

import com.ben.csp.entity.ai.Garrisonable;
import com.ben.csp.entity.ai.GarrisonGoal;
import com.ben.csp.entity.ai.ReportToManagerGoal;
import com.ben.csp.entity.ai.WorkAtAssignedPostGoal;
import com.ben.csp.radiology.PsychologicalState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class IndustrialWorkerEntity extends PathAwareEntity implements Garrisonable, EnterprisePersonnel {
    private String enterpriseName = "";
    private BlockPos workplacePos = BlockPos.ZERO;
    @Nullable private Identifier processId;
    @Nullable private UUID managerUuid;
    private final PsychologicalState psychologicalState = new PsychologicalState();

    public IndustrialWorkerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createIndustrialWorkerAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ReportToManagerGoal(this));
        this.goalSelector.add(2, new WorkAtAssignedPostGoal(this, 1.0D));
        this.goalSelector.add(3, new GarrisonGoal(this, 1.0D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));

    }

    @Override
    public void setEnterpriseName(String name) { this.enterpriseName = name; }
    @Override
    public String getEnterpriseName() { return this.enterpriseName; }

    public void setWorkplacePos(BlockPos pos) { this.workplacePos = pos; }
    @Override
    public BlockPos getWorkplacePos() { return this.workplacePos; }

    public void setProcessId(@Nullable Identifier id) { this.processId = id; }
    @Nullable public Identifier getProcessId() { return this.processId; }

    public void setManagerUuid(@Nullable UUID uuid) { this.managerUuid = uuid; }
    @Nullable public UUID getManagerUuid() { return this.managerUuid; }

    public PsychologicalState getPsychologicalState() { return this.psychologicalState; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("EnterpriseName", this.enterpriseName);
        if (workplacePos != null && !workplacePos.equals(BlockPos.ZERO)) {
            nbt.put("WorkplacePos", NbtHelper.fromBlockPos(workplacePos));
        }
        if (this.processId != null) {
            nbt.putString("ProcessId", this.processId.toString());
        }
        if (this.managerUuid != null) {
            nbt.putUuid("ManagerUuid", this.managerUuid);
        }
        this.psychologicalState.writeToNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.enterpriseName = nbt.getString("EnterpriseName");
        if (nbt.contains("WorkplacePos", 10)) { // 10 = Compound Tag type
            this.workplacePos = NbtHelper.toBlockPos(nbt.getCompound("WorkplacePos"));
        }
        if (nbt.contains("ProcessId", 8)) { // 8 = String type
            this.processId = new Identifier(nbt.getString("ProcessId"));
        }
        if (nbt.contains("ManagerUuid")) {
            this.managerUuid = nbt.getUuid("ManagerUuid");
        }
        this.psychologicalState.readFromNbt(nbt);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient()) {
            player.sendMessage(Text.literal("Worker " + this.getUuidAsString().substring(0, 8) + " reporting for duty."), false);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockPos getGarrisonPos() {
        // An Industrial Worker garrisons at its assigned workplace.
        return this.workplacePos;
    }

    @Override
    public int getGarrisonRadius() {
        // They should stay very close to their post.
        return 2;
    }
}