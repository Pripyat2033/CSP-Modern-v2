package com.ben.csp.data;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class InfrastructureRequirementLoader implements SimpleSynchronousResourceReloadListener {
    @Override
    public Identifier getFabricId() {
        return new Identifier("csp-modern", "infrastructure_requirements");
    }

    @Override
    public void reload(ResourceManager manager) {
        // Placeholder for loading requirements
    }
}