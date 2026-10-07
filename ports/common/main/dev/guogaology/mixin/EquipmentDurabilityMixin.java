package dev.guogaology.mixin;

import dev.guogaology.mining.GearData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

/** The sole enchantment wear call is shared by tools, armor and hurtWithoutBreaking. */
@Mixin(ItemStack.class)
public abstract class EquipmentDurabilityMixin {
    @WrapOperation(method="processDurabilityChange",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"),require=1,expect=1)
    private int guogaology$wear(ServerLevel world,ItemStack stack,int amount,Operation<Integer> original,@Local(argsOnly=true) ServerPlayer player){
        return original.call(world,stack,GearData.wearCost(stack,amount,player,world.getRandom()::nextDouble));
    }
}
