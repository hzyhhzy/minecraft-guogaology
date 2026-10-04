package dev.googology.mixin;

import dev.googology.mining.MiningEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.intprovider.IntProvider;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Block.class)
public abstract class MiningExperienceMixin {
    @Shadow protected abstract void dropExperience(ServerWorld level,BlockPos pos,int amount);
    @Redirect(method="dropExperienceWhenMined",at=@At(value="INVOKE",target="Lnet/minecraft/block/Block;dropExperience(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;I)V"))
    private void googology$oreExperience(Block self,ServerWorld level,BlockPos pos,int amount,ServerWorld originalWorld,BlockPos originalPos,ItemStack tool,IntProvider range){dropExperience(level,pos,MiningEffects.experience(amount,tool));}
}
