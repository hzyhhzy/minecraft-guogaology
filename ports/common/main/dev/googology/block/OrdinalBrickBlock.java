package dev.googology.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** All sixteen ordinal digits share an ID and retain their value as an item component. */
public final class OrdinalBrickBlock extends StatefulDecorBlock {
    public static final IntegerProperty NUMBER=IntegerProperty.create("number",0,15);
    public OrdinalBrickBlock(Properties settings){super(settings);registerDefaultState(getStateDefinition().any().setValue(NUMBER,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(NUMBER);}
}
