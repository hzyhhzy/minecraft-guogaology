package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class BowEntityRemovalMixin {
    @Inject(method="remove",at=@At("HEAD"))
    private void guogaology$removeVolley(Entity.RemovalReason reason,CallbackInfo callback){if(this instanceof ArrowShotAccess arrow&&arrow.guogaology$shot()!=null)BowEffects.removed((Entity)(Object)this,reason,arrow.guogaology$shot());}
}
