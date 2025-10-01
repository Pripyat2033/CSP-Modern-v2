package com.ben.csp.radiology;

import net.minecraft.entity.effect.StatusEffect;

/**
 * Represents a single biological effect triggered by a specific radiation dose.
 */
public record DoseEffect(
    double thresholdSv,
    StatusEffect statusEffect,
    int durationTicks,
    int amplifier,
    float directDamagePerSecond
) {}