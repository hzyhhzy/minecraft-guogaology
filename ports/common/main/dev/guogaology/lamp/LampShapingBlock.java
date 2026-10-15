package dev.guogaology.lamp;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class LampShapingBlock extends Block {
    public LampShapingBlock(Properties settings){super(settings);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level world,BlockPos pos,Player player,BlockHitResult hit){
        if(!world.isClientSide())player.openMenu(new SimpleMenuProvider(
                (id,inventory,p)->new LampShapingMenu(id,inventory,world,pos),Component.translatable("block.guogaology.guogao_lamp_shaper")));
        return InteractionResult.SUCCESS;
    }
}
