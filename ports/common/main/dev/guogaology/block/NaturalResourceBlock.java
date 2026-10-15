package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Small, permanently collectible material components, without a ticker or entity. */
public final class NaturalResourceBlock extends Block {
    public static final MapCodec<NaturalResourceBlock> CODEC = simpleCodec(NaturalResourceBlock::new);
    public NaturalResourceBlock(Properties settings) { super(settings); }
    @Override public MapCodec<NaturalResourceBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch(BuiltInRegistries.BLOCK.getKey(this).getPath()) {
            case "matrix_sheet" -> box(1,0,1,15,10,15);
            case "arrow_knot" -> box(2,0,2,14,14,14);
            case "hydra_sprig" -> box(1,0,2,15,15,14);
            case "laver_gel" -> box(2,0,2,14,9,14);
            case "cloud_pearl" -> box(2,0,2,14,12,14);
            case "set_membrane" -> box(1,0,1,15,10,15);
            case "dread_resin" -> box(3,0,3,13,14,13);
            default -> box(0,0,0,16,14,16);
        };
    }
}
