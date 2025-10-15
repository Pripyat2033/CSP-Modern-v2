package com.ben.csp.logistics;

import com.ben.csp.CSPMod;
import com.ben.csp.build.BuildTask;
import com.ben.csp.entity.BargeEntity;
import com.ben.csp.world.LocationType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Science Grade: A central singleton managing the project's supply chain.
 * It handles resource requisitions, creates dispatch orders, and assigns them to available transport entities.
 */
public class LogisticsManager {
    private static final LogisticsManager INSTANCE = new LogisticsManager();
    private final Queue<DispatchOrder> completedOrders = new ConcurrentLinkedQueue<>();

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
        Map<String, Integer> requiredMaterials = task.getRequiredMaterials();
        if (requiredMaterials == null || requiredMaterials.isEmpty()) {
            CSPMod.LOGGER.info("No materials required for task '{}'. Skipping logistics.", task.getDescription());
            return;
        }

        DispatchOrder order = new DispatchOrder(task.getDestination(), requiredMaterials);

        // Decouple from DirectorateManager by getting the location here.
        BlockPos portPos = com.ben.csp.build.DirectorateManager.getLocation(LocationType.PORT_AUTHORITY);
        if (portPos == null) {
            CSPMod.LOGGER.warn("Could not fulfill resource request for task '{}': Port Authority location is not defined.", task.getDescription());
            return;
        }

        // Find an available barge to carry out the order.
        Optional<BargeEntity> availableBarge = findAvailableBarge(world, portPos);

        if (availableBarge.isPresent()) {
            BargeEntity barge = availableBarge.get();
            barge.setDestination(order.destination());

            // Convert the material map into a list of ItemStacks for the cargo.
            List<ItemStack> cargo = order.materials().entrySet().stream()
                    .map(entry -> {
                        Identifier itemId = new Identifier(entry.getKey());
                        if (!Registries.ITEM.containsId(itemId)) {
                            CSPMod.LOGGER.error("Logistics Error: Invalid item ID '{}' in resource request for task '{}'.", itemId, task.getDescription());
                            return null; // Or return ItemStack.EMPTY
                        }
                        return new ItemStack(Registries.ITEM.get(itemId), entry.getValue());
                    })
                    .filter(Objects::nonNull).toList();
            barge.loadCargo(cargo);
            CSPMod.LOGGER.info("LogisticsManager dispatched Barge {} to {} for task '{}'", barge.getUuid(), order.destination(), task.getDescription());
        } else {
            CSPMod.LOGGER.warn("Could not fulfill resource request for task '{}': No available barges.", task.getDescription());
            // In a real system, this would queue the request.
        }
    }

    private Optional<BargeEntity> findAvailableBarge(ServerWorld world, BlockPos portLocation) {
        // Find barges that are idle (no destination) near the port.
        return world.getEntitiesByClass(BargeEntity.class, new Box(portLocation).expand(32), barge -> barge.getDestination() == null)
                .stream()
                .findFirst(); 
    }

    /**
     * Called by entities when they complete a delivery.
     * @param order The completed dispatch order.
     */
    public void reportOrderComplete(DispatchOrder order) {
        this.completedOrders.add(order);
    }

    public List<DispatchOrder> drainCompletedOrders() {
        // Atomically drain the queue to prevent race conditions.
        // This is safer than iterating and then clearing.
        List<DispatchOrder> drained = new ArrayList<>();
        DispatchOrder order;
        while ((order = completedOrders.poll()) != null) {
            drained.add(order);
        }
        return drained;
    }

}