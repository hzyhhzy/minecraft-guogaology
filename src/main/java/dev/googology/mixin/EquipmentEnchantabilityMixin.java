package dev.googology.mixin;
import dev.googology.mining.GearData;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemStack.class)
public abstract class EquipmentEnchantabilityMixin {
    @Inject(method="isEnchantable",at=@At("HEAD"),cancellable=true)
    private void googology$noTable(CallbackInfoReturnable<Boolean> result){if(GearData.forbidsEnchantments((ItemStack)(Object)this))result.setReturnValue(false);}
    @Inject(method="addEnchantment",at=@At("HEAD"),cancellable=true)
    private void googology$noEnchantment(RegistryEntry<Enchantment> enchantment,int level,CallbackInfo result){if(GearData.forbidsEnchantments((ItemStack)(Object)this))result.cancel();}
}
