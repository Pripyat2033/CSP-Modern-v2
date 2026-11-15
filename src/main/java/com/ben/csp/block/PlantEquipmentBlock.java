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

    /**
     * The turbine's cylinder cover. In the machine hall the K-500-65/3000 shows
     * only its five massive white circular covers (one HP + four LP) — the rotors
     * and diaphragms are inside. Painted steel, so white iron.
     */
    public static PlantEquipmentBlock turbineCover() {
        return new PlantEquipmentBlock(Blocks.WHITE_CONCRETE);
    }

    /** The TVV-500 generator body, bolted to the turbine on the same shaft. */
    public static PlantEquipmentBlock generator() {
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

    /** The ГЦН's 6 kV drive motor, mounted above the pump on the same shaft. */
    public static PlantEquipmentBlock pumpMotor() {
        return new PlantEquipmentBlock(Blocks.LIGHT_GRAY_CONCRETE);
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

    // ---- equipment named by the 1983 operating regulation (1E-S-II) ----

    /** Main steam stop valve (ГПЗ, главная паровая задвижка): the big gate valve. */
    public static PlantEquipmentBlock steamStopValve() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Turbine stop/control valve (СРК, стопорно-регулирующий клапан). */
    public static PlantEquipmentBlock turbineStopValve() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** MCP seal-oil pump (МНУ, маслонасос уплотнений). */
    public static PlantEquipmentBlock sealOilPump() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Turbine turning (barring) gear (ВПУ, валоповоротное устройство). */
    public static PlantEquipmentBlock turningGear() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Pump/heat-exchanger unit (НТУ, насосно-теплообменная установка). */
    public static PlantEquipmentBlock heatExchanger() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Clean-condensate tank (БЧК, бак чистого конденсата), the makeup reserve. */
    public static PlantEquipmentBlock cleanCondensateTank() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Compressor station (компрессорная станция): dried instrument air, 6 kgf/cm2. */
    public static PlantEquipmentBlock compressor() {
        return new PlantEquipmentBlock(Blocks.LIGHT_GRAY_CONCRETE);
    }

    /** Wet gas holder (МГ, мокрый газгольдер) of the reactor gas circuit. */
    public static PlantEquipmentBlock gasHolder() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Block control-room desk (БЩУ-О): the operator's panel with its instruments. */
    public static PlantEquipmentBlock controlDesk() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** AZ-5 scram button (КАЗ, кнопка аварийной защиты). */
    public static PlantEquipmentBlock az5Button() {
        return new PlantEquipmentBlock(Blocks.RED_CONCRETE);
    }

    /** Radiation-safety monitor (КРБ, контроль радиационной безопасности). */
    public static PlantEquipmentBlock radiationMonitor() {
        return new PlantEquipmentBlock(Blocks.LIGHT_GRAY_CONCRETE);
    }

    /** Oil circuit breaker (МВ, масляный выключатель). */
    public static PlantEquipmentBlock circuitBreaker() {
        return new PlantEquipmentBlock(Blocks.IRON_BLOCK);
    }

    /** Standby diesel generator (ДГ, дизель-генератор) of the emergency supply. */
    public static PlantEquipmentBlock dieselGenerator() {
        return new PlantEquipmentBlock(Blocks.LIGHT_GRAY_CONCRETE);
    }
}
