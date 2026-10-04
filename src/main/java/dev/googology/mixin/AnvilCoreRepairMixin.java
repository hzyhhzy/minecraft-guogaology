package dev.googology.mixin;

import dev.googology.mining.GearData;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A repair ingredient must never silently consume another equipment's installed cores. */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilCoreRepairMixin {
    @Inject(method="updateResult",at=@At("RETURN"))
    private void googology$preserveIngredients(CallbackInfo ci){
        var menu=(ScreenHandler)(Object)this;
        if(!menu.getSlot(0).getStack().isEmpty()&&!GearData.cores(menu.getSlot(1).getStack()).isEmpty())menu.getSlot(2).setStack(ItemStack.EMPTY);
    }
}
