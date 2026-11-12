package com.ben.csp.rbmk.plant;

import com.ben.csp.rbmk.RbmkLattice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a running {@link PlantSystem} to the readings its field instruments show.
 * The valve and the meter block entities hold no physics of their own — they ask
 * this class, so the number on the block is exactly the number the plant holds.
 *
 * <p>This class is deliberately free of Minecraft types: it can be exercised by
 * the standalone self-test against a real, stepped plant, which is how we prove
 * the instruments show the physics rather than a constant.
 */
public final class PlantInstruments {

    private PlantInstruments() {}

    private static Map<Long, Integer> channelIndex;

    /** Which circuit a channel belongs to, by its side of the reactor (0 or 1). */
    public static int circuitOf(int channelZ) {
        return channelZ >= 0 ? 1 : 0;
    }

    /** A valve's position, 0 (shut) .. 1 (open), from the plant's own valve. */
    public static double valvePosition(PlantSystem p, int channelZ) {
        Valve[] valves = p.valves();
        return valves[Math.floorMod(circuitOf(channelZ), valves.length)].position();
    }

    public static boolean valveOpen(PlantSystem p, int channelZ) {
        Valve[] valves = p.valves();
        return valves[Math.floorMod(circuitOf(channelZ), valves.length)].open();
    }

    /** The rotameter index for a lattice channel, or -1 if it is not a channel. */
    public static int channelIndexOf(int x, int z) {
        if (channelIndex == null) {
            Map<Long, Integer> m = new HashMap<>();
            List<RbmkLattice.Channel> chs = RbmkLattice.channels();
            for (int i = 0; i < chs.size(); i++) {
                RbmkLattice.Channel c = chs.get(i);
                m.put(key(c.x(), c.z()), i);
            }
            channelIndex = m;
        }
        Integer i = channelIndex.get(key(x, z));
        return i == null ? -1 : i;
    }

    /** The channel's live mass flow (kg/s), or 0 if it is not a channel. */
    public static double channelFlowKgS(PlantSystem p, int channelX, int channelZ) {
        int idx = channelIndexOf(channelX, channelZ);
        Rotameter[] meters = p.rotameters();
        if (idx < 0 || idx >= meters.length) {
            return 0;
        }
        return meters[idx].flowKgS();
    }

    /** The meter's float height, 0..1 of full scale. */
    public static double channelReading(PlantSystem p, int channelX, int channelZ) {
        int idx = channelIndexOf(channelX, channelZ);
        Rotameter[] meters = p.rotameters();
        if (idx < 0 || idx >= meters.length) {
            return 0;
        }
        return meters[idx].reading();
    }

    /** Is the channel starving (well below nominal flow)? */
    public static boolean channelStarving(PlantSystem p, int channelX, int channelZ) {
        int idx = channelIndexOf(channelX, channelZ);
        Rotameter[] meters = p.rotameters();
        if (idx < 0 || idx >= meters.length) {
            return false;
        }
        return meters[idx].starving();
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
