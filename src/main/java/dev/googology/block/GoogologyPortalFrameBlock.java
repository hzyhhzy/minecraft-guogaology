package dev.googology.block;

import dev.googology.portal.PortalRitual;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public final class GoogologyPortalFrameBlock extends StatefulDecorBlock {
    public static final IntProperty STYLE=IntProperty.of("style",0,2);
    private static final VoxelShape[] SHAPES={VoxelShapes.union(createCuboidShape(0,0,0,16,3,16),createCuboidShape(2,3,2,14,5,14),createCuboidShape(3,5,3,13,9,13),createCuboidShape(4,9,4,12,11,12),createCuboidShape(1,5,1,4,8,4),createCuboidShape(1,5,12,4,8,15),createCuboidShape(12,5,1,15,8,4),createCuboidShape(12,5,12,15,8,15)),
            VoxelShapes.union(createCuboidShape(0,0,0,16,3,16),createCuboidShape(2,3,2,14,5,14),createCuboidShape(3,5,3,13,10,13),createCuboidShape(4,10,4,12,12,12),createCuboidShape(1,5,1,3,13,3),createCuboidShape(1,5,13,3,13,15),createCuboidShape(13,5,1,15,13,3),createCuboidShape(13,5,13,15,13,15)),
            VoxelShapes.union(createCuboidShape(0,0,0,16,3,16),createCuboidShape(2,3,2,14,5,14),createCuboidShape(2,5,2,14,8,14),createCuboidShape(4,8,4,12,12,12),createCuboidShape(1,5,1,4,10,4),createCuboidShape(1,5,12,4,10,15),createCuboidShape(12,5,1,15,10,4),createCuboidShape(12,5,12,15,10,15))};
    public GoogologyPortalFrameBlock(Settings settings) { super(settings);setDefaultState(getStateManager().getDefaultState().with(STYLE,0)); }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder) { builder.add(STYLE); }
    @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context) { return SHAPES[state.get(STYLE)]; }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context) { return SHAPES[state.get(STYLE)]; }
    @Override protected void onStateReplaced(BlockState state,World world,BlockPos pos,BlockState next,boolean moved) {
        if(!next.isOf(this) && world instanceof ServerWorld server) PortalRitual.collapseAt(server,pos);
        super.onStateReplaced(state,world,pos,next,moved);
    }
}
