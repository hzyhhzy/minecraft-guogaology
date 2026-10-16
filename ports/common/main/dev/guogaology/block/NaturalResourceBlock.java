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
    private static final VoxelShape SPRIG=Shapes.or(box(6,0,6,10,9,10),box(2,7,5,7,12,10),box(10,6,6,14,10,11));
    private static final VoxelShape MEMBRANE=Shapes.or(box(1,0,1,15,2,15),box(1,2,13,15,9,15));
    public NaturalResourceBlock(Properties settings) { super(settings); }
    @Override public MapCodec<NaturalResourceBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch(BuiltInRegistries.BLOCK.getKey(this).getPath()) {
            case "matrix_sheet" -> SHEETS;
            case "hydra_sprig" -> SPRIG;
            case "set_membrane" -> MEMBRANE;
            default -> Shapes.block();
        };
    }
}
