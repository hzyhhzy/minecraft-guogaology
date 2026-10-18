package dev.guogaology.networkqa263.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The isolated automated run must never capture the user's pointer. */
@Mixin(MouseHandler.class)
public abstract class UnlockedMouseMixin {
    @Inject(method="grabMouse", at=@At("HEAD"), cancellable=true)
    private void freeMouse(CallbackInfo ci) { ci.cancel(); }
}
