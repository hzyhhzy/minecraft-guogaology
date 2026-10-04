package dev.googology.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Solid, six-faced pixels for actual multi-block lamps, with portable color. */
public final class MosaicLightBlock extends StatefulDecorBlock {
    public static final IntegerProperty COLOR=IntegerProperty.create("color",0,15);
    public MosaicLightBlock(Properties settings){super(settings);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(COLOR);}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level world,net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(!player.isShiftKeyDown()||!player.getMainHandItem().isEmpty()||!player.getOffhandItem().isEmpty())return net.minecraft.world.InteractionResult.PASS;
        if(!world.isClientSide())world.setBlock(pos,state.setValue(COLOR,(state.getValue(COLOR)+1)%16),Block.UPDATE_ALL);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
