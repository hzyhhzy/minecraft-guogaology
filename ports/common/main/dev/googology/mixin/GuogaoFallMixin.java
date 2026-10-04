package dev.googology.mixin;

import dev.googology.GoogologyMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class GuogaoFallMixin {
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void googology$softLanding(double distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (((LivingEntity)(Object)this).level().dimension().equals(GoogologyMod.GUOGAO))
            cir.setReturnValue(false);
    }
}
