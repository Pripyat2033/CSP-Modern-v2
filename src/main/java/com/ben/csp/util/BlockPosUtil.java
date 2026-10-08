package com.ben.csp.util;

import net.minecraft.util.math.BlockPos;

/**
 * Utility class for encoding and decoding BlockPos offsets into a single integer.
 * This is a common workaround for the absence of a BlockPosProperty in Minecraft's state manager.
 */
public class BlockPosUtil {

    // We will pack two small signed integers into one integer.
    // To handle negative numbers, we add an offset to make them non-negative before encoding.
    // A range of -2 to +2 (5 values) is sufficient for a 5x5 structure centered on the main block.
    private static final int BITS = 3; // 3 bits can store 2^3 = 8 values (e.g., -3 to 4)
    private static final int MASK = (1 << BITS) - 1;
    private static final int OFFSET = 1 << (BITS - 1); // Offset to handle negative numbers, e.g., 4

    /**
     * Encodes the relative offset from a main block to a part block into a single integer.
     * Supports both positive and negative offsets.
     * @param mainPos The position of the main block.
     * @param partPos The position of the part block.
     * @return An integer representing the encoded offset.
     */
    public static int encodeRelativePos(BlockPos mainPos, BlockPos partPos) {
        // Get relative offsets
        int dx = partPos.getX() - mainPos.getX() + OFFSET;
        int dz = partPos.getZ() - mainPos.getZ() + OFFSET;

        // Check if the offset is within the encodable range
        if ((dx & ~MASK) != 0 || (dz & ~MASK) != 0) {
            throw new IllegalArgumentException("Relative position is too far to be encoded: " + (partPos.subtract(mainPos)));
        }

        // Pack into a single integer
        return (dx & MASK) | ((dz & MASK) << BITS);
    }

    /**
     * Decodes an integer from a block state back into the main block's absolute position.
     * @param encodedValue The integer value from the block state.
     * @param partPos The absolute position of the part block.
     * @return The calculated absolute position of the main block, or null if the value is invalid.
     */
    public static BlockPos decodeToMainPos(int encodedValue, BlockPos partPos) {
        // Unpack the values
        int dx = (encodedValue & MASK) - OFFSET;
        int dz = ((encodedValue >> BITS) & MASK) - OFFSET;

        // Calculate the main block's position
        return partPos.add(-dx, 0, -dz);
    }
}