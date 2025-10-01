package com.ben.csp.entity;

import com.ben.csp.entity.ai.ManageBrigadeGoal;
import com.google.common.collect.Lists;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Science Grade: A "Brigadier" (Бригадир) - a team leader of a "Brigade" (work crew).
 * This NPC manages a small team of Stroiteli and reports to a Master.
 */
public class BrigadierEntity extends PathAwareEntity implements EnterprisePersonnel {

    @Nullable
    private UUID masterUuid;
    private final List<UUID> brigadeMemberUuids = Lists.newArrayList();
    private String enterpriseName = "";


    public BrigadierEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createBrigadierAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ManageBrigadeGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
    }

    @Nullable public UUID getMasterUuid() { return masterUuid; }
    public void setMasterUuid(@Nullable UUID uuid) { this.masterUuid = uuid; }

    public List<UUID> getBrigadeMemberUuids() { return brigadeMemberUuids; }

    @Override
    public String getEnterpriseName() { return this.enterpriseName; }
    @Override
    public void setEnterpriseName(String name) { this.enterpriseName = name; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (masterUuid != null) { nbt.putUuid("MasterUUID", masterUuid); }
        nbt.putString("EnterpriseName", this.enterpriseName);
        NbtList brigadeList = new NbtList();
        for (UUID memberUuid : brigadeMemberUuids) {
            brigadeList.add(NbtString.of(memberUuid.toString()));
        }
        nbt.put("BrigadeMembers", brigadeList);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("MasterUUID")) { this.masterUuid = nbt.getUuid("MasterUUID"); }
        this.enterpriseName = nbt.getString("EnterpriseName");
        if (nbt.contains("BrigadeMembers", NbtElement.LIST_TYPE)) {
            brigadeMemberUuids.clear();
            NbtList brigadeList = nbt.getList("BrigadeMembers", NbtElement.STRING_TYPE);
            for (NbtElement element : brigadeList) {
                brigadeMemberUuids.add(UUID.fromString(element.asString()));
            }
        }
    }
}