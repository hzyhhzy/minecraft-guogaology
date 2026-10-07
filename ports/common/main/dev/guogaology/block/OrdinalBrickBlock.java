package dev.guogaology.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** All sixteen ordinal digits share an ID and retain their value as an item component. */
public final class OrdinalBrickBlock extends StatefulDecorBlock {
    public static final IntegerProperty NUMBER=IntegerProperty.create("number",0,15);
    public OrdinalBrickBlock(Properties settings){super(settings);registerDefaultState(getStateDefinition().any().setValue(NUMBER,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(NUMBER);}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level world,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(!world.isClientSide())world.setBlock(pos,state.setValue(NUMBER,(state.getValue(NUMBER)+1)%16),Block.UPDATE_CLIENTS);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
