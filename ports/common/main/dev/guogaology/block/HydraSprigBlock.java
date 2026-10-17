package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** A sprig with a real upright or ceiling-hung model and matching outline. */
public final class HydraSprigBlock extends Block {
    public static final MapCodec<HydraSprigBlock> CODEC=simpleCodec(HydraSprigBlock::new);
    public static final BooleanProperty HANGING=BlockStateProperties.HANGING;
    private static final VoxelShape UP=Shapes.or(box(6,0,6,10,9,10),box(2,7,5,7,12,10),box(10,6,6,14,10,11));
    private static final VoxelShape DOWN=Shapes.or(box(6,7,6,10,16,10),box(2,4,6,7,9,11),box(10,6,5,14,10,10));
    public HydraSprigBlock(Properties settings){super(settings);registerDefaultState(getStateDefinition().any().setValue(HANGING,false));}
    @Override public MapCodec<HydraSprigBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(HANGING);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return state.getValue(HANGING)?DOWN:UP;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var p=context.getClickedPos();var w=context.getLevel();
        boolean below=!w.getBlockState(p.below()).isAir(),above=!w.getBlockState(p.above()).isAir();
        return defaultBlockState().setValue(HANGING,!below&&(above||context.getClickedFace()==Direction.DOWN));
    }
}
