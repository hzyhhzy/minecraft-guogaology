package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Naturally occurring sweets on Outer Laver tables. */
public final class GummyJarBlock extends TransparentBlock {
    public static final MapCodec<GummyJarBlock> CODEC = simpleCodec(GummyJarBlock::new);
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);
    public GummyJarBlock(Properties settings) { super(settings); }
    @Override public MapCodec<GummyJarBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) { return 1.0f; }
}
