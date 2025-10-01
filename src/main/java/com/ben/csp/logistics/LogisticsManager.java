package com.ben.csp.logistics;

import com.ben.csp.CSPMod;
import com.ben.csp.build.BuildTask;
import com.ben.csp.entity.BargeEntity;
import com.ben.csp.entity.ModEntities;
import com.ben.csp.world.LocationType;
import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Science Grade: A central singleton managing the project's supply chain.
 * It handles resource requisitions, creates dispatch orders, and assigns them to available transport entities.
 */
public class LogisticsManager {
    private static final LogisticsManager INSTANCE = new LogisticsManager();
    private final List<DispatchOrder> completedOrders = Lists.newArrayList();

    private LogisticsManager() {}

    public static LogisticsManager getInstance() {
        return INSTANCE;
    }

    /**
     * Fulfills a resource request for a given build task.
     * This is a placeholder for a more complex system that would check inventory.
     * For now, it finds a port and dispatches a barge with placeholder materials.
     * @param task The build task requiring resources.
     * @param world The world to operate in.
     */
    public void fulfillResourceRequest(BuildTask task, ServerWorld world) {
        // In a real implementation, this would get materials from the task definition.
        Map<String, Integer> materials = Map.of("minecraft:cobblestone", 64, "minecraft:iron_ingot", 32);
        DispatchOrder order = new DispatchOrder(task.getDestination(), materials);

        // Find an available barge to carry out the order.
        Optional<BargeEntity> availableBarge = findAvailableBarge(world);

        if (availableBarge.isPresent()) {
            BargeEntity barge = availableBarge.get();
            barge.setDestination(order.destination());
            // For now, just represent the cargo with a single item stack.
            barge.loadCargo(new ItemStack(Items.IRON_BLOCK, 1));
            CSPMod.LOGGER.info("LogisticsManager dispatched Barge {} to {} for task '{}'", barge.getUuid(), order.destination(), task.getDescription());
        } else {
            CSPMod.LOGGER.warn("Could not fulfill resource request for task '{}': No available barges.", task.getDescription());
            // In a real system, this would queue the request.
        }
    }

    private Optional<BargeEntity> findAvailableBarge(ServerWorld world) {
        BlockPos portPos = com.ben.csp.build.DirectorateManager.getLocation(LocationType.PORT_AUTHORITY);
        if (portPos == null) {
            return Optional.empty();
        }

        // Find barges that are idle (no destination) near the port.
        return world.getEntitiesByType(ModEntities.BARGE, barge -> barge.getDestination() == null && barge.getBlockPos().isWithinDistance(portPos, 32))
                .stream()
                .findFirst();
    }

    /**
     * Called by entities when they complete a delivery.
     * @param order The completed dispatch order.
     */
    public void reportOrderComplete(DispatchOrder order) {
        completedOrders.add(order);
    }

    public List<DispatchOrder> drainCompletedOrders() {
        if (completedOrders.isEmpty()) return Collections.emptyList();
        List<DispatchOrder> drained = List.copyOf(completedOrders);
        completedOrders.clear();
        return drained;
    }
}