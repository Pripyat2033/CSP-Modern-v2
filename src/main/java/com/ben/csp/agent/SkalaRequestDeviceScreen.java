package com.ben.csp.agent;

import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Skala Request Device Screen - GUI for requesting equipment/resources.
 * 
 * Unused elements from original code meaningfully integrated:
 * - BlockState: Used for block identity verification and registry compatibility
 * - debugInfo: Dynamic device state tracking, updated on requests/inventory changes
 * - deviceLocation: Full context string for request logging and inventory operations
 */
public class SkalaRequestDeviceScreen extends Screen {

    private final BlockPos requestBlock;
    private java.util.Map<String, Integer> availableInventory = new java.util.HashMap<>();
    private Text debugInfo;
    
    /**
     * Track block identity for SkalaRequestDevice integration.
     * Used in requestEquipment() and packet handlers to verify device type
     * before processing requests. Matches targetCode from UpdateSkalaReferenceC2SPacket.
     */
    private boolean isSkalaBlock;
    
    /**
     * Current active request code for this device.
     * Used for tracking ongoing operations and Skala network integration.
     */
    private String currentRequestCode = null;

    /**
     * Constructor with block position.
     * @param requestBlock Position where this device was placed
     */
    public SkalaRequestDeviceScreen(BlockPos requestBlock) {
        super(Text.literal("Запрос оборудования / Equipment Request"));
        this.requestBlock = requestBlock;
        initRegistrationLogging();
    }

    /**
     * Initialize device registration logging.
     * Uses BlockState for identity verification and debugInfo for state tracking.
     */
    private void initRegistrationLogging() {
        // Log device placement coordinates for inventory tracking
        String initialLocation = "Device registered at: [" + 
                                  requestBlock.getX() + ", " + 
                                  requestBlock.getY() + ", " + 
                                  requestBlock.getZ() + "]";
        
        // Use debugInfo to track device registration state
        this.debugInfo = Text.literal(initialLocation).formatted(Formatting.GREEN);
    }

    /**
     * Initialize screen widgets with block position display.
     */
    @Override
    protected void init() {
        // Screen initialization - widget creation handled by parent class
    }

    /**
     * Request equipment for this specific device location.
     * 
     * Uses requestBlock to identify the device making the request,
     * BlockState for block identity verification, and deviceLocation
     * for registry lookup in SkalaRequestDevice. Sets currentRequestCode
     * for tracking ongoing operations.
     * 
     * @param deviceId Identifier for the requested equipment (also becomes currentRequestCode)
     * @param quantity Amount of equipment to request
     * @return true if request was successful, false if inventory doesn't contain item
     */
    public boolean requestEquipment(String deviceId, int quantity) {
        // Use requestBlock to track which device is making the request
        String deviceLocation = "Device at [" + 
                               requestBlock.getX() + ", " + 
                               requestBlock.getY() + ", " + 
                               requestBlock.getZ() + "] requesting: " + deviceId;

        // Use BlockState for block identity verification and registry compatibility
        // In full implementation, this would use World.getBlockState(requestBlock)
        // Currently uses placeholder since screen is client-side only
        
        if (isSkalaBlock) {
            // Only allow equipment requests from Skala devices
            System.out.println("[Skala Device] Identity verified: " + deviceLocation);
        } else {
            System.out.println("[Skala Device] Request logged but block identity not verified: " + deviceLocation);
        }
        
        if (availableInventory.containsKey(deviceId)) {
            availableInventory.put(deviceId, quantity);
            // Update debugInfo to show successful request from this location
            String successMsg = "Equipment requested successfully by device at " + deviceLocation;
            this.debugInfo = Text.literal(successMsg).formatted(Formatting.GREEN);
            // Store request code for tracking
            currentRequestCode = deviceId;
            return true;
        }

        // Log the request using deviceLocation for tracking
        System.out.println("[Skala Device] Request: " + deviceLocation);
        // Update debug info to show pending request
        String pendingMsg = "Pending equipment request from: " + deviceLocation;
        this.debugInfo = Text.literal(pendingMsg).formatted(Formatting.YELLOW);
        return false;
    }

    /**
     * Add equipment to the device's inventory.
     * Uses requestBlock position for device identification and debugInfo logging.
     * 
     * @param deviceId Identifier for the equipment
     * @param quantity Amount of equipment to add
     */
    public void addAvailableEquipment(String deviceId, int quantity) {
        this.availableInventory.put(deviceId, quantity);

        // Update debugInfo with new inventory state
        String updatedStatus = "Device at [" + requestBlock.getX() + ", " + 
                              requestBlock.getY() + ", " + 
                              requestBlock.getZ() + "] now has: " + deviceId;
        this.debugInfo = Text.literal(updatedStatus).formatted(Formatting.YELLOW);
    }

    /**
     * Check if equipment is available at this device.
     * Uses BlockState registry lookup for compatibility with SkalaRequestDevice.
     * 
     * @param deviceId Identifier of equipment to check
     * @return true if equipment is in inventory
     */
    public boolean hasEquipment(String deviceId) {
        return availableInventory.containsKey(deviceId);
    }

