package dev.guogaology.block;


import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Blue/green/white porcelain for the crown; every face and recovered item shares its color. */
public final class AstraWeaveBlock extends StatefulDecorBlock {
    public static final IntegerProperty COLOR=IntegerProperty.create("color",0,63);
    public AstraWeaveBlock(Properties settings){super(settings);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(COLOR);}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level world,net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(!player.isShiftKeyDown()||!player.getMainHandItem().isEmpty()||!player.getOffhandItem().isEmpty())return net.minecraft.world.InteractionResult.PASS;
        if(!world.isClientSide())world.setBlock(pos,state.setValue(COLOR,(state.getValue(COLOR)+1)%64),Block.UPDATE_ALL);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
