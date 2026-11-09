package com.ben.csp.data;

import com.ben.csp.infra.InfrastructureRequirements;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads data/csp/enterprise_infrastructure.json into {@link InfrastructureRequirements}.
 * The shipped file has no tier field, so every blueprint list is registered under tier 0.
 */
public class InfrastructureRequirementLoader implements SimpleSynchronousResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("csp-modern");
    private static final Identifier ID = new Identifier("csp-modern", "infrastructure_requirements");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        Map<String, Map<Integer, List<String>>> requirements = new HashMap<>();
        Map<Identifier, Resource> found = manager.findResources(
            "csp", id -> id.getPath().equals("enterprise_infrastructure.json"));

        for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
            try (Reader reader = entry.getValue().getReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray list = root.getAsJsonArray("requirements");
                if (list == null) {
                    continue;
                }
                for (JsonElement element : list) {
                    JsonObject requirement = element.getAsJsonObject();
                    String type = requirement.get("type").getAsString();
                    List<String> blueprints = new ArrayList<>();
                    for (JsonElement blueprint : requirement.getAsJsonArray("blueprints")) {
                        blueprints.add(blueprint.getAsString());
                    }
                    requirements.computeIfAbsent(type, key -> new HashMap<>()).put(0, blueprints);
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to load infrastructure requirements from {}", entry.getKey(), exception);
            }
        }

        InfrastructureRequirements.getInstance().loadRequirements(requirements);
        LOGGER.info("Loaded infrastructure requirements for {} categories", requirements.size());
    }
}
