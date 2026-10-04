package dev.googology.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class EnhancementTable extends Block {
    public final int rank;
    public EnhancementTable(Properties props,int rank){super(props);this.rank=rank;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide())player.openMenu(new SimpleMenuProvider((id,inventory,p)->new EnhancementMenu(id,inventory,level,pos,rank),Component.translatable("block.googology.enhancement_table"+(rank==1?"":"_"+rank))));
        return InteractionResult.SUCCESS;
    }
}
