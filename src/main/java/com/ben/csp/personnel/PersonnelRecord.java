package com.ben.csp.personnel;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A data object representing a single person in the PersonnelRosterManager.
 * This is the "Dosye" (file/dossier) for an individual.
 */
public class PersonnelRecord {
    private final UUID uuid;
    private final String name;
    private final String role; // e.g., "Geodezist", "Prorab"
    @Nullable
    private String assignedEnterprise;

    public PersonnelRecord(UUID uuid, String name, String role) {
        this.uuid = uuid;
        this.name = name;
        this.role = role;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public String getRole() { return role; }
    @Nullable
    public String getAssignedEnterprise() { return assignedEnterprise; }
    public void setAssignedEnterprise(@Nullable String assignedEnterprise) { this.assignedEnterprise = assignedEnterprise; }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("UUID", uuid);
        nbt.putString("Name", name);
        nbt.putString("Role", role);
        if (assignedEnterprise != null) {
            nbt.putString("Enterprise", assignedEnterprise);
        }
        return nbt;
    }

    public static PersonnelRecord fromNbt(NbtCompound nbt) {
        PersonnelRecord record = new PersonnelRecord(nbt.getUuid("UUID"), nbt.getString("Name"), nbt.getString("Role"));
        if (nbt.contains("Enterprise")) {
            record.setAssignedEnterprise(nbt.getString("Enterprise"));
        }
        return record;
    }
}