package dev.guogaology.lamp;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class LampShapingBlock extends Block {
    public LampShapingBlock(Settings settings){super(settings);}
    @Override protected ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,BlockHitResult hit){
        if(!world.isClient)player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (id,inventory,p)->new LampShapingMenu(id,inventory,world,pos),Text.translatable("block.guogaology.guogao_lamp_shaper")));
        return ActionResult.SUCCESS;
    }
}
