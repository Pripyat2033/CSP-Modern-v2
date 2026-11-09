package com.ben.csp.block.entity;

import com.ben.csp.block.GenplanBlock;
import com.ben.csp.block.ModBlocks;
import com.ben.csp.plan.PlanDocument;
import com.ben.csp.plan.PlanExtent;
import com.ben.csp.plan.PlanLayer;
import com.ben.csp.plan.PlanPanelGeometry;
import com.ben.csp.plan.PlanPrimitive;
import com.ben.csp.plan.PlanSheet;
import com.ben.csp.plan.PlanSheetRasterizer;
import com.ben.csp.plan.PlanStyle;
import com.ben.csp.plan.Scale;
import com.ben.csp.plan.TitleBlock;
import com.ben.csp.prombaza.Area;
import com.ben.csp.prombaza.Prombaza;
import com.ben.csp.prombaza.PrombazaState;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The wall генплан: a display that projects a plan. The block knows nothing about
 * prombazas; it takes a plan, rasterises it and hands the picture to the renderer,
 * which paints it on the block faces.
 *
 * A panel is the connected group of генплан blocks facing the same way. The whole
 * drawing is held once and sliced across the panel: each block shows the cell that
 * falls on it, so a wall built from many blocks shows one continuous plan. The
 * drawing is derived from where the panel stands: built inside a prombaza it shows
 * that prombaza's territory.
 */
public class GenplanBlockEntity extends BlockEntity {

    public static final String ID = "genplan_block_entity";

    private static final int BASE_WIDTH_PX = 320;
    private static final int MAX_HEIGHT_PX = 512;

    public static final BlockEntityType<GenplanBlockEntity> GENPLAN_BLOCK_ENTITY_TYPE =
        FabricBlockEntityTypeBuilder.create(GenplanBlockEntity::new, ModBlocks.GENPLAN_BLOCK).build();

    private int rasterW;
    private int rasterH;
    private int[] raster = new int[0];

    private int cols = 1;
    private int rows = 1;
    private int col = 0;
    private int row = 0;

    public GenplanBlockEntity(BlockPos pos, BlockState state) {
        super(GENPLAN_BLOCK_ENTITY_TYPE, pos, state);
    }

    public int rasterW() { return rasterW; }
    public int rasterH() { return rasterH; }
    public int[] raster() { return raster; }
    public int cols() { return cols; }
    public int rows() { return rows; }
    public int col() { return col; }
    public int row() { return row; }

    public boolean hasRaster() {
        return rasterW > 0 && rasterH > 0 && raster.length == rasterW * rasterH;
    }

    /**
     * Server side: re-derive the drawing for the whole panel this block belongs
     * to, and slice it across the panel's blocks. Called when a block is placed,
     * broken, or the plan changes.
     */
    public void refresh() {
        if (!(getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld)) {
            return;
        }
        Direction facing = getCachedState().get(Properties.HORIZONTAL_FACING);
        List<BlockPos> group = connectedGroup(facing);
        List<Long> packed = new ArrayList<>();
        for (BlockPos p : group) {
            packed.add(PlanPanelGeometry.pack(p.getX(), p.getY(), p.getZ()));
        }
        PlanPanelGeometry.Panel panel = PlanPanelGeometry.layout(packed, facingIndex(facing));

        // The drawing is derived once, from the prombaza the panel stands in.
        BlockPos origin = representative(panel, group);
        Optional<Prombaza> here = PrombazaState.get(serverWorld.getServer()).registry()
            .at(origin.getX(), origin.getY(), origin.getZ());

        int[] pixels = null;
        int pw = 0, ph = 0;
        if (here.isPresent()) {
            PlanSheet sheet = buildSheet(here.get());
            PlanExtent e = sheet.extent();
            pw = BASE_WIDTH_PX;
            ph = (int) Math.max(16, Math.min(MAX_HEIGHT_PX,
                Math.round(BASE_WIDTH_PX * e.height() / Math.max(1.0, e.width()))));
            PlanSheetRasterizer rast = new PlanSheetRasterizer(pw, ph, 3.0);
            rast.rasterize(sheet);
            pixels = rast.renderer().pixels();
        }

        for (PlanPanelGeometry.Cell cell : panel.cells()) {
            BlockPos pos = new BlockPos(PlanPanelGeometry.unpackXFrom(cell.pos()),
                PlanPanelGeometry.unpackYFrom(cell.pos()), PlanPanelGeometry.unpackZFrom(cell.pos()));
            if (serverWorld.getBlockEntity(pos) instanceof GenplanBlockEntity be) {
                be.applySlice(pixels, pw, ph, panel.cols(), panel.rows(), cell.col(), cell.row());
            }
        }
    }

