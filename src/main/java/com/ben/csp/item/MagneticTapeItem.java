package com.ben.csp.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MagneticTapeItem extends Item {
    private static final String PROGRAM_KEY = "ProgramName";

    public MagneticTapeItem(FabricItemSettings settings) {
        super(settings);
    }

    public MagneticTapeItem() {
        this(new FabricItemSettings());
    }

    public static void setProgram(ItemStack stack, String programName) {
        if (stack.getItem() instanceof MagneticTapeItem) {
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putString(PROGRAM_KEY, programName);
        }
    }

    @Nullable
    public static String getProgram(ItemStack stack) {
        if (stack.hasNbt() && stack.getNbt().contains(PROGRAM_KEY)) {
            return stack.getNbt().getString(PROGRAM_KEY);
        }
        return null;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        String program = getProgram(stack);
        tooltip.add(Text.literal("Program: " + (program != null ? program : "BLANK")).formatted(Formatting.GRAY));
    }
}
