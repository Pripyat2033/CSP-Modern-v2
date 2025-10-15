package com.ben.csp.block;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * The BlockEntity for the Archive Filing Cabinet. It stores "documents" as NBT data.
 */
public class ArchiveFilingCabinetBlockEntity extends BlockEntity {

    private final List<NbtCompound> filedDocuments = Lists.newArrayList();

    public ArchiveFilingCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ARCHIVE_FILING_CABINET_BLOCK_ENTITY, pos, state);
    }

    public void fileDocument(NbtCompound document) {
        this.filedDocuments.add(document);
        this.markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList nbtList = new NbtList();
        nbtList.addAll(this.filedDocuments);
        nbt.put("FiledDocuments", nbtList);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.filedDocuments.clear();
        if (nbt.contains("FiledDocuments", NbtElement.LIST_TYPE)) {
            NbtList nbtList = nbt.getList("FiledDocuments", NbtElement.COMPOUND_TYPE);
            nbtList.forEach(element -> this.filedDocuments.add((NbtCompound) element));
        }
    }
}