    /** The connected group of генплан blocks facing the same way as this one. */
    private List<BlockPos> connectedGroup(Direction facing) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(getPos());
        seen.add(getPos());
        World world = getWorld();
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            out.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.offset(d);
                if (seen.contains(n)) {
                    continue;
                }
                if (world.getBlockState(n).getBlock() instanceof GenplanBlock
                    && world.getBlockState(n).get(Properties.HORIZONTAL_FACING) == facing) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        return out;
    }

    private BlockPos representative(PlanPanelGeometry.Panel panel, List<BlockPos> group) {
        int midCol = panel.cols() / 2;
        int midRow = panel.rows() / 2;
        for (PlanPanelGeometry.Cell c : panel.cells()) {
            if (c.col() == midCol && c.row() == midRow) {
                return new BlockPos(PlanPanelGeometry.unpackXFrom(c.pos()),
                    PlanPanelGeometry.unpackYFrom(c.pos()), PlanPanelGeometry.unpackZFrom(c.pos()));
            }
        }
        return getPos();
    }

    private static int facingIndex(Direction facing) {
        return switch (facing) {
            case EAST -> 0;
            case SOUTH -> 1;
            case WEST -> 2;
            default -> 3; // NORTH
        };
    }

    private void applySlice(int[] pixels, int pw, int ph, int cols, int rows, int col, int row) {
        this.raster = pixels == null ? new int[0] : pixels;
        this.rasterW = pw;
        this.rasterH = ph;
        this.cols = cols;
        this.rows = rows;
        this.col = col;
        this.row = row;
        markDirty();
        sync();
    }

    private PlanSheet buildSheet(Prombaza prombaza) {
        Area area = prombaza.area();
        PlanExtent extent = new PlanExtent(area.minX(), area.minZ(), area.maxX(), area.maxZ());

        PlanDocument doc = new PlanDocument(prombaza.name());
        PlanLayer boundary = doc.layer("prombaza-boundary");
        boundary.add(new PlanPrimitive.Polyline(
            new double[] {area.minX(), area.maxX(), area.maxX(), area.minX()},
            new double[] {area.minZ(), area.minZ(), area.maxZ(), area.maxZ()},
            true, PlanStyle.of(0xFF1A1A1A, 2.0)));

        TitleBlock titleBlock = new TitleBlock(prombaza.name(), "План промбазы", Scale.S1_2000,
            "1", 1, prombaza.createdBy(), prombaza.createdBy(), prombaza.createdDay());
        return new PlanSheet("1", "План промбазы", Scale.S1_2000, extent, doc, titleBlock);
    }

    private void sync() {
        if (getWorld() != null && !getWorld().isClient()) {
            getWorld().updateListeners(getPos(), getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("rasterW", rasterW);
        nbt.putInt("rasterH", rasterH);
        nbt.putInt("cols", cols);
        nbt.putInt("rows", rows);
        nbt.putInt("col", col);
        nbt.putInt("row", row);
        nbt.put("raster", new NbtIntArray(raster));
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        rasterW = nbt.getInt("rasterW");
        rasterH = nbt.getInt("rasterH");
        cols = Math.max(1, nbt.getInt("cols"));
        rows = Math.max(1, nbt.getInt("rows"));
        col = nbt.getInt("col");
        row = nbt.getInt("row");
        if (nbt.contains("raster", NbtElement.INT_ARRAY_TYPE)) {
            raster = nbt.getIntArray("raster");
        } else {
            raster = new int[0];
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
