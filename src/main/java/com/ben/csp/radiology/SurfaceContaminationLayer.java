package com.ben.csp.radiology;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ARL: Represents the physical layer of contamination on a surface,
 * with properties like cohesion that change over time.
 */
public class SurfaceContaminationLayer {
    // Isotope Name -> Activity (Bq)
    public final Map<String, Double> composition = new ConcurrentHashMap<>();
    // How "caked on" the contamination is. 0.0 = loose dust, 1.0 = solid grime.
    public double cohesion = 0.0;

    public void addDeposition(HotParticle particle) {
        composition.merge(particle.isotope.name(), particle.activityBq, Double::sum);
    }

    public double getTotalActivity() {
        return composition.values().stream().mapToDouble(d -> d).sum();
    }

    public void tick(boolean isRaining) {
        if (isRaining) {
            // Rain makes the layer more cohesive (caked on)
            cohesion = Math.min(1.0, cohesion + 0.001);
        } else {
            // Slowly dries out and becomes less cohesive, but never fully if it's been wet.
            cohesion = Math.max(0.0, cohesion - 0.0001);
        }
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putDouble("cohesion", cohesion);
        NbtList compList = new NbtList();
        composition.forEach((isotope, activity) -> {
            NbtCompound isoNbt = new NbtCompound();
            isoNbt.putString("name", isotope);
            isoNbt.putDouble("activity", activity);
            compList.add(isoNbt);
        });
        nbt.put("composition", compList);
        return nbt;
    }
}