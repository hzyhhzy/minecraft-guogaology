package dev.guogaology.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/** A single ordinal number (0..32) on a brightly colored festive lamp. */
public final class ChristmasDigitBlock extends StatefulDecorBlock {
    public static final int MAX_DIGIT=32;
    public static final IntProperty DIGIT = IntProperty.of("digit",0,MAX_DIGIT);
    private final int color;
    public int color(){return color;}
    public ChristmasDigitBlock(Settings settings,int color) {
        super(settings);
        this.color=color;
        setDefaultState(getStateManager().getDefaultState().with(DIGIT,0));
    }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(DIGIT); }
    @Override protected net.minecraft.util.ActionResult onUse(BlockState state,net.minecraft.world.World world,net.minecraft.util.math.BlockPos pos,net.minecraft.entity.player.PlayerEntity player,net.minecraft.util.hit.BlockHitResult hit){
        if(!world.isClient)world.setBlockState(pos,state.with(DIGIT,state.get(DIGIT)>=32?0:state.get(DIGIT)+1),Block.NOTIFY_LISTENERS);
        return net.minecraft.util.ActionResult.SUCCESS;
    }
}
