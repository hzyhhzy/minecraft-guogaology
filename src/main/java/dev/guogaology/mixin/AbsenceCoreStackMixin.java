package dev.guogaology.mixin;

import dev.guogaology.mining.AbsenceCoreItems;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lazy, ID-scoped upgrade for saved, nested, copied and network-decoded item stacks. */
@Mixin(ItemStack.class)
public abstract class AbsenceCoreStackMixin {
    // All non-empty constructor routes converge here; the EMPTY/Void constructor is untouched.
    @Inject(method="<init>(Lnet/minecraft/item/ItemConvertible;ILnet/minecraft/component/ComponentMapImpl;)V",at=@At("RETURN"))
    private void guogaology$normalizeLoadedAbsence(CallbackInfo ci) {
        AbsenceCoreItems.normalizeStandalone((ItemStack)(Object)this);
    }
    @Inject(method={"applyChanges","applyUnvalidatedChanges","applyComponentsFrom"},at=@At("RETURN"))
    private void guogaology$normalizePatchedAbsence(CallbackInfo ci) {
        AbsenceCoreItems.normalizeStandalone((ItemStack)(Object)this);
    }
}
