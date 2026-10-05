package dev.googology.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** A single ordinal number (0..32) on a brightly colored festive lamp. */
public final class ChristmasDigitBlock extends StatefulDecorBlock {
    public static final int BLANK=33;
    public static final IntegerProperty DIGIT = IntegerProperty.create("digit",0,BLANK);
    private final int color;
    public int color(){return color;}
    public ChristmasDigitBlock(Properties settings,int color) {
        super(settings);
        this.color=color;
        registerDefaultState(getStateDefinition().any().setValue(DIGIT,BLANK));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(DIGIT); }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level world,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(!world.isClientSide())world.setBlock(pos,state.setValue(DIGIT,state.getValue(DIGIT)>=32?0:state.getValue(DIGIT)+1),Block.UPDATE_CLIENTS);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
