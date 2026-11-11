package com.ben.csp.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Logistics Management Module
 *
 * Historical Function: Manages transportation, warehouse operations, and
 * material distribution across the construction enterprise. Coordinates with
 * supply chain enterprises and Prombaza facilities.
 *
 * <p>A delivery plan is worked out from what is actually registered: the routes
 * that exist and the stock that has been received. A destination with no route,
 * or no stock, is simply not planned. Nothing is invented when there is nothing
 * to move.
 */
public class LogisticsManager {

    /**
     * Transport route registry for materials delivery
     * Stores routes between sources (mines, factories) and destinations
     * (construction sites).
     */
    public static class TransportRoute {
        public final String source;
        public final String destination;
        public final int capacityPerTrip;
        public final double travelTimeMinutes;
        public final boolean refrigerated; // For temperature-sensitive materials

        public TransportRoute(String source, String destination, int capacity,
                              double travelTime, boolean refrigerated) {
            this.source = source;
            this.destination = destination;
            this.capacityPerTrip = capacity;
            this.travelTimeMinutes = travelTime;
            this.refrigerated = refrigerated;
        }
    }

    /**
     * Warehouse inventory tracking for individual materials.
     */
    public static class MaterialInventory {
        public final String materialId;
        public final int quantity;
        public final double storageVolume; // in m³
        public final boolean perishable;

        public MaterialInventory(String materialId, int quantity,
                                 double volume, boolean perishable) {
            this.materialId = materialId;
            this.quantity = quantity;
            this.storageVolume = volume;
            this.perishable = perishable;
        }
    }

    private final List<TransportRoute> routes = new ArrayList<>();
    private final Map<String, MaterialInventory> inventory = new LinkedHashMap<>();
    private final Map<String, String> destinationOf = new HashMap<>();

    /**
     * Initialize logistics manager with default state.
     */
    public LogisticsManager() {
        // Starts empty: routes and stock are registered as they arise.
    }

    /**
     * Register a transport route for material delivery.
     *
     * @throws IllegalArgumentException if the capacity is not positive or the
     *         travel time is negative
     */
    public void registerRoute(String source, String destination, int capacity,
                              double travelTimeMinutes, boolean refrigerated) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("route capacity must be positive");
        }
        if (travelTimeMinutes < 0) {
            throw new IllegalArgumentException("travel time must not be negative");
        }
        routes.add(new TransportRoute(source, destination, capacity,
            travelTimeMinutes, refrigerated));
    }

    /**
     * Track inventory received at a warehouse or site. Stock already held for the
     * same material is added to, not overwritten.
     *
     * @throws IllegalArgumentException if the quantity or volume is negative
     */
    public void trackInventoryReceived(String materialId, int quantity,
                                       double volume, String destination) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        if (volume < 0) {
            throw new IllegalArgumentException("volume must not be negative");
        }
        MaterialInventory existing = inventory.get(materialId);
        boolean perishable = existing != null && existing.perishable;
        int newQty = quantity + (existing == null ? 0 : existing.quantity);
        double newVol = volume + (existing == null ? 0 : existing.storageVolume);
        inventory.put(materialId, new MaterialInventory(materialId, newQty, newVol, perishable));
        destinationOf.put(materialId, destination);
    }

    /**
     * Calculate the delivery schedule: how many minutes the stock at each
     * destination needs, given the fastest route to it.
     *
     * <p>Volume at a destination is carried in whole trips of the fastest route's
     * capacity, so a heavier stock never shortens the time. A destination with no
     * route is left out of the plan rather than guessed at.
     *
     * @return destination name to delivery time in minutes; empty if nothing is
     *         registered
     */
    public Map<String, Double> calculateDeliverySchedule() {
        Map<String, Double> volumeByDestination = new LinkedHashMap<>();
        for (MaterialInventory inv : inventory.values()) {
            String dest = destinationOf.get(inv.materialId);
            if (dest == null) {
                continue;
            }
            volumeByDestination.merge(dest, inv.storageVolume, Double::sum);
        }

        Map<String, Double> plan = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : volumeByDestination.entrySet()) {
            TransportRoute fastest = fastestRouteTo(e.getKey());
            if (fastest == null) {
                continue;
            }
            double volume = e.getValue();
            long trips = (long) Math.ceil(volume / fastest.capacityPerTrip);
            if (trips <= 0) {
                trips = 1;
            }
            plan.put(e.getKey(), trips * fastest.travelTimeMinutes);
        }
        return plan;
    }

    /** The route with the least travel time to a destination, or null. */
    private TransportRoute fastestRouteTo(String destination) {
        TransportRoute best = null;
        for (TransportRoute r : routes) {
            if (r.destination.equals(destination)
                && (best == null || r.travelTimeMinutes < best.travelTimeMinutes)) {
                best = r;
            }
        }
        return best;
    }

    /** The stock currently held, by material. The map is unmodifiable. */
    public Map<String, MaterialInventory> inventory() {
        return Collections.unmodifiableMap(inventory);
    }

    /** The routes registered so far. The list is unmodifiable. */
    public List<TransportRoute> routes() {
        return Collections.unmodifiableList(routes);
    }
}
