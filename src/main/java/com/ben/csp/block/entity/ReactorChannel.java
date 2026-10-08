package com.ben.csp.block.entity;

import net.minecraft.nbt.NbtCompound;

/**
 * Represents the state of a single channel within the RBMK reactor core.
 * This class is designed to be mutable and will be updated by the physics simulator.
 */
public class ReactorChannel {

    private ChannelType type;

    // --- Physical Properties ---
    private double temperature; // in Celsius
    private double neutronFlux; // arbitrary units
    private double waterFlow; // kg/s
    private double steamQuality; // 0.0 (all water) to 1.0 (all steam)

    // --- Isotope Properties (for Xenon Pit simulation) ---
    private double iodine135; // concentration
    private double xenon135; // concentration

    // --- Material Properties ---
    private float fuelBurnup; // 0.0 (fresh) to 1.0 (spent)
    private float controlRodInsertion; // 0.0 (fully withdrawn) to 1.0 (fully inserted)
    private float graphiteIntegrity; // 1.0 (pristine) to 0.0 (crumbled)

    public ReactorChannel(ChannelType type) {
        this.type = type;
        this.temperature = 20.0; // Start at ambient temperature
        this.graphiteIntegrity = 1.0f;
        // All other values default to 0
    }

    public ChannelType getType() { return type; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public double getNeutronFlux() { return neutronFlux; }
    public void setNeutronFlux(double neutronFlux) { this.neutronFlux = neutronFlux; }
    public double getWaterFlow() { return waterFlow; }
    public void setWaterFlow(double waterFlow) { this.waterFlow = waterFlow; }
    public double getSteamQuality() { return steamQuality; }
    public void setSteamQuality(double steamQuality) { this.steamQuality = steamQuality; }
    public double getIodine135() { return iodine135; }
    public void setIodine135(double iodine135) { this.iodine135 = iodine135; }
    public double getXenon135() { return xenon135; }
    public void setXenon135(double xenon135) { this.xenon135 = xenon135; }
    public float getFuelBurnup() { return fuelBurnup; }
    public void setFuelBurnup(float fuelBurnup) { this.fuelBurnup = fuelBurnup; }
    public float getControlRodInsertion() { return controlRodInsertion; }
    public void setControlRodInsertion(float controlRodInsertion) { this.controlRodInsertion = controlRodInsertion; }
    public float getGraphiteIntegrity() { return graphiteIntegrity; }
    public void setGraphiteIntegrity(float graphiteIntegrity) { this.graphiteIntegrity = graphiteIntegrity; }

    /**
     * Reads the channel's state from an NBT compound.
     * @param nbt The NBT data to read from.
     */
    public void readNbt(NbtCompound nbt) {
        this.type = ChannelType.valueOf(nbt.getString("Type"));
        this.temperature = nbt.getDouble("Temperature");
        this.neutronFlux = nbt.getDouble("NeutronFlux");
        this.waterFlow = nbt.getDouble("WaterFlow");
        this.steamQuality = nbt.getDouble("SteamQuality");
        this.iodine135 = nbt.getDouble("Iodine135");
        this.xenon135 = nbt.getDouble("Xenon135");
        this.fuelBurnup = nbt.getFloat("FuelBurnup");
        this.controlRodInsertion = nbt.getFloat("ControlRodInsertion");
        this.graphiteIntegrity = nbt.getFloat("GraphiteIntegrity");
    }

    /**
     * Writes the channel's state to an NBT compound.
     * @return A new NBT compound with this channel's data.
     */
    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Type", this.type.name());
        nbt.putDouble("Temperature", this.temperature);
        nbt.putDouble("NeutronFlux", this.neutronFlux);
        nbt.putDouble("WaterFlow", this.waterFlow);
        nbt.putDouble("SteamQuality", this.steamQuality);
        nbt.putDouble("Iodine135", this.iodine135);
        nbt.putDouble("Xenon135", this.xenon135);
        nbt.putFloat("FuelBurnup", this.fuelBurnup);
        nbt.putFloat("ControlRodInsertion", this.controlRodInsertion);
        nbt.putFloat("GraphiteIntegrity", this.graphiteIntegrity);
        return nbt;
    }

    /**
     * Creates a ReactorChannel from NBT data.
     * @param nbt The NBT data.
     * @return A new ReactorChannel instance.
     */
    public static ReactorChannel fromNbt(NbtCompound nbt) {
        ChannelType type = ChannelType.EMPTY;
        try {
            type = ChannelType.valueOf(nbt.getString("Type"));
        } catch (IllegalArgumentException e) {
            // Handle cases where the type name is invalid, default to EMPTY
        }
        ReactorChannel channel = new ReactorChannel(type);
        channel.readNbt(nbt);
        return channel;
    }
}