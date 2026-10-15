package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** Small, permanently collectible material components, without a ticker or entity. */
public final class NaturalResourceBlock extends Block {
    public static final MapCodec<NaturalResourceBlock> CODEC = createCodec(NaturalResourceBlock::new);
    public NaturalResourceBlock(Settings settings) { super(settings); }
    @Override public MapCodec<NaturalResourceBlock> getCodec() { return CODEC; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(Registries.BLOCK.getId(this).getPath()) {
            case "matrix_sheet" -> createCuboidShape(1,0,1,15,10,15);
            case "arrow_knot" -> createCuboidShape(2,0,2,14,14,14);
            case "hydra_sprig" -> createCuboidShape(1,0,2,15,15,14);
            case "laver_gel" -> createCuboidShape(2,0,2,14,9,14);
            case "cloud_pearl" -> createCuboidShape(2,0,2,14,12,14);
            case "set_membrane" -> createCuboidShape(1,0,1,15,10,15);
            case "dread_resin" -> createCuboidShape(3,0,3,13,14,13);
            default -> createCuboidShape(0,0,0,16,14,16);
        };
    }
}
