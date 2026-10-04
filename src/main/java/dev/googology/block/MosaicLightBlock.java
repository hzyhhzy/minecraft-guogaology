package dev.googology.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/** Solid, six-faced pixels for actual multi-block lamps, with portable color. */
public final class MosaicLightBlock extends StatefulDecorBlock {
    public static final IntProperty COLOR=IntProperty.of("color",0,15);
    public MosaicLightBlock(Settings settings){super(settings);}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(COLOR);}
    @Override protected net.minecraft.util.ActionResult onUse(BlockState state,net.minecraft.world.World world,net.minecraft.util.math.BlockPos pos,
            net.minecraft.entity.player.PlayerEntity player,net.minecraft.util.hit.BlockHitResult hit){
        if(!player.isSneaking()||!player.getMainHandStack().isEmpty()||!player.getOffHandStack().isEmpty())return net.minecraft.util.ActionResult.PASS;
        if(!world.isClient)world.setBlockState(pos,state.with(COLOR,(state.get(COLOR)+1)%16),Block.NOTIFY_ALL);
        return net.minecraft.util.ActionResult.success(world.isClient);
    }
}
