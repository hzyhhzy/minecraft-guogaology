package dev.googology.mixin;

import dev.googology.mining.GearData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A repair ingredient must never silently consume another equipment's installed cores. */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneCoreRepairMixin {
    @Inject(method="createResult",at=@At("RETURN"))
    private void googology$preserveIngredients(CallbackInfo ci){
        var menu=(AbstractContainerMenu)(Object)this;
        if(!menu.getSlot(0).getItem().isEmpty()&&!GearData.cores(menu.getSlot(1).getItem()).isEmpty())menu.getSlot(2).set(ItemStack.EMPTY);
    }
}
