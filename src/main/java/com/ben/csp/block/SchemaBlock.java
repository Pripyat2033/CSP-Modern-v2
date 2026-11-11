package com.ben.csp.block;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;

/**
 * A structural section of the reactor — a <b>схема (Schema)</b>.
 *
 * <p>In Soviet usage the reactor is not one casting: it is a set of factory-built
 * sections, each with its own designation, trucked to site and craned together.
 * The upper biological shield is <b>Схема Е</b>; the lower shield, the vessel
 * (кожух) and the side water tanks are the other sections. These are real parts
 * of the reactor, not decoration, so the world builds them as real blocks.
 */
public class SchemaBlock extends Block {

    /** The reactor sections, each a separate Schema. */
    public enum Section {
        /** Схема Е — upper biological shield (UBS), Ø17 x 3 m, ~2000 t. */
        UPPER_SHIELD("ubs", "Схема Е", "Upper biological shield"),
        /** Lower biological shield (LBS), Ø14.5 x 2 m. */
        LOWER_SHIELD("lbs", "Схема", "Lower biological shield"),
        /** The reactor vessel / кожух, inner Ø14.5 x 9.7 m. */
        VESSEL("vessel", "Кожух", "Reactor vessel"),
        /** Side biological-shield water tank, Ø16.6 -> 19 m, 16 compartments. */
        WATER_TANK("water_tank", "Бак биозащиты", "Side biological-shield water tank");

        private final String id;
        private final String soviet;
        private final String english;

        Section(String id, String soviet, String english) {
            this.id = id;
            this.soviet = soviet;
            this.english = english;
        }

        public String id() { return id; }
        public String soviet() { return soviet; }
        public String english() { return english; }
    }

    private final Section section;

    public SchemaBlock(Section section, Block base) {
        super(FabricBlockSettings.copyOf(base));
        this.section = section;
    }

    public Section section() {
        return section;
    }

    /** A section block with a sensible base material for its real make-up. */
    public static SchemaBlock of(Section section) {
        switch (section) {
            case UPPER_SHIELD: // serpentinite concrete with steel-graphite plugs
                return new SchemaBlock(section, Blocks.GRAY_CONCRETE);
            case LOWER_SHIELD: // steel and serpentinite
                return new SchemaBlock(section, Blocks.DEEPSLATE_TILES);
            case VESSEL:       // steel shell
                return new SchemaBlock(section, Blocks.IRON_BLOCK);
            case WATER_TANK:   // water, in 16 compartments
                return new SchemaBlock(section, Blocks.BLUE_ICE);
            default:
                return new SchemaBlock(section, Blocks.IRON_BLOCK);
        }
    }
}
