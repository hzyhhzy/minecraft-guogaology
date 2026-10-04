package dev.googology.block;

import net.minecraft.block.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/** Blue/green/white porcelain for the crown; every face and recovered item shares its color. */
public final class AstraWeaveBlock extends StatefulDecorBlock {
    public static final IntProperty COLOR=IntProperty.of("color",0,63);
    public AstraWeaveBlock(Settings settings){super(settings);}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(COLOR);}
    @Override protected net.minecraft.util.ActionResult onUse(BlockState state,net.minecraft.world.World world,net.minecraft.util.math.BlockPos pos,
            net.minecraft.entity.player.PlayerEntity player,net.minecraft.util.hit.BlockHitResult hit){
        if(!player.isSneaking()||!player.getMainHandStack().isEmpty()||!player.getOffHandStack().isEmpty())return net.minecraft.util.ActionResult.PASS;
        if(!world.isClient)world.setBlockState(pos,state.with(COLOR,(state.get(COLOR)+1)%64),Block.NOTIFY_ALL);
        return net.minecraft.util.ActionResult.success(world.isClient);
    }
}
