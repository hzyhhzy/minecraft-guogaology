package dev.guogaology.mixin;

import dev.guogaology.GuogaologyMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class GuogaoFallMixin {
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void guogaology$softLanding(double distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (((LivingEntity)(Object)this).level().dimension().equals(GuogaologyMod.GUOGAO))
            cir.setReturnValue(false);
    }
}
