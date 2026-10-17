package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/** Small, permanently collectible material components, without a ticker or entity. */
public final class NaturalResourceBlock extends Block {
    public static final MapCodec<NaturalResourceBlock> CODEC = createCodec(NaturalResourceBlock::new);
    private static final VoxelShape SHEETS=VoxelShapes.union(createCuboidShape(1,0,1,15,2,15),createCuboidShape(2,3,2,14,5,14));
    private static final VoxelShape MEMBRANE=VoxelShapes.union(createCuboidShape(2,4,7,14,12,9),createCuboidShape(4,2,7,12,14,9),createCuboidShape(7,2,4,9,14,12),createCuboidShape(7,4,2,9,12,14),createCuboidShape(2,7,4,14,9,12),createCuboidShape(4,7,2,12,9,14));
    public NaturalResourceBlock(Settings settings) { super(settings); }
    @Override public MapCodec<NaturalResourceBlock> getCodec() { return CODEC; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(Registries.BLOCK.getId(this).getPath()) {
            case "matrix_sheet" -> SHEETS;
            case "set_membrane" -> MEMBRANE;
            default -> VoxelShapes.fullCube();
        };
    }
}
