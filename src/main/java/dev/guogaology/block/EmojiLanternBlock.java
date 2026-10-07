package dev.guogaology.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/** Each of 26 exterior tiles stores its exact part and emotion; the center is a crystal. */
public final class EmojiLanternBlock extends StatefulDecorBlock {
    public static final IntProperty PART = IntProperty.of("part", 0, 27);
    public static final IntProperty EMOTION = IntProperty.of("emotion",0,1);
    public EmojiLanternBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(PART, 27).with(EMOTION,0));
    }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(PART,EMOTION); }
    public static BlockState tile(Block lamp, int x, int y, int z) {
        return tile(lamp,x,y,z,0);
    }
    public static BlockState tile(Block lamp,int x,int y,int z,int emotion){
        if(x==1&&y==1&&z==1)return dev.guogaology.GuogaologyBlocks.ORDINAL_CRYSTAL.getDefaultState();
        return lamp.getDefaultState().with(PART,x+3*y+9*z).with(EMOTION,emotion);
    }
}
