package dev.googology.mixin;

import dev.googology.GoogologyMod;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class GuogaoGravityMixin {
    @Inject(method = "getFinalGravity", at = @At("RETURN"), cancellable = true)
    private void googology$quarterGravity(CallbackInfoReturnable<Double> cir) {
        if (((Entity)(Object)this).getWorld().getRegistryKey().equals(GoogologyMod.GUOGAO))
            cir.setReturnValue(cir.getReturnValueD() * 0.25);
    }
}
