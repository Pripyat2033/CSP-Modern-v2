package com.ben.csp.skala;

import net.minecraft.util.math.BlockPos;

/**
 * "Science Grade": A functional interface representing the action performed
 * when a specific SKALA code is entered. This allows for a fully data-driven
 * and extensible command system, capable of handling thousands of unique codes
 * for querying, control, and program execution.
 */
@FunctionalInterface
public interface SkalaAction {
    void execute(SkalaCoreBlockEntity core, BlockPos requestDevicePos);
}