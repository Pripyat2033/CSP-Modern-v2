package com.ben.csp.logistics;

import net.minecraft.util.math.Vec3d;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MaterialBatch {
    public enum QualityStatus { PENDING, PASSED, DEFECTIVE, QUARANTINED }

    public final UUID batchId;
    public final String materialId;
    public int quantity;
    public final double inherentQuality; // 0.0 to 1.0
    public final String supplierId;
    public QualityStatus status;
    private final List<Defect> defects;
    private final List<Defect> foundDefects;

    public MaterialBatch(String materialId, int quantity, double quality, String supplierId) {
        this.batchId = UUID.randomUUID();
        this.materialId = materialId;
        this.quantity = quantity;
        this.inherentQuality = quality;
        this.supplierId = supplierId;
        this.status = QualityStatus.PENDING;
        this.defects = generateDefects(quality);
        this.foundDefects = new ArrayList<>();
    }

    private MaterialBatch(UUID batchId, String materialId, int quantity, double quality, String supplierId, QualityStatus status, List<Defect> defects, List<Defect> foundDefects) {
        this.batchId = batchId;
        this.materialId = materialId;
        this.quantity = quantity;
        this.inherentQuality = quality;
        this.supplierId = supplierId;
        this.status = status;
        this.defects = defects;
        this.foundDefects = foundDefects;
    }

    private List<Defect> generateDefects(double quality) {
        List<Defect> generated = new ArrayList<>();
        // Bad quality = more defects
        int numDefects = (int) Math.round((1.0 - quality) * 5 * Math.random());
        for (int i = 0; i < numDefects; i++) {
            generated.add(new Defect(
                "IMPURITY",
                Math.random() * 0.5,
                Math.random(),
                new Vec3d(Math.random(), Math.random(), Math.random())
            ));
        }
        return generated;
    }

    public List<Defect> getDefects() { return defects; }
    public boolean wasDefectFound(Defect d) { return foundDefects.contains(d); }
    public void markDefectAsFound(Defect d) { if (!foundDefects.contains(d)) foundDefects.add(d); }
    public MaterialBatch.QualityStatus getStatus() { return this.status; }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("batchId", this.batchId);
        nbt.putString("materialId", this.materialId);
        nbt.putInt("quantity", this.quantity);
        nbt.putDouble("inherentQuality", this.inherentQuality);
        nbt.putString("supplierId", this.supplierId);
        nbt.putString("status", this.status.name());

        NbtList defectsNbt = new NbtList();
        this.defects.forEach(defect -> defectsNbt.add(defect.toNbt()));
        nbt.put("defects", defectsNbt);

        NbtList foundDefectsNbt = new NbtList();
        this.foundDefects.forEach(defect -> foundDefectsNbt.add(defect.toNbt()));
        nbt.put("foundDefects", foundDefectsNbt);

        return nbt;
    }

    public static MaterialBatch fromNbt(NbtCompound nbt) {
        UUID batchId = nbt.getUuid("batchId");
        String materialId = nbt.getString("materialId");
        int quantity = nbt.getInt("quantity");
        double quality = nbt.getDouble("inherentQuality");
        String supplierId = nbt.getString("supplierId");
        QualityStatus status = QualityStatus.valueOf(nbt.getString("status"));

        List<Defect> defects = new ArrayList<>();
        NbtList defectsNbt = nbt.getList("defects", 10);
        defectsNbt.forEach(defectNbt -> defects.add(Defect.fromNbt((NbtCompound) defectNbt)));

        List<Defect> foundDefects = new ArrayList<>();
        NbtList foundDefectsNbt = nbt.getList("foundDefects", 10);
        foundDefectsNbt.forEach(defectNbt -> foundDefects.add(Defect.fromNbt((NbtCompound) defectNbt)));

        return new MaterialBatch(batchId, materialId, quantity, quality, supplierId, status, defects, foundDefects);
    }
}