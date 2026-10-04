package dev.googology.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/** All sixteen ordinal digits share an ID and retain their value as an item component. */
public final class OrdinalBrickBlock extends StatefulDecorBlock {
    public static final IntProperty NUMBER=IntProperty.of("number",0,15);
    public OrdinalBrickBlock(Settings settings){super(settings);setDefaultState(getStateManager().getDefaultState().with(NUMBER,0));}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(NUMBER);}
}
