package dev.guogaology.mixin;
import dev.guogaology.mining.MiningEffects;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(Block.class)
public abstract class BlockYieldMixin {
    @WrapMethod(method="getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;")
    private static List<ItemStack> guogaology$yield(BlockState state,ServerWorld world,BlockPos pos,BlockEntity blockEntity,Entity owner,ItemStack tool,Operation<List<ItemStack>> original){return original.call(state,world,pos,blockEntity,owner,MiningEffects.lootTool(state,world,owner,tool));}
}
