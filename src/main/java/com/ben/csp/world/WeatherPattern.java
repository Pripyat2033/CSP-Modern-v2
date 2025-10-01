package com.ben.csp.world;

import net.minecraft.util.math.Vec3d;

/**
 * A data record representing a specific, stable weather condition for a period of time.
 * @param durationTicks The length of this weather pattern in Minecraft ticks.
 * @param windVector A vector representing wind direction and speed.
 * @param precipitationIntensity A value from 0.0 (clear) to 1.0 (heavy).
 */
public record WeatherPattern(
    int durationTicks, Vec3d windVector, double precipitationIntensity
) {
}