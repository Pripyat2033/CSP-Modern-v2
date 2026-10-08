package com.ben.csp.agent;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.world.item.Item;

public class DirectorsPlanshetItem extends Item {
    // Directors' plansheet item
    
    public static final FabricItemSettings PROPERTIES = new FabricItemSettings();
    
    public DirectorsPlanshetItem() {
        super(PROPERTIES);
    }
}
