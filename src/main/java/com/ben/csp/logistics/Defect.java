package com.ben.csp.logistics;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec3d;

/**
 * Represents a single discovered or potential defect in a material batch.
 * @param type A string identifier for the type of defect (e.g., "CRACK", "IMPURITY").
 * @param severity How serious the defect is (0.0 to 1.0).
 * @param discoverability How easy it is to find (0.0 to 1.0).
 * @param location The relative position of the defect within the batch/block.
 */
public record Defect(String type, double severity, double discoverability, Vec3d location) {
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("type", this.type);
        nbt.putDouble("severity", this.severity);
        nbt.putDouble("discoverability", this.discoverability);
        NbtList locationNbt = new NbtList();
        locationNbt.add(NbtDouble.of(this.location.x));
        locationNbt.add(NbtDouble.of(this.location.y));
        locationNbt.add(NbtDouble.of(this.location.z));
        nbt.put("location", locationNbt);
        return nbt;
    }

    public static Defect fromNbt(NbtCompound nbt) {
        String type = nbt.getString("type");
        double severity = nbt.getDouble("severity");
        double discoverability = nbt.getDouble("discoverability");
        NbtList locationNbt = nbt.getList("location", 6); // 6 is NbtDouble.TYPE
        Vec3d location = new Vec3d(locationNbt.getDouble(0), locationNbt.getDouble(1), locationNbt.getDouble(2));
        return new Defect(type, severity, discoverability, location);
    }
}