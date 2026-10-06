package dev.googology.mixin;

import dev.googology.mining.GearData;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

/** One filter at the shared wear path; vanilla Unbreaking processes the surviving points. */
@Mixin(ItemStack.class)
public abstract class EquipmentDurabilityMixin {
    @WrapOperation(method="damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V",at=@At(value="INVOKE",target="Lnet/minecraft/enchantment/EnchantmentHelper;getItemDamage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;I)I"))
    private int googology$wear(ServerWorld world,ItemStack stack,int amount,Operation<Integer> original,@Local(argsOnly=true) ServerPlayerEntity player){
        return original.call(world,stack,GearData.wearCost(stack,amount,player,world.random::nextDouble));
    }
}
