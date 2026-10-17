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
    private static final VoxelShape SPRIG=VoxelShapes.union(createCuboidShape(6,0,6,10,9,10),createCuboidShape(2,7,5,7,12,10),createCuboidShape(10,6,6,14,10,11));
    private static final VoxelShape MEMBRANE=VoxelShapes.union(createCuboidShape(1,0,1,15,2,15),createCuboidShape(1,2,13,15,9,15));
    private static final VoxelShape PEARL=VoxelShapes.union(createCuboidShape(6,1,6,10,10,10),createCuboidShape(4,0,5,12,2,11),createCuboidShape(5,0,3,11,2,13),createCuboidShape(4,8,4,12,14,12),createCuboidShape(5,13,5,11,15,11),createCuboidShape(1,8,5,6,12,11),createCuboidShape(10,8,5,15,12,11),createCuboidShape(5,8,1,11,12,6),createCuboidShape(5,8,10,11,12,15),createCuboidShape(3,9,3,6,13,6),createCuboidShape(10,9,3,13,13,6),createCuboidShape(3,9,10,6,13,13),createCuboidShape(10,9,10,13,13,13));
    public NaturalResourceBlock(Settings settings) { super(settings); }
    @Override public MapCodec<NaturalResourceBlock> getCodec() { return CODEC; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(Registries.BLOCK.getId(this).getPath()) {
            case "matrix_sheet" -> SHEETS;
            case "hydra_sprig" -> SPRIG;
            case "set_membrane" -> MEMBRANE;
            case "cloud_pearl" -> PEARL;
            default -> VoxelShapes.fullCube();
        };
    }
}
