package com.ben.csp.opb;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class OPB82RuleLoader implements SimpleSynchronousResourceReloadListener {
    @Override
    public Identifier getFabricId() {
        return new Identifier("csp-modern", "opb82_rules");
    }

    @Override
    public void reload(ResourceManager manager) {
        // Placeholder for loading rules
    }
}