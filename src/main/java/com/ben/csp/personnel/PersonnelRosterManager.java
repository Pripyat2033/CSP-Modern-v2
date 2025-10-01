package com.ben.csp.personnel;

import com.google.common.collect.Maps;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Science Grade: A central singleton that maintains a complete roster of all personnel
 * involved in the project. It tracks their roles, assignments, and status.
 * This class persists across server restarts.
 */
public class PersonnelRosterManager extends PersistentState {
    private static final String NAME = "csp_personnel_roster";
    private final Map<UUID, PersonnelRecord> roster = Maps.newHashMap();

    public PersonnelRosterManager() {}

    public static PersonnelRosterManager get(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        PersistentStateManager stateManager = world.getPersistentStateManager();
        return stateManager.getOrCreate(PersonnelRosterManager::fromNbt, PersonnelRosterManager::new, NAME);
    }

    public void addPersonnel(PersonnelRecord record) {
        roster.put(record.getUuid(), record);
        markDirty();
    }

    @Nullable
    public PersonnelRecord getPersonnel(UUID uuid) {
        return roster.get(uuid);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList rosterList = new NbtList();
        for (PersonnelRecord record : roster.values()) {
            rosterList.add(record.toNbt());
        }
        nbt.put("Roster", rosterList);
        return nbt;
    }

    public static PersonnelRosterManager fromNbt(NbtCompound nbt) {
        PersonnelRosterManager manager = new PersonnelRosterManager();
        NbtList rosterList = nbt.getList("Roster", 10); // 10 = Compound Tag type
        for (int i = 0; i < rosterList.size(); i++) {
            PersonnelRecord record = PersonnelRecord.fromNbt(rosterList.getCompound(i));
            manager.addPersonnel(record);
        }
        return manager;
    }
}