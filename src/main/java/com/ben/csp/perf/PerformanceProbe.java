package com.ben.csp.perf;

import net.minecraft.server.MinecraftServer;

/**
 * Measures how long a server tick takes, so "can it cope?" is answered by a number
 * rather than an opinion. One tick should take under 50 ms (twenty ticks a second).
 * The average is smoothed so a single spike does not mislead; the peak is kept so a
 * spike is not hidden.
 */
public final class PerformanceProbe {

    private PerformanceProbe() {}

    private static long tickStart;
    private static double averageMspt;
    private static double peakMspt;

    public static void onTickStart(MinecraftServer server) {
        tickStart = System.nanoTime();
    }

    public static void onTickEnd(MinecraftServer server) {
        if (tickStart == 0) {
            return;
        }
        double ms = (System.nanoTime() - tickStart) / 1_000_000.0;
        averageMspt = averageMspt == 0.0 ? ms : averageMspt * 0.95 + ms * 0.05;
        if (ms > peakMspt) {
            peakMspt = ms;
        }
    }

    public static double averageMspt() {
        return averageMspt;
    }

    public static double peakMspt() {
        return peakMspt;
    }

    public static void resetPeak() {
        peakMspt = 0.0;
    }

    /** Plain verdict against the 50 ms budget for 20 ticks a second. */
    public static String verdict() {
        double a = averageMspt;
        if (a == 0.0) {
            return "no reading yet";
        }
        if (a < 30.0) {
            return "comfortable";
        }
        if (a < 50.0) {
            return "playable";
        }
        if (a < 80.0) {
            return "heavy — below 20 ticks a second";
        }
        return "overloaded";
    }
}
