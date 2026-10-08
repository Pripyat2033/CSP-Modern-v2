package com.ben.csp.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * ICMS Panel Block Entity - Control panel state and functionality.
 */
public class IcmsPanelBlockEntity extends BlockEntity {

    public enum PanelState { ACTIVE, STANDBY, FAULTED }
    public enum RouteMode { NORMAL, HOLD, REJECT }

    private PanelState state = PanelState.STANDBY;
    private RouteMode routeMode = RouteMode.NORMAL;

    public IcmsPanelBlockEntity(BlockPos pos, BlockState state) {
        super(null, pos, state);
    }

    public PanelState getState() { return state; }
    public void setState(PanelState newState) { this.state = newState; }
    public RouteMode getRouteMode() { return routeMode; }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("state", state.name());
        nbt.putString("routeMode", routeMode.name());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("state")) this.state = PanelState.valueOf(nbt.getString("state"));
        if (nbt.contains("routeMode")) this.routeMode = RouteMode.valueOf(nbt.getString("routeMode"));
    }

    public void tick(World world, BlockState state) {
        // Handle panel tick logic
    }
}
