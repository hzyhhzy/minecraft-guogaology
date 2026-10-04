package dev.googology.mixin;

import dev.googology.mining.MiningEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.valueproviders.IntProvider;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Block.class)
public abstract class MiningExperienceMixin {
    @Shadow protected abstract void popExperience(ServerLevel level,BlockPos pos,int amount);
    @Redirect(method="tryDropExperience",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;popExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)V"))
    private void googology$oreExperience(Block self,ServerLevel level,BlockPos pos,int amount,ServerLevel originalWorld,BlockPos originalPos,ItemStack tool,IntProvider range){popExperience(level,pos,MiningEffects.experience(amount,tool));}
}
