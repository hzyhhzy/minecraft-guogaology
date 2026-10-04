package dev.googology.block;

import dev.googology.portal.PortalRitual;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public final class GoogologyPortalFrameBlock extends StatefulDecorBlock {
    public static final BooleanProperty FRUIT=BooleanProperty.of("fruit");
    private static final VoxelShape SHAPE=VoxelShapes.union(
            createCuboidShape(0,0,0,16,3,16), createCuboidShape(2,3,2,14,6,14),
            createCuboidShape(1,6,1,15,8,15), createCuboidShape(4,8,4,12,12,12),
            createCuboidShape(1,8,1,4,11,4),createCuboidShape(12,8,1,15,11,4),
            createCuboidShape(1,8,12,4,11,15),createCuboidShape(12,8,12,15,11,15));
    public GoogologyPortalFrameBlock(Settings settings) { super(settings);setDefaultState(getStateManager().getDefaultState().with(FRUIT,false)); }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder) { builder.add(FRUIT); }
    @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context) { return SHAPE; }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context) { return SHAPE; }
    @Override protected void onStateReplaced(BlockState state,World world,BlockPos pos,BlockState next,boolean moved) {
        if(!next.isOf(this) && world instanceof ServerWorld server) PortalRitual.collapseAt(server,pos);
        super.onStateReplaced(state,world,pos,next,moved);
    }
}
