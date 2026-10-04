package dev.googology.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Each of 26 exterior tiles stores its exact part and emotion; the center is a crystal. */
public final class EmojiLanternBlock extends StatefulDecorBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 27);
    public static final IntegerProperty EMOTION = IntegerProperty.create("emotion",0,1);
    public EmojiLanternBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any().setValue(PART, 27).setValue(EMOTION,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART,EMOTION); }
    public static BlockState tile(Block lamp, int x, int y, int z) {
        return tile(lamp,x,y,z,0);
    }
    public static BlockState tile(Block lamp,int x,int y,int z,int emotion){
        if(x==1&&y==1&&z==1)return dev.googology.GoogologyBlocks.ORDINAL_CRYSTAL.defaultBlockState();
        return lamp.defaultBlockState().setValue(PART,x+3*y+9*z).setValue(EMOTION,emotion);
    }
}
