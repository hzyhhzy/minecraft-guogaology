package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TransparentBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** A frosted fruit confection. Its embossed sides are geometry, not a cake skin. */
public final class FruitJellyBlock extends TransparentBlock {
    public static final MapCodec<FruitJellyBlock> CODEC = createCodec(FruitJellyBlock::new);
    private static final VoxelShape SHAPE = createCuboidShape(0.5, 0, 0.5, 15.5, 15, 15.5);

    public FruitJellyBlock(Settings settings) { super(settings); }
    @Override public MapCodec<FruitJellyBlock> getCodec() { return CODEC; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) { return SHAPE; }
    @Override protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) { return 1.0f; }
}
