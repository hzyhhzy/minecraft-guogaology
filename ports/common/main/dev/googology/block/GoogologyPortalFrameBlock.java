package dev.googology.block;

import dev.googology.portal.PortalRitual;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GoogologyPortalFrameBlock extends StatefulDecorBlock {
    public static final BooleanProperty FRUIT=BooleanProperty.create("fruit");
    private static final VoxelShape SHAPE=Shapes.or(
            box(0,0,0,16,3,16), box(2,3,2,14,6,14),
            box(1,6,1,15,8,15), box(4,8,4,12,12,12),
            box(1,8,1,4,11,4),box(12,8,1,15,11,4),
            box(1,8,12,4,11,15),box(12,8,12,15,11,15));
    public GoogologyPortalFrameBlock(Properties settings) { super(settings);registerDefaultState(getStateDefinition().any().setValue(FRUIT,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FRUIT); }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) { return SHAPE; }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) { return SHAPE; }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel world,BlockPos pos,boolean moved) {
        PortalRitual.collapseAt(world,pos);
        super.affectNeighborsAfterRemoval(state,world,pos,moved);
    }
}
