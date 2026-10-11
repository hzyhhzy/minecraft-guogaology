package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TransparentBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** Naturally occurring sweets on Outer Laver tables. */
public final class GummyJarBlock extends TransparentBlock {
    public static final MapCodec<GummyJarBlock> CODEC = createCodec(GummyJarBlock::new);
    private static final VoxelShape SHAPE = createCuboidShape(1, 0, 1, 15, 16, 15);
    public GummyJarBlock(Settings settings) { super(settings); }
    @Override public MapCodec<GummyJarBlock> getCodec() { return CODEC; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) { return SHAPE; }
    @Override protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) { return 1.0f; }
}
