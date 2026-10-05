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
    @Override protected net.minecraft.util.ActionResult onUse(BlockState state,net.minecraft.world.World world,net.minecraft.util.math.BlockPos pos,net.minecraft.entity.player.PlayerEntity player,net.minecraft.util.hit.BlockHitResult hit){
        if(!world.isClient)world.setBlockState(pos,state.with(NUMBER,(state.get(NUMBER)+1)%16),Block.NOTIFY_LISTENERS);
        return net.minecraft.util.ActionResult.SUCCESS;
    }
}
