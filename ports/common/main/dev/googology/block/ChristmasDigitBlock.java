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
}