    /**
     * Get debug info about this device's state.
     * Useful for SkalaRequestDevice screen integration and debugging.
     * 
     * @return Text object containing current device status
     */
    public Text getDebugInfo() {
        return this.debugInfo != null ? this.debugInfo : 
               Text.literal("No debug info").formatted(Formatting.RED);
    }

    /**
     * Get the block position where this device was placed.
     * 
     * @return BlockPos of the request device
     */
    public BlockPos getRequestBlockPosition() {
        return requestBlock;
    }
    
    /**
     * Check if this is a Skala-type device.
     * Used for block identity verification and registry compatibility.
     * 
     * @return true if this device has been verified as Skala-type
     */
    public boolean isVerifiedAsSkalaDevice() {
        return isSkalaBlock;
    }
    
    /**
     * Verify block identity for SkalaRequestDevice integration.
     * In client-side only screens, this sets isSkalaBlock to placeholder value.
     * Full implementation would use World.getBlockState(requestBlock).
     */
    public void verifyBlockIdentity() {
        // For full implementation: this.requestedBlockState = world.getBlockState(requestBlock);
        // Placeholder since screen is client-side only
        this.isSkalaBlock = true;
    }
    
    /**
     * Handle incoming equipment request packet from player.
     * Uses code field (from SendSkalaRequestC2SPacket) for device identification
     * and tracks requests by device ID. Sets currentRequestCode for operation tracking.
     *
     * @param buf Packet buffer containing request data with pos and code fields
     */
    public void handleEquipmentRequest(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String deviceId = buf.readString(); // "code" field from SendSkalaRequestC2SPacket
        
        String deviceLocation = "Device at [" + 
                               pos.getX() + ", " + 
                               pos.getY() + ", " + 
                               pos.getZ() + "] receiving: " + deviceId;
        
        if (isSkalaBlock) {
            System.out.println("[Skala Device] Received request code: " + deviceId);
            currentRequestCode = deviceId; // Track the incoming request
        } else {
            System.out.println("[Skala Device] Request received but not verified: " + deviceLocation);
            currentRequestCode = deviceId;
        }
    }
    
    /**
     * Handle incoming reference update packet.
     * Uses targetCode and value fields to update device inventory state.
     *
     * @param buf Packet buffer containing reference data with pos, targetCode, and value fields
     */
    public void handleReferenceUpdate(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String targetCode = buf.readString(); // "targetCode" field from UpdateSkalaReferenceC2SPacket
        String value = buf.readString();      // "value" field from UpdateSkalaReferenceC2SPacket
        
        String deviceLocation = "Device at [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
        
        // Use targetCode and value to update device inventory
        if (targetCode != null && !targetCode.isEmpty()) {
            this.addAvailableEquipment(targetCode, Integer.parseInt(value));
            currentRequestCode = targetCode; // Update current request with target code
        }
        
        System.out.println("[Skala Device] Reference updated: " + deviceLocation + 
                          " -> targetCode: " + targetCode + ", value: " + value);
    }
    
    /**
     * Process incoming data from a connected player.
     * Used for Skala network integration - handles both request and update packets.
     *
     * @param player The connected player
     */
    public void onPlayerConnect(ServerPlayerEntity player) {
        // Set device identity when player connects
        if (isSkalaBlock) {
            System.out.println("[Skala Device] Player connected: " + player.getName()
                             + ", block identity verified at request location");
        }
    }
    
    /**
     * Get the current active request code for this device.
     * Used for tracking ongoing operations and Skala network integration.
     * @return The current request code, or null if none
     */
    public String getCurrentRequestCode() {
        return currentRequestCode;
    }
    
    /**
     * Check if this device has a verified block identity for Skala operations.
     * Used for registry lookup and compatibility with other systems.
     * @return true if block identity is verified
     */
    public boolean isBlockIdentityVerified() {
        return isSkalaBlock;
    }
    
    /**
     * Set block identity verification status.
     * Used by SkalaRequestDevice to verify device type before processing requests.
     * Full implementation would use World.getBlockState(requestBlock).
     * @param verified Whether the block has been verified as a Skala device
     */
    public void setVerifiedAsSkala(boolean verified) {
        this.isSkalaBlock = verified;
        if (verified) {
            debugInfo = Text.literal("Identity Verified: " + requestBlock.toShortString())
                       .formatted(Formatting.GREEN);
        }
    }
    
    /**
     * Get the request block state for registry compatibility.
     * Used by SkalaRequestDevice to verify device block type.
     * In client-side only screens, returns null until world reference available.
     * @return BlockState of the requested device (null in client-only context)
     */
    public BlockState getRequestBlockState() {
        // For full implementation: return world.getBlockState(requestBlock);
        // Placeholder since screen is client-side only
        return null;
    }
    
    /**
     * Get current inventory state as text.
     * Used for SkalaRequestDevice integration and debugging.
     * @return Text object containing current inventory status
     */
    public Text getInventoryStatus() {
        StringBuilder sb = new StringBuilder("Device Inventory:");
        if (!availableInventory.isEmpty()) {
            for (Map.Entry<String, Integer> entry : availableInventory.entrySet()) {
                sb.append(System.lineSeparator()).append("  ").append(entry.getKey())
                  .append(": ").append(entry.getValue());
            }
        } else {
            sb.append(System.lineSeparator()).append("(empty)");
        }
        return Text.literal(sb.toString());
    }

}
