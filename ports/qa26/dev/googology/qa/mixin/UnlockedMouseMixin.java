package dev.googology.qa.mixin;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
public abstract class UnlockedMouseMixin {
    @Inject(method="grabMouse",at=@At("HEAD"),cancellable=true)
    private void freeMouse(CallbackInfo ci){ci.cancel();}
}
