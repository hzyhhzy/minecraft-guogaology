package dev.googology.mining;

import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;

public final class EnhancementTable extends Block {
    public final int rank;
    public EnhancementTable(Settings props,int rank){super(props);this.rank=rank;}
    @Override protected ActionResult onUse(BlockState state,World level,BlockPos pos,PlayerEntity player,BlockHitResult hit){
        if(!level.isClient)player.openHandledScreen(new SimpleNamedScreenHandlerFactory((id,inventory,p)->new EnhancementMenu(id,inventory,level,pos,rank),Text.translatable("block.googology.enhancement_table"+(rank==1?"":"_"+rank))));
        return ActionResult.SUCCESS;
    }
}
