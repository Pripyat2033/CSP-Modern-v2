package com.ben.csp.entity;

import com.ben.csp.entity.ai.Dosye;
import com.ben.csp.entity.ai.DosyeNbt;
import com.ben.csp.entity.ai.DynamicPersonalityEngine;
import com.ben.csp.entity.ai.Garrisonable;
import com.ben.csp.entity.ai.WorkerChat;
import com.ben.csp.entity.goals.FollowPlayerGoal;
import com.ben.csp.entity.goals.WorkAtAssignedPostGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for every CSP human worker.
 *
 * <p>Each worker gets a procedurally generated dossier ({@link Dosye}) on spawn,
 * belongs to an enterprise, and can be posted to a workplace. Concrete
 * subclasses declare their {@link WorkerRole}, which drives both their
 * attributes and their idle behaviour.
 */
public abstract class CspPersonEntity extends PathAwareEntity implements Garrisonable, EnterprisePersonnel {

    @Nullable
    private Dosye dosye;
    private String enterpriseName = "";
    @Nullable
    private BlockPos workplacePos;
    @Nullable
    private BlockPos garrisonPos;
    private boolean sitting;
    private int talkTicks;
    private int talkTargetId = -1;
    private int followTicks;
    private int followTargetId = -1;

    protected CspPersonEntity(EntityType<? extends CspPersonEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        super.tick();
        // Generate the dossier lazily: NBT load has run by the first tick, so a
        // worker restored from a save keeps its file instead of a fresh one.
        if (!getWorld().isClient() && dosye == null) {
            this.dosye = DynamicPersonalityEngine.getInstance().generateDosye();
            applyDosyeName();
        }
        // While being spoken to, the worker stops and turns to face the player,
        // so a conversation reads as one instead of a chase.
        if (!getWorld().isClient() && followTicks > 0) {
            followTicks--;
        }
        if (!getWorld().isClient() && talkTicks > 0) {
            talkTicks--;
            getNavigation().stop();
            var target = getWorld().getEntityById(talkTargetId);
            if (target instanceof PlayerEntity p && p.isAlive()) {
                getLookControl().lookAt(p, 30.0f, 30.0f);
            } else {
                talkTicks = 0;
            }
        }
    }

    /** The role this worker fills; drives behaviour and the chain of command. */
    public abstract WorkerRole getRole();

