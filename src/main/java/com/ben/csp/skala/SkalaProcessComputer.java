package com.ben.csp.skala;

import com.google.common.collect.Maps;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * "Science Grade": A PersistentState that simulates the SKALA process computer.
 * It manages ~12,000 input channels and updates them with a realistic 2-5 second latency.
 */
public class SkalaProcessComputer extends PersistentState {
    private static final String NAME = "skala_process_computer";
    private static final String CHANNELS_KEY = "Channels";
    private static final String CODE_KEY = "Code";
    private static final String VALUE_KEY = "Value";
    private static final String SYSTEM_STATE_KEY = "SystemState";
    private static final String STATE_TIMER_KEY = "StateTimer";

    // --- System State Simulation ---
    private enum SystemState { OFFLINE, BOOTING, LOADING_PROGRAM, RUNNING, HALTED }
    private SystemState systemState = SystemState.OFFLINE;
    private int stateTimer = 0;

    // --- V-3M Processor Simulation ---
    private enum ProcessorState { ACTIVE, STANDBY, HALTED }
    private final V3MProcessor[] processors = new V3MProcessor[]{new V3MProcessor(), new V3MProcessor()};

    // --- Data Acquisition Simulation (DREG and DIIS) ---
    private static final int DREG_SCAN_TICKS = 100; // Main scan: 5 seconds (5 * 20 ticks)
    private static final int DIIS_SCAN_TICKS = 10;  // High-speed scan: 0.5 seconds (0.5 * 20 ticks)

    private final List<SkalaChannel> dregChannels = new ArrayList<>(12000); // Main slow channels
    private final List<SkalaChannel> diisChannels = new ArrayList<>(200);   // High-priority fast channels
    private final Map<String, SkalaChannel> channelMap = Maps.newHashMap(); // Unified map for quick lookups

    private int dregScanIndex = 0;
    private int diisScanIndex = 0;

    private static class V3MProcessor {
        ProcessorState state = ProcessorState.HALTED;
    }

    public SkalaProcessComputer() {
        // Default state: one processor on standby, one halted.
        processors[0].state = ProcessorState.STANDBY;
        processors[1].state = ProcessorState.HALTED;
        this.systemState = SystemState.OFFLINE;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putString(SYSTEM_STATE_KEY, this.systemState.name());
        nbt.putInt(STATE_TIMER_KEY, this.stateTimer);

        NbtList channelData = new NbtList();
        for (SkalaChannel channel : dregChannels) { // Only save DREG channels for now
            NbtCompound channelNbt = new NbtCompound();
            channelNbt.putString(CODE_KEY, channel.getCode());
            channelNbt.putInt(VALUE_KEY, (int) channel.getValue());
            channelData.add(channelNbt);
        }
        nbt.put(CHANNELS_KEY, channelData);
        return nbt;
    }

    public static SkalaProcessComputer fromNbt(NbtCompound nbt) {
        SkalaProcessComputer skala = new SkalaProcessComputer();
        if (nbt.contains(SYSTEM_STATE_KEY, NbtElement.STRING_TYPE)) {
            skala.systemState = SystemState.valueOf(nbt.getString(SYSTEM_STATE_KEY));
        }
        skala.stateTimer = nbt.getInt(STATE_TIMER_KEY);
        // In a full implementation, we would re-link data sources here.
        // For now, we just read the last known values.
        NbtList channelData = nbt.getList("Channels", NbtElement.COMPOUND_TYPE);
        // This is a simplified load; a real one would need to reconstruct the channels.
        return skala;
    }

    public static SkalaProcessComputer get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(SkalaProcessComputer::fromNbt, SkalaProcessComputer::new, NAME);
    }

    /**
     * The main computer simulation loop. This is called every server tick.
     */
    public void tick(World world) {
        if (systemState == SystemState.OFFLINE || systemState == SystemState.HALTED) {
            return;
        }

        stateTimer--;
        if (stateTimer > 0) return;

        switch (systemState) {
            case BOOTING -> {
                // Finished booting, now start loading the main program (e.g., PRIZMA)
                systemState = SystemState.LOADING_PROGRAM;
                stateTimer = 20 * 30; // Simulate a 30-second program load from tape
            }
            case LOADING_PROGRAM -> {
                // Finished loading, system is now operational
                systemState = SystemState.RUNNING;
                processors[0].state = ProcessorState.ACTIVE;
                processors[1].state = ProcessorState.STANDBY;
            }
            case RUNNING -> {
                // Run the two separate data acquisition loops
                runDregScan(world);
                runDiisScan(world);
            }
            case OFFLINE, HALTED -> {
                // Do nothing
            }
        }
    }

    /**
     * Simulates the main, slow DREG polling loop for all ~12,000 channels.
     */
    private void runDregScan(World world) {
        if (dregChannels.isEmpty()) return;
        int channelsToScan = (int) Math.ceil((double) dregChannels.size() / DREG_SCAN_TICKS);
        for (int i = 0; i < channelsToScan; i++) {
            if (dregScanIndex >= dregChannels.size()) dregScanIndex = 0;
            dregChannels.get(dregScanIndex).update(world);
            dregScanIndex++;
        }
    }

    /**
     * Simulates the faster DIIS polling loop for high-priority channels.
     */
    private void runDiisScan(World world) {
        if (diisChannels.isEmpty()) return;
        int channelsToScan = (int) Math.ceil((double) diisChannels.size() / DIIS_SCAN_TICKS);
        for (int i = 0; i < channelsToScan; i++) {
            if (diisScanIndex >= diisChannels.size()) diisScanIndex = 0;
            diisChannels.get(diisScanIndex).update(world);
            diisScanIndex++;
        }
    }

    /**
     * Starts the boot sequence of the SKALA computer.
     */
    public void startBootSequence() {
        if (systemState == SystemState.OFFLINE) {
            this.systemState = SystemState.BOOTING;
            this.stateTimer = 20 * 5; // Simulate a 5-second boot time
        }
    }

    public void registerChannel(SkalaChannel channel) {
        registerChannel(channel, EnumSet.of(SkalaChannel.ScanType.DREG));
    }

    public void registerChannel(SkalaChannel channel, EnumSet<SkalaChannel.ScanType> scanTypes) {
        if (!channelMap.containsKey(channel.getCode())) {
            channelMap.put(channel.getCode(), channel);
            if (scanTypes.contains(SkalaChannel.ScanType.DREG)) {
                dregChannels.add(channel);
            }
            if (scanTypes.contains(SkalaChannel.ScanType.DIIS)) {
                diisChannels.add(channel);
            }
        }
    }

    @Nullable
    public SkalaChannel getChannel(String code) {
        return channelMap.get(code);
    }
    public String getSystemState() { return this.systemState.name(); }
}