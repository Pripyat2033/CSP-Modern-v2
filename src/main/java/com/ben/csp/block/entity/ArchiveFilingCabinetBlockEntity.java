package com.ben.csp.block.entity;

import com.ben.csp.block.ModBlocks;
import com.google.common.collect.Lists;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArchiveFilingCabinetBlockEntity extends BlockEntity {
    private final List<NbtCompound> filedDocuments = Lists.newArrayList();

    public ArchiveFilingCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ARCHIVE_FILING_CABINET_BLOCK_ENTITY, pos, state);
    }

    public void fileDocument(@Nullable NbtCompound document) {
        if (document != null) {
            this.filedDocuments.add(document);
            this.markDirty();
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList list = new NbtList();
        list.addAll(filedDocuments);
        nbt.put("FiledDocuments", list);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.filedDocuments.clear();
        if (nbt.contains("FiledDocuments", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("FiledDocuments", NbtElement.COMPOUND_TYPE);
            list.forEach(element -> this.filedDocuments.add((NbtCompound) element));
        }
    }
}