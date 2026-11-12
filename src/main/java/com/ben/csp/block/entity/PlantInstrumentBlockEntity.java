package com.ben.csp.block.entity;

import com.ben.csp.rbmk.plant.PlantInstruments;
import com.ben.csp.rbmk.plant.PlantSystem;
import com.ben.csp.rbmk.plant.PlantTicker;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

/**
 * Base for the plant's field instruments — the flow-control valves and the ball
 * flow meters on the МЦК pipework. Each instrument is bound to one fuel channel
 * and reads its live state from the running {@link PlantSystem}: the valve its
 * position, the meter its channel flow. The value is synced to the client so the
 * renderer can show it.
 *
 * <p>Nothing here is invented. With no plant running the instrument reads "dead"
 * (closed, no flow); once the plant is started it shows the real computed value,
 * so what the operator sees in the world is the same number the physics holds.
 */
public abstract class PlantInstrumentBlockEntity extends BlockEntity {

    /** The channel (lattice x,z) this instrument serves. */
    protected int channelX;
    protected int channelZ;
    /** Is there a running plant this instrument can read? */
    protected boolean live;
    /** The reading, 0..1 (valve position, or meter float height). */
    protected double reading;
    /** Meter only: the channel mass flow (kg/s). */
    protected double flowKgS;
    /** Meter only: the channel is starving. */
    protected boolean starving;
    /** Valve only: the valve is fully open. */
    protected boolean open;

    protected PlantInstrumentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setChannel(int x, int z) {
        this.channelX = x;
        this.channelZ = z;
    }

    public int channelX() {
        return channelX;
    }

    public int channelZ() {
        return channelZ;
    }

    public boolean live() {
        return live;
    }

    public double reading() {
        return reading;
    }

    public double flowKgS() {
        return flowKgS;
    }

    public boolean starving() {
        return starving;
    }

    public boolean open() {
        return open;
    }

    /** Server tick: read the plant, and push the value to clients when it changes. */
    protected void tickInstrument() {
        boolean wasLive = live;
        double oldReading = reading;
        double oldFlow = flowKgS;
        boolean oldStarving = starving;
        boolean oldOpen = open;

        read();

        if (wasLive != live || oldReading != reading || oldFlow != flowKgS
                || oldStarving != starving || oldOpen != open) {
            markDirty();
            if (getWorld() != null && !getWorld().isClient()) {
                getWorld().updateListeners(getPos(), getCachedState(), getCachedState(), 3);
            }
        }
    }

    /** Read this instrument's live value from the running plant (server side). */
    protected abstract void read();

    protected static PlantSystem plant() {
        return PlantTicker.plant();
    }

    /** Which circuit (0 or 1) a channel belongs to, by its side of the reactor. */
    protected int circuit() {
        return PlantInstruments.circuitOf(channelZ);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("chX", channelX);
        nbt.putInt("chZ", channelZ);
        nbt.putBoolean("live", live);
        nbt.putDouble("reading", reading);
        nbt.putDouble("flow", flowKgS);
        nbt.putBoolean("starving", starving);
        nbt.putBoolean("open", open);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        channelX = nbt.getInt("chX");
        channelZ = nbt.getInt("chZ");
        live = nbt.getBoolean("live");
        reading = nbt.getDouble("reading");
        flowKgS = nbt.getDouble("flow");
        starving = nbt.getBoolean("starving");
        open = nbt.getBoolean("open");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
