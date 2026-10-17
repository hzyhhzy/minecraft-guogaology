package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** Small, permanently collectible material components, without a ticker or entity. */
public final class NaturalResourceBlock extends Block {
    public static final MapCodec<NaturalResourceBlock> CODEC = simpleCodec(NaturalResourceBlock::new);
    private static final VoxelShape SHEETS=Shapes.or(box(1,0,1,15,2,15),box(2,3,2,14,5,14));
    private static final VoxelShape MEMBRANE=Shapes.or(box(2,4,7,14,12,9),box(4,2,7,12,14,9),box(7,2,4,9,14,12),box(7,4,2,9,12,14),box(2,7,4,14,9,12),box(4,7,2,12,9,14));
    private static final VoxelShape PEARL=Shapes.or(Block.box(2,2,3,14,14,13),Block.box(3,1,4,13,15,12),Block.box(3,3,2,13,13,14));
    public NaturalResourceBlock(Properties settings) { super(settings); }
    @Override public MapCodec<NaturalResourceBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch(BuiltInRegistries.BLOCK.getKey(this).getPath()) {
            case "matrix_sheet" -> SHEETS;
            case "set_membrane" -> MEMBRANE;
            case "cloud_pearl" -> PEARL;
            default -> Shapes.block();
        };
    }
}
