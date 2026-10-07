package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A frosted fruit confection. Its embossed sides are geometry, not a cake skin. */
public final class FruitJellyBlock extends TransparentBlock {
    public static final MapCodec<FruitJellyBlock> CODEC = simpleCodec(FruitJellyBlock::new);
    private static final VoxelShape SHAPE = box(0.5, 0, 0.5, 15.5, 15, 15.5);

    public FruitJellyBlock(Properties settings) { super(settings); }
    @Override public MapCodec<FruitJellyBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) { return 1.0f; }
}
