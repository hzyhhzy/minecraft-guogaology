package dev.guogaology.mixin;

import dev.guogaology.GuogaologyMod;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class GuogaoGravityMixin {
    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true)
    private void guogaology$quarterGravity(CallbackInfoReturnable<Double> cir) {
        if (((Entity)(Object)this).level().dimension().equals(GuogaologyMod.GUOGAO))
            cir.setReturnValue(cir.getReturnValueD() * 0.25);
    }
}
