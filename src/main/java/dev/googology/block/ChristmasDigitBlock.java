package dev.googology.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/** A single ordinal number (0..32) on a brightly colored festive lamp. */
public final class ChristmasDigitBlock extends StatefulDecorBlock {
    public static final int BLANK=33;
    public static final IntProperty DIGIT = IntProperty.of("digit",0,BLANK);
    private final int color;
    public int color(){return color;}
    public ChristmasDigitBlock(Settings settings,int color) {
        super(settings);
        this.color=color;
        setDefaultState(getStateManager().getDefaultState().with(DIGIT,BLANK));
    }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(DIGIT); }
}
