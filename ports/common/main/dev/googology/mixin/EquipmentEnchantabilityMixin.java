package dev.googology.mixin;
import dev.googology.mining.GearData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemStack.class)
public abstract class EquipmentEnchantabilityMixin {
    @Inject(method="isEnchantable",at=@At("HEAD"),cancellable=true)
    private void googology$noTable(CallbackInfoReturnable<Boolean> result){if(GearData.forbidsEnchantments((ItemStack)(Object)this))result.setReturnValue(false);}
    @Inject(method="enchant",at=@At("HEAD"),cancellable=true)
    private void googology$noEnchantment(Holder<Enchantment> enchantment,int level,CallbackInfo result){if(GearData.forbidsEnchantments((ItemStack)(Object)this))result.cancel();}
}
