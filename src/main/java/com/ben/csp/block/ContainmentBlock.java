package com.ben.csp.block;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

/**
 * The wall of an energoblok compartment — the reinforced concrete of the sealed
 * boxes that <b>are</b> the RBMK's containment.
 *
 * <p>The common claim that an RBMK-1000 has no containment is false. A unit is
 * built as a set of separate, pressure-resistant concrete boxes, each rated for a
 * design-basis accident that is not a core melt but the rupture of a large-bore
 * water pipe feeding the core. These walls are that containment, so they are a
 * real material in the world, not decoration.
 */
public class ContainmentBlock extends Block {

    /** What a piece of the box wall is, so the world reads as architecture. */
    public enum Kind {
        /** The sealed concrete wall of a compartment (0.5 m at 1:4). */
        WALL("containment_wall", "Reinforced concrete wall of a sealed compartment"),
        /** A pressure-relief / steam-relief panel: the floor relief assemblies. */
        RELIEF("containment_relief", "Pressure-relief panel — vents to the steam corridor"),
        /** A blast door / personnel airlock in a compartment wall. */
        DOOR("containment_door", "Blast door — access through the compartment wall");

        private final String id;
        private final String description;

        Kind(String id, String description) {
            this.id = id;
            this.description = description;
        }

        public String id() { return id; }
        public String description() { return description; }
    }

    private final Kind kind;

    public ContainmentBlock(Kind kind, Block base) {
        super(FabricBlockSettings.copyOf(base));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /** A containment block with a base material matching its real make-up. */
    public static ContainmentBlock of(Kind kind) {
        switch (kind) {
            case WALL:   // reinforced, heavily steel-reinforced concrete
                return new ContainmentBlock(kind, Blocks.GRAY_CONCRETE);
            case RELIEF: // steel relief panel set into the floor
                return new ContainmentBlock(kind, Blocks.IRON_BLOCK);
            case DOOR:   // steel blast door
                return new ContainmentBlock(kind, Blocks.IRON_BLOCK);
            default:
                return new ContainmentBlock(kind, Blocks.GRAY_CONCRETE);
        }
    }
}
