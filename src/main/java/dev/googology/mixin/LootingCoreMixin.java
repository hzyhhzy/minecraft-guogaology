package dev.googology.mixin;
import dev.googology.mining.GearData;
import net.minecraft.enchantment.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnchantmentHelper.class)
public abstract class LootingCoreMixin {
    @Inject(method="getEquipmentLevel",at=@At("RETURN"),cancellable=true)
    private static void googology$looting(RegistryEntry<Enchantment> enchantment,LivingEntity owner,CallbackInfoReturnable<Integer> result){if(enchantment.matchesKey(Enchantments.LOOTING)&&owner instanceof PlayerEntity)result.setReturnValue(result.getReturnValueI()+GearData.yieldLevel(owner.getMainHandStack(),owner,true));}
}
