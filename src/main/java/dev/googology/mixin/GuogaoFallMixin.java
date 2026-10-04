package dev.googology.mixin;

import dev.googology.GoogologyMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class GuogaoFallMixin {
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void googology$softLanding(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (((LivingEntity)(Object)this).getWorld().getRegistryKey().equals(GoogologyMod.GUOGAO))
            cir.setReturnValue(false);
    }
}
