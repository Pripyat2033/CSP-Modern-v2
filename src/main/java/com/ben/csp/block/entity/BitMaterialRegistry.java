package com.ben.csp.block.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * BitMaterial Registry - Material property lookups.
 */
public class BitMaterialRegistry {

    private static final Map<String, MaterialProperties> MATERIAL_PROPERTIES = new HashMap<>();

    public static void registerMaterial(String name, MaterialProperties props) {
        MATERIAL_PROPERTIES.put(name.toLowerCase(), props);
    }

    public static Optional<MaterialProperties> getMaterialProperties(String name) {
        return Optional.ofNullable(MATERIAL_PROPERTIES.get(name.toLowerCase()));
    }

    public static boolean hasMaterial(String name) {
        return MATERIAL_PROPERTIES.containsKey(name.toLowerCase());
    }

    public static class MaterialProperties {
        public final double thermalConductivity;
        public final double electricalResistivity;
        public final double density;

        public MaterialProperties(double tc, double er, double d) {
            this.thermalConductivity = tc;
            this.electricalResistivity = er;
            this.density = d;
        }
    }

    static {
        registerMaterial("structural_concrete", new MaterialProperties(1.0, 2.5e12, 880.0));
        registerMaterial("graphite_block", new MaterialProperties(120.0, 3.3e5, 720.0));
    }
}
