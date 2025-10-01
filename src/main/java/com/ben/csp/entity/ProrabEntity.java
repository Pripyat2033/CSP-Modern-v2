package com.ben.csp.entity;

import com.ben.csp.personnel.PersonnelRecord;
import com.google.common.collect.Lists;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;

/**
 * Science Grade: A "Prorab" (Прораб) - a works foreman or site supervisor.
 * This NPC is responsible for direct oversight of construction tasks and personnel.
 */
public class ProrabEntity extends PathAwareEntity {

    private final PsychologicalState psychologicalState = new PsychologicalState();
    private final List<NbtCompound> documentQueue = Lists.newArrayList();

    public ProrabEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createProrabAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        // AI Goals like ReviewProrabReportsGoal and ProrabReportToGlavnyInzhenerGoal will be restored next.
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
    }

    public void acceptReportFromMaster(MasterEntity master, String report) {
        // Placeholder for more complex logic.
        this.sendMessage(Text.literal("Prorab received report from " + master.getName().getString() + ": " + report));
        this.psychologicalState.addStress(0.05f); // Receiving reports is stressful.
    }

    public void queueDocument(NbtCompound document) {
        this.documentQueue.add(document);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        psychologicalState.writeToNbt(nbt);

        NbtList documents = new NbtList();
        documents.addAll(documentQueue);
        nbt.put("DocumentQueue", documents);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        psychologicalState.readFromNbt(nbt);

        if (nbt.contains("DocumentQueue", 9)) { // 9 = List type
            documentQueue.clear();
            NbtList documents = nbt.getList("DocumentQueue", 10); // 10 = Compound type
            documents.forEach(doc -> documentQueue.add((NbtCompound) doc));
        }
    }
}