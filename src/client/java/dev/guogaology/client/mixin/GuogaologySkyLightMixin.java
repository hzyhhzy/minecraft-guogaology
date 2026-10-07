package dev.guogaology.client.mixin;

import dev.guogaology.client.GuogaologyDimensionEffects;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class GuogaologySkyLightMixin {
    @Inject(method="getSkyBrightness",at=@At("RETURN"),cancellable=true)
    private void guogaology$regionalNight(float tickDelta, CallbackInfoReturnable<Float> cir) {
        float amount=GuogaologyDimensionEffects.forestAmount();
        if(amount>0) cir.setReturnValue(cir.getReturnValueF()*(1-amount)+.14f*amount);
    }
}
