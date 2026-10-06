package dev.googology.mixin;
import dev.googology.mining.GearData;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnchantmentHelper.class)
public abstract class LootingCoreMixin {
    @Inject(method="getEnchantmentLevel",at=@At("RETURN"),cancellable=true)
    private static void googology$looting(Holder<Enchantment> enchantment,LivingEntity owner,CallbackInfoReturnable<Integer> result){if(enchantment.is(Enchantments.LOOTING)&&owner instanceof Player)result.setReturnValue(result.getReturnValueI()+GearData.yieldLevel(owner.getMainHandItem(),owner,true));}
}
