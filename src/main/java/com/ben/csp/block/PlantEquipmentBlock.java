package com.ben.csp.block;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

/**
 * A piece of plant equipment as a real block — a steam separator, a turbine, a
 * condenser, a main circulating pump. The RBMK plant is a collection of separate
 * machines, so the world should show them as separate machines, not one block.
 */
public class PlantEquipmentBlock extends Block {

    public PlantEquipmentBlock(Block base) {
        super(FabricBlockSettings.copyOf(base));
    }

    /** Steam separator drum (барабан-сепаратор): thick steel pressure vessel. */
    public static PlantEquipmentBlock steamSeparator() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Turbine (K-500-65/3000): heavy machined steel. */
    public static PlantEquipmentBlock turbine() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Condenser: steel shell. */
    public static PlantEquipmentBlock condenser() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Main circulating pump (ГЦН): pump body. */
    public static PlantEquipmentBlock pump() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Feedwater / deaerator tank: large steel vessel. */
    public static PlantEquipmentBlock feedwater() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** ECCS accumulator: high-pressure nitrogen-blanketed tank. */
    public static PlantEquipmentBlock accumulator() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Piping run: the connecting lines between the machines. */
    public static PlantEquipmentBlock pipe() {
        return new PlantEquipmentBlock(Blocks.COPPER_BLOCK);
    }
}
