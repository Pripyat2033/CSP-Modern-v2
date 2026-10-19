package com.ben.csp.entity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Enterprise - Core enterprise entity with headquarters position and name.
 * Used for connecting personnel entities to their organization.
 */
public class Enterprise {
    
    private final String enterpriseId;
    private final String enterpriseName;
    private final BlockPos hqPos;
    private World world;
    private int employeeCount = 0;
    private @Nullable BlockState headquartersBlockState;
    
    public Enterprise(String id, String name, BlockPos hqPosition) {
        this.enterpriseId = id;
        this.enterpriseName = name;
        this.hqPos = hqPosition;
    }
    
    /**
     * Create enterprise with default ID based on name.
     */
    public Enterprise(String name, BlockPos hqPosition) {
        this(createDefaultId(name), name, hqPosition);
    }
    
    private static String createDefaultId(String name) {
        return "enterprise_" + 
            Character.toUpperCase(name.charAt(0)) + 
            name.substring(1).toLowerCase().replace(" ", "_").replace("-", "_");
    }
    
    public String getEnterpriseId() {
        return enterpriseId;
    }
    
    public String getEnterpriseName() {
        return enterpriseName;
    }
    
    public BlockPos getHqPos() {
        return hqPos;
    }
    
    public World getWorld() {
        return world;
    }
    
    /**
     * Set the world this enterprise belongs to.
     */
    public void setWorld(World world) {
        this.world = world;
    }
    
    public int getEmployeeCount() {
        return employeeCount;
    }
    
    /**
     * Add an employee and update count.
     */
    public void addEmployee() {
        this.employeeCount++;
    }
    
    /**
     * Remove an employee and update count.
     */
    public void removeEmployee() {
        if (employeeCount > 0) {
            this.employeeCount--;
        }
    }
    
    /**
     * Set the headquarters block state (for rendering/interaction).
     */
    public void setHeadquartersBlockState(BlockState state) {
        this.headquartersBlockState = state;
    }
    
    @Nullable
    public BlockState getHeadquartersBlockState() {
        return headquartersBlockState;
    }
    
    /**
     * Check if enterprise has employees.
     */
    public boolean isEmpty() {
        return employeeCount == 0;
    }
    
    /**
     * Get enterprise name with fallback to ID.
     */
    public String getDisplayName() {
        return !enterpriseName.isEmpty() ? enterpriseName : enterpriseId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Enterprise)) return false;
        Enterprise that = (Enterprise) o;
        return enterpriseId != null ? enterpriseId.equals(that.enterpriseId) : false;
    }
    
    @Override
    public int hashCode() {
        return enterpriseId != null ? enterpriseId.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Enterprise{" +
                "enterpriseId='" + enterpriseId + '\'' +
                ", enterpriseName='" + enterpriseName + '\'' +
                ", hqPos=" + hqPos +
                ", employeeCount=" + employeeCount +
                '}';
    }
}