    public static DefaultAttributeContainer.Builder createPersonAttributes() {
        return PathAwareEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new FollowPlayerGoal(this));
        this.goalSelector.add(0, new SwimGoal(this));
        switch (getRole().getProfile()) {
            case GARRISON -> {
                this.goalSelector.add(1, new WorkAtAssignedPostGoal(this));
                this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
                this.goalSelector.add(3, new LookAroundGoal(this));
            }
            case LABOUR, SUPERVISE -> {
                this.goalSelector.add(1, new WorkAtAssignedPostGoal(this));
                this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8f));
                this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
                this.goalSelector.add(4, new LookAroundGoal(this));
            }
            case SURVEY -> {
                this.goalSelector.add(1, new WanderAroundFarGoal(this, 1.0f));
                this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
                this.goalSelector.add(3, new LookAroundGoal(this));
            }
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!getWorld().isClient() && hand == Hand.MAIN_HAND) {
            WorkerChat.speak(this, player);
            // Stop and turn to the speaker, and hold it for a few seconds.
            this.talkTicks = 120;
            this.talkTargetId = player.getId();
            getNavigation().stop();
            getLookControl().lookAt(player, 30.0f, 30.0f);
        }
        return ActionResult.SUCCESS;
    }

    private void applyDosyeName() {
        if (dosye != null) {
            setCustomName(Text.literal(dosye.getFullName() + " " + roleTag(getRole())));
            setCustomNameVisible(true);
        }
    }

    /** A short, plain tag for the job, shown on the nametag. */
    public static String roleTag(WorkerRole role) {
        return switch (role) {
            case TOLKACH -> "[labourer]";
            case STROITEL -> "[builder]";
            case GEODEZIST -> "[surveyor]";
            case UCHETCHIK -> "[record keeper]";
            case BRIGADIER -> "[brigade leader]";
            case SEKRETAR -> "[secretary]";
            case NACHALNIK_SNABZHENIYA -> "[supply chief]";
            case MASTER -> "[foreman]";
            case PRORAB -> "[site superintendent]";
            case GLAVNY_INZHENER -> "[chief engineer]";
            case FACILITY_DIRECTOR -> "[facility director]";
            case ENTERPRISE_DIRECTOR -> "[enterprise director]";
            case KOMMUTATOR -> "[switchboard operator]";
        };
    }

    @Nullable
    public Dosye getDosye() {
        return dosye;
    }

    // ------------------------------------------------------------------ post

    @Nullable
    public BlockPos getWorkplacePos() {
        return workplacePos;
    }

    public void setWorkplacePos(@Nullable BlockPos workplacePos) {
        this.workplacePos = workplacePos;
    }

    // ------------------------------------------------------------- orders

    /** "Come here" — walk to the speaker, then stop. */
    public void orderCome(PlayerEntity player) {
        startFollowing(player, 120);
    }

    /** "Follow me" — stay with the speaker for a long while. */
    public void orderFollow(PlayerEntity player) {
        startFollowing(player, 12000);
    }

    private void startFollowing(PlayerEntity player, int ticks) {
        this.followTargetId = player.getId();
        this.followTicks = ticks;
        this.sitting = false;
        getNavigation().stop();
    }

    /** "Stand down" — stop where you are. */
    public void orderStandDown() {
        this.followTicks = 0;
        this.sitting = true;
        getNavigation().stop();
    }

    /** "To your post" — resume the assigned work. */
    public void orderGoToPost() {
        this.followTicks = 0;
        this.sitting = false;
        getNavigation().stop();
    }

    public boolean isFollowing() {
        return followTicks > 0;
    }

    public int getFollowTargetId() {
        return followTargetId;
    }

    // ---------------------------------------------------------- Garrisonable

    @Override
    public void setGarrisonPos(@Nullable BlockPos pos) {
        this.garrisonPos = pos;
    }

    @Override
    @Nullable
    public BlockPos getGarrisonPos() {
        return garrisonPos != null ? garrisonPos : workplacePos;
    }

    @Override
    public void setSitting(boolean sitting) {
        this.sitting = sitting;
    }

    @Override
    public boolean isSitting() {
        return sitting;
    }

    // -------------------------------------------------- EnterprisePersonnel

    @Override
    public Enterprise getEnterprise() {
        return null;
    }

    @Override
    public void setEnterprise(Enterprise enterprise) {
        this.enterpriseName = enterprise == null ? "" : enterprise.getEnterpriseName();
    }

    @Override
    public String getEnterpriseName() {
        return enterpriseName;
    }

    @Override
    public void setEnterpriseName(String name) {
        this.enterpriseName = name == null ? "" : name;
    }

    @Override
    public boolean hasEnterprise() {
        return !enterpriseName.isEmpty();
    }

    @Override
    public String getPersonnelRole() {
        return getRole().getId();
    }

    @Override
    public void setPersonnelRole(String role) {
        // A worker's role is fixed by its class; nothing to set.
    }

    // ------------------------------------------------------------------ NBT

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (dosye != null) {
            nbt.put("Dosye", DosyeNbt.toNbt(dosye));
        }
        nbt.putString("EnterpriseName", enterpriseName);
        if (workplacePos != null) {
            nbt.putLong("WorkplacePos", workplacePos.asLong());
        }
        if (garrisonPos != null) {
            nbt.putLong("GarrisonPos", garrisonPos.asLong());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Dosye")) {
            this.dosye = DosyeNbt.fromNbt(nbt.getCompound("Dosye"));
            applyDosyeName();
        }
        this.enterpriseName = nbt.getString("EnterpriseName");
        if (nbt.contains("WorkplacePos")) {
            this.workplacePos = BlockPos.fromLong(nbt.getLong("WorkplacePos"));
        }
        if (nbt.contains("GarrisonPos")) {
            this.garrisonPos = BlockPos.fromLong(nbt.getLong("GarrisonPos"));
        }
    }
}
