package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class BowEntityRemovalMixin {
    @Inject(method="remove",at=@At("HEAD"))
    private void googology$removeVolley(Entity.RemovalReason reason,CallbackInfo callback){if(this instanceof ArrowShotAccess arrow&&arrow.googology$shot()!=null)BowEffects.removed((Entity)(Object)this,reason,arrow.googology$shot());}
}
