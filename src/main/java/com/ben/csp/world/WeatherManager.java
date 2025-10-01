package com.ben.csp.world;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the weather simulation for the ChSZO, cycling through data-driven patterns.
 * This provides the RCSS with realistic wind and precipitation data.
 */
public class WeatherManager {
    private static final WeatherManager INSTANCE = new WeatherManager();

    private List<WeatherPattern> weatherCycle = new ArrayList<>();
    private int currentPatternIndex = 0;
    private int ticksInCurrentPattern = 0;

    private Vec3d currentWindVector = Vec3d.ZERO;
    private double currentPrecipitationIntensity = 0.0;
    private AtmosphericStability currentStability = AtmosphericStability.NEUTRAL;

    private WeatherManager() {}

    public static WeatherManager getInstance() {
        return INSTANCE;
    }

    public void loadWeatherCycle(List<WeatherPattern> cycle) {
        this.weatherCycle = cycle;
        this.currentPatternIndex = 0;
        this.ticksInCurrentPattern = 0;
        if (!cycle.isEmpty()) {
            applyPattern(cycle.get(0));
        }
    }

    public void onServerTick(MinecraftServer server) {
        if (weatherCycle.isEmpty()) return;

        ticksInCurrentPattern++;

        // ARL: Update atmospheric stability based on time of day
        ServerWorld world = server.getOverworld();
        long timeOfDay = world.getTimeOfDay() % 24000;
        if (timeOfDay > 1000 && timeOfDay < 12000) { // Daytime
            this.currentStability = AtmosphericStability.UNSTABLE;
        } else if (timeOfDay > 13000 && timeOfDay < 23000) { // Nighttime
            this.currentStability = AtmosphericStability.STABLE;
        } else { // Dawn/Dusk
            this.currentStability = AtmosphericStability.NEUTRAL;
        }

        WeatherPattern currentPattern = weatherCycle.get(currentPatternIndex);

        if (ticksInCurrentPattern >= currentPattern.durationTicks()) {
            currentPatternIndex = (currentPatternIndex + 1) % weatherCycle.size();
            ticksInCurrentPattern = 0;
            applyPattern(weatherCycle.get(currentPatternIndex));
        }
    }

    private void applyPattern(WeatherPattern pattern) {
        this.currentWindVector = pattern.windVector();
        this.currentPrecipitationIntensity = pattern.precipitationIntensity();
    }

    public Vec3d getWindVector() { return this.currentWindVector; }
    public double getPrecipitationIntensity() { return this.currentPrecipitationIntensity; }
    public AtmosphericStability getAtmosphericStability() { return this.currentStability; }
}