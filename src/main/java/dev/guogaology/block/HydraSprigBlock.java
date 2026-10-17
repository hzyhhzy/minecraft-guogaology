package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.*;
import net.minecraft.world.BlockView;

/** A sprig with a real upright or ceiling-hung model and matching outline. */
public final class HydraSprigBlock extends Block {
    public static final MapCodec<HydraSprigBlock> CODEC=createCodec(HydraSprigBlock::new);
    public static final BooleanProperty HANGING=Properties.HANGING;
    private static final VoxelShape UP=VoxelShapes.union(createCuboidShape(6,0,6,10,9,10),createCuboidShape(2,7,5,7,12,10),createCuboidShape(10,6,6,14,10,11));
    private static final VoxelShape DOWN=VoxelShapes.union(createCuboidShape(6,7,6,10,16,10),createCuboidShape(2,4,6,7,9,11),createCuboidShape(10,6,5,14,10,10));
    public HydraSprigBlock(Settings settings){super(settings);setDefaultState(getStateManager().getDefaultState().with(HANGING,false));}
    @Override public MapCodec<HydraSprigBlock> getCodec(){return CODEC;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(HANGING);}
    @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return state.get(HANGING)?DOWN:UP;}
    @Override public BlockState getPlacementState(ItemPlacementContext context){
        var p=context.getBlockPos();var w=context.getWorld();
        boolean below=!w.getBlockState(p.down()).isAir(),above=!w.getBlockState(p.up()).isAir();
        return getDefaultState().with(HANGING,!below&&(above||context.getSide()==Direction.DOWN));
    }
}
