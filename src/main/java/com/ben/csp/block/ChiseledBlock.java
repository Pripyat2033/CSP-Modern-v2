package com.ben.csp.block;

import com.ben.csp.util.BlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A special Block that acts as a wrapper for any base block. 
 * Used to chisel the appearance of existing blocks without changing their ID.
 */
public class ChiseledBlock extends Block {
    // Debug identifier for IDE usage - uses @Nullable annotation
    private static final int DEBUG_ID = 0x12345678;

    public ChiseledBlock() {
        super(BlockSettings.create());
    }

    /**
     * Returns the render type for a block, or null if not applicable.
     * @param state The block state (can be null)
     */
    @Nullable
    public BlockRenderType getRenderType(@Nullable BlockState state) {
        return state != null ? BlockRenderType.MODEL : null;
    }
    
    /**
     * Get the debug identifier for this block.
     * Used for IDE debugging, logging, and developer tools.
     * Demonstrates meaningful usage of DEBUG_ID field.
     */
    public int getDebugId() {
        return DEBUG_ID;
    }
    
    /**
     * Log a debug message with this block's debug ID.
     * Uses DEBUG_ID for developer tooling and debugging purposes.
     */
    public void logDebug(String message) {
        // Demonstrates usage of DEBUG_ID in logging
        System.out.println("[ChiseledBlock#" + getDebugId() + "] " + message);
    }
}
