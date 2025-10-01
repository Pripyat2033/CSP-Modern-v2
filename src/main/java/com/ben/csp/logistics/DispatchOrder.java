package com.ben.csp.logistics;

import net.minecraft.util.math.BlockPos;
import java.util.Map;

/**
 * Represents an order to dispatch a set of materials to a destination.
 * @param destination The target block position for the delivery.
 * @param materials A map of material identifiers to the quantity required.
 */
public record DispatchOrder(BlockPos destination, Map<String, Integer> materials) {}