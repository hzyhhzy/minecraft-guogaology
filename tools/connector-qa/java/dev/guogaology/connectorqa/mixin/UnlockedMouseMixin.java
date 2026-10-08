package dev.guogaology.connectorqa.mixin;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mouse.class)
public abstract class UnlockedMouseMixin {
    @Inject(method="lockCursor",at=@At("HEAD"),cancellable=true)
    private void unlocked(CallbackInfo ci){ci.cancel();}
}
