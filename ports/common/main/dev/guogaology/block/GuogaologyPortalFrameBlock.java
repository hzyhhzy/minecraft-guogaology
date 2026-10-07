package dev.guogaology.block;

import dev.guogaology.portal.PortalRitual;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GuogaologyPortalFrameBlock extends StatefulDecorBlock {
    public static final IntegerProperty STYLE=IntegerProperty.create("style",0,2);
    private static final VoxelShape[] SHAPES={Shapes.or(box(0,0,0,16,3,16),box(2,3,2,14,5,14),box(3,5,3,13,9,13),box(4,9,4,12,11,12),box(1,5,1,4,8,4),box(1,5,12,4,8,15),box(12,5,1,15,8,4),box(12,5,12,15,8,15)),
            Shapes.or(box(0,0,0,16,3,16),box(2,3,2,14,5,14),box(3,5,3,13,10,13),box(4,10,4,12,12,12),box(1,5,1,3,13,3),box(1,5,13,3,13,15),box(13,5,1,15,13,3),box(13,5,13,15,13,15)),
            Shapes.or(box(0,0,0,16,3,16),box(2,3,2,14,5,14),box(2,5,2,14,8,14),box(4,8,4,12,12,12),box(1,5,1,4,10,4),box(1,5,12,4,10,15),box(12,5,1,15,10,4),box(12,5,12,15,10,15))};
    public GuogaologyPortalFrameBlock(Properties settings) { super(settings);registerDefaultState(getStateDefinition().any().setValue(STYLE,0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(STYLE); }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) { return SHAPES[state.getValue(STYLE)]; }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) { return SHAPES[state.getValue(STYLE)]; }
    /** Activated frames are spent ritual material, regardless of tool or Silk Touch. */
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder) { return java.util.List.of(); }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel world,BlockPos pos,boolean moved) {
        PortalRitual.collapseAt(world,pos);
        super.affectNeighborsAfterRemoval(state,world,pos,moved);
    }
}
