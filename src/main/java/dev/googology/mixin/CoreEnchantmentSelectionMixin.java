package dev.googology.mixin;
import dev.googology.mining.GearData;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Enchantment.class)
public abstract class CoreEnchantmentSelectionMixin {
    @Inject(method="isAcceptableItem",at=@At("HEAD"),cancellable=true)
    private void googology$noBook(ItemStack stack,CallbackInfoReturnable<Boolean> result){if(GearData.forbidsEnchantments(stack))result.setReturnValue(false);}
}